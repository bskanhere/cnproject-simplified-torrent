package cn.torrent;

import cn.torrent.config.CommonConfig;
import cn.torrent.config.PeerInfo;
import cn.torrent.config.PeersConfig;
import cn.torrent.exceptions.HandShakeException;
import cn.torrent.tasks.SelectOptimisticallyUnChokedNeighborTimer;
import cn.torrent.tasks.SelectPreferredNeighborTimer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Peer {

    private final CommonConfig commonConfig;
    private final Logger logger;
    private final PeersConfig peersConfig;
    private final int peerID;
    private final ArrayList<Thread> threadPool;
    private final ArrayList<MessageIO> serverIOHandlers = new ArrayList<>();
    private final ArrayList<MessageIO> IOHandlers = new ArrayList<>();
    private HashMap<Integer, MessageIO> IOHandlersMap = new HashMap<>();
    private PeerState state;

    public Peer(
            final int peerID,
            final CommonConfig commonConfig,
            final PeersConfig peersConfig,
            final String logPath) {
        this.peerID = peerID;
        this.commonConfig = commonConfig;
        this.peersConfig = peersConfig;
        this.logger = new Logger(logPath);
        this.threadPool = new ArrayList<>();
    }

    public void run() {
        Optional<PeerInfo> currentPeerInfo = peersConfig.get(peerID);
        if (!currentPeerInfo.isPresent()) {
            throw new IllegalArgumentException("peerID is invalid");
        }

        ArrayList<PeerInfo> serversBefore = peersConfig.before(peerID);
        ArrayList<PeerInfo> serversAfter = peersConfig.after(peerID);

        Acceptor acceptor = new Acceptor(currentPeerInfo.get().port, serversAfter.size(), serverIOHandlers);
        Thread acceptThread = new Thread(acceptor);
        acceptThread.start();

        makeConnections(serversBefore);

        try {
            acceptThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        IOHandlers.addAll(serverIOHandlers);

        performHandshake(currentPeerInfo.get(), serversAfter);

        state = PeerState.from(peerID, commonConfig, peersConfig, IOHandlersMap);

        sendBitField();

        FileHandler fileHandler = new FileHandler(commonConfig.fileName);
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
                new SelectPreferredNeighborTimer(state, logger),
                0,
                commonConfig.unChokingInterval * 1000);
        optimisticNeighbourTimer.scheduleAtFixedRate(
                new SelectOptimisticallyUnChokedNeighborTimer(state, logger),
                0,
                commonConfig.optimisticUnChokingInterval * 1000);

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

        logger.complete(peerID);
        logger.close();
    }

    public Socket connect(final String ipAddress, final int port) throws IOException {
        return new Socket(ipAddress, port);
    }

    private void makeConnections(final ArrayList<PeerInfo> servers) {
        for (PeerInfo peerServerInfo : servers) {
            try {
                Socket clientSocket = connect(peerServerInfo.ipAddress, peerServerInfo.port);
                IOHandlers.add(new MessageIO(clientSocket));
                logger.makesConnection(peerID, peerServerInfo.peerID);
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

        for (MessageIO io : IOHandlers) {
            try {
                int receivedPeerID = io.readHandShakeMessage();
                IOHandlersMap.put(receivedPeerID, io);
                Optional<PeerInfo> clientPeer =
                        after.stream().filter(peerInfo -> peerInfo.peerID == receivedPeerID).findFirst();
                if (clientPeer.isPresent()) {
                    logger.isConnected(currentPeerInfo.peerID, receivedPeerID);
                }
            } catch (IOException | HandShakeException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBitField() {
        for (Map.Entry<Integer, MessageIO> set : IOHandlersMap.entrySet()) {
            try {
                byte[] bitFieldOfPeer = state.getBitFieldOfPeer(peerID);
                set.getValue().writeBitField(bitFieldOfPeer);
                logger.sendBitField(set.getKey());
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






