package cn.torrent;

import cn.torrent.exceptions.HandShakeException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Peer {

    private final CommonInfo commonInfo;
    private final Logger logger;
    private final PeersInfo peersInfo;
    private final int peerID;
    private final ArrayList<Thread> threadPool;
    final ArrayList<MessageIO> serverIOHandlers = new ArrayList<>();
    final ArrayList<MessageIO> IOHandlers = new ArrayList<>();
    HashMap<Integer, MessageIO> IOHandlersMap = new HashMap<>();
    PeerState state;

    public Peer(
            final int peerID,
            final CommonInfo commonInfo,
            final PeersInfo peersInfo,
            final String logPath) {
        this.peerID = peerID;
        this.commonInfo = commonInfo;
        this.peersInfo = peersInfo;
        this.logger = new Logger(logPath);
        this.threadPool = new ArrayList<>();
    }

    public void run() {
        // get my peer info
        Optional<PeerInfo> currentPeerInfo = peersInfo.get(peerID);
        if (!currentPeerInfo.isPresent()) {
            throw new IllegalArgumentException("peerID is invalid");
        }
        // Data
        ArrayList<PeerInfo> serversBefore = peersInfo.before(peerID);
        ArrayList<PeerInfo> serversAfter = peersInfo.after(peerID);

        // My server
        Acceptor acceptor = new Acceptor(currentPeerInfo.get().port, serversAfter.size(), serverIOHandlers);
        Thread acceptThread = new Thread(acceptor);
        acceptThread.start();

        // Connect to clients
        makeConnections(serversBefore);

        // Finish server accepting
        try {
            acceptThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        IOHandlers.addAll(serverIOHandlers);

        performHandshake(currentPeerInfo.get(), serversAfter);

        state = PeerState.from(peerID, commonInfo, peersInfo, IOHandlersMap);

        sendBitField();

        FileHandler fileHandler = new FileHandler(commonInfo.FileName);
        for (Map.Entry<Integer, MessageIO> set : IOHandlersMap.entrySet()) {
            try {
                PeerHandler peerHandler = new PeerHandler(state, set.getKey(), fileHandler, logger);
                Thread thread = new Thread(peerHandler);
                thread.setName("p" + peerID + "h" + set.getKey());
                threadPool.add(thread);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Timer preferredNeighbourTimer = new Timer();
        Timer optimisticNeighbourTimer = new Timer();

        preferredNeighbourTimer.scheduleAtFixedRate(
                new PreferredNeighborSelectionTimerTask(state, logger),
                0,
                commonInfo.UnChokingInterval * 1000);
        optimisticNeighbourTimer.scheduleAtFixedRate(
                new OptimisticNeighborSelectionTimerTask(state, logger),
                0,
                commonInfo.OptimisticUnChokingInterval * 1000);

        for (Thread thread : threadPool) {
            thread.start();
        }
        for (Thread thread : threadPool) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        preferredNeighbourTimer.cancel();
        optimisticNeighbourTimer.cancel();
        for (Map.Entry<Integer, MessageIO> set : IOHandlersMap.entrySet()) {
            try {
                set.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        logger.close();
    }

    // Make this private when not testing
    public Socket connect(final String ipAddress, final int port) throws IOException {
        return new Socket(ipAddress, port);
    }

    private void makeConnections(final ArrayList<PeerInfo> servers) {
        for (PeerInfo peerServerInfo : servers) {
            try {
                Socket clientSocket = connect(peerServerInfo.ipAddress, peerServerInfo.port);
                IOHandlers.add(new MessageIO(clientSocket));
                logger.connection(peerID, peerServerInfo.peerID);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(peerID + " Cant connect to " + peerServerInfo.peerID);
                System.out.println(e.getMessage());
                return;
            }
        }
    }

    private void performHandshake(final PeerInfo currentPeerInfo, final ArrayList<PeerInfo> after) {
        // Send handshake
        for (MessageIO io : IOHandlers) {
            try {
                io.writeHandShakeMessage(currentPeerInfo.peerID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Read handshake and set map<peer,socket>
        for (MessageIO io : IOHandlers) {
            try {
                int receivedPeerID = io.readHandShakeMessage();
                IOHandlersMap.put(receivedPeerID, io);
                Optional<PeerInfo> clientPeer =
                        after.stream().filter(peerInfo -> peerInfo.peerID == receivedPeerID).findFirst();
                if (clientPeer.isPresent()) {
                    logger.connected(currentPeerInfo.peerID, receivedPeerID);
                }
            } catch (IOException | HandShakeException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBitField() {
        for (Map.Entry<Integer, MessageIO> set : IOHandlersMap.entrySet()) {
            try {
                BitField myBitField = state.getBitFieldOfPeer(peerID);
                set.getValue().writeBitField(myBitField);
                logger.sendBitField(set.getKey(), myBitField);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Acceptor implements Runnable {
    final int port;
    int expectedConnections;
    final ArrayList<MessageIO> ioHandlers;

    Acceptor(int port, int expectedConnections, ArrayList<MessageIO> ioHandlers) {
        this.port = port;
        this.expectedConnections = expectedConnections;
        this.ioHandlers = ioHandlers;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port, 200);
            while (expectedConnections-- > 0) {
                Socket clientSocket = serverSocket.accept();
                ioHandlers.add(new MessageIO(clientSocket));
            }
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("cant close server");
            e.printStackTrace();
        }
    }
}

class PreferredNeighborSelectionTimerTask extends TimerTask {
    final PeerState state;
    final Logger logger;

    public PreferredNeighborSelectionTimerTask(final PeerState state, Logger logger) {
        this.state = state;
        this.logger = logger;
    }

    @Override
    public void run() {
        state.updatePreferredNeighbors();
        for (PeerInfo peerInfo : state.getPeersInfo()) {
            if (peerInfo.peerID == state.peerID) continue;
            MessageIO io = state.getIOHandlerPeer(peerInfo.peerID);
            try {
                if (state.neighbourChokeStatus.get(peerInfo.peerID) == PeerState.ChokeStatus.CHOKED) {
                    io.writeChoke();
                    logger.sendChoke(state.peerID, peerInfo.peerID);
                } else {
                    io.writeUnChoke();
                    logger.sendUnChoke(state.peerID, peerInfo.peerID);
                }
            } catch (IOException e) {
                break;
            }
        }
    }
}


class OptimisticNeighborSelectionTimerTask extends TimerTask {
    final PeerState state;
    final Logger logger;

    OptimisticNeighborSelectionTimerTask(PeerState state, Logger logger) {
        this.state = state;
        this.logger = logger;
    }

    @Override
    public void run() {
        Optional<Integer> opUnchokedPeer = state.updateOptimisticNeighbor();
        if (opUnchokedPeer.isPresent()) {
            MessageIO io = state.getIOHandlerPeer(opUnchokedPeer.get());
            try {
                io.writeUnChoke();
                logger.optimisticallyUnChoking(state.peerID, opUnchokedPeer.get());
            } catch (IOException ignored) {
            }
        }
    }
}
