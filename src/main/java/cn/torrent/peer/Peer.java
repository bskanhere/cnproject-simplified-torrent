package cn.torrent.peer;

import cn.torrent.FileHandler;
import cn.torrent.Log;
import cn.torrent.SocketMessageReadWrite;
import cn.torrent.config.CommonConfig;
import cn.torrent.config.PeerInfo;
import cn.torrent.config.PeersConfig;
import cn.torrent.exceptions.HandShakeException;
import cn.torrent.tasks.SelectOptimisticallyUnChokedNeighborTimer;
import cn.torrent.tasks.SelectPreferredNeighborTimer;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class Peer {

    private final CommonConfig commonConfig;
    private final Log log;
    private final PeersConfig peersConfig;
    private final int peerId;
    private final ArrayList<Thread> peerHandlerThreadPool;
    private final ArrayList<SocketMessageReadWrite> serverReadWriteHandlers = new ArrayList<>();
    private final ArrayList<SocketMessageReadWrite> readWriteHandlers = new ArrayList<>();
    private HashMap<Integer, SocketMessageReadWrite> readWriteHandlersMap = new HashMap<>();
    private PeerState state;

    public Peer(
            final int peerId,
            final CommonConfig commonConfig,
            final PeersConfig peersConfig,
            final String logPath) {
        this.peerId = peerId;
        this.commonConfig = commonConfig;
        this.peersConfig = peersConfig;
        this.log = new Log(logPath);
        this.peerHandlerThreadPool = new ArrayList<>();
    }

    public void run() {
        Optional<PeerInfo> currentPeerInfo = peersConfig.get(peerId);
        if (!currentPeerInfo.isPresent()) {
            throw new IllegalArgumentException("peerID is invalid");
        }
        log.initialised(currentPeerInfo.get().ipAddress, currentPeerInfo.get().port, (commonConfig.fileSize / commonConfig.pieceSize) + 1);


        ArrayList<PeerInfo> serversBefore = new ArrayList<>();
        for (PeerInfo peer : peersConfig.getPeersList()) {
            if (peer.peerID == peerId) break;
            else {
                serversBefore.add(peer);
            }
        }
        ArrayList<PeerInfo> serversAfter = new ArrayList<>();
        boolean found = false;
        for (PeerInfo peer : peersConfig.getPeersList()) {
            if (peer.peerID == peerId) {
                found = true;
                continue;
            }
            if (found) {
                serversAfter.add(peer);
            }
        }

        ConnectionRequestHandler connectionRequestHandler = new ConnectionRequestHandler(currentPeerInfo.get().port, serversAfter.size(), serverReadWriteHandlers);
        Thread acceptThread = new Thread(connectionRequestHandler);
        acceptThread.start();

        makeConnections(serversBefore);

        try {
            acceptThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        readWriteHandlers.addAll(serverReadWriteHandlers);

        performHandshake(currentPeerInfo.get(), serversAfter);

        state = PeerState.from(peerId, commonConfig, peersConfig, readWriteHandlersMap);
        log.bitfield(state);

        sendBitField();

        FileHandler fileHandler = new FileHandler("files" + peerId + "/" + commonConfig.fileName);

        for (Map.Entry<Integer, SocketMessageReadWrite> set : readWriteHandlersMap.entrySet()) {
            PeerHandler peerHandler = new PeerHandler(state, set.getKey(), fileHandler, log);
            Thread thread = new Thread(peerHandler);
            thread.setName("peer:" + peerId + ":handler:" + set.getKey());
            peerHandlerThreadPool.add(thread);
        }

        Timer preferredNeighbourTimer = new Timer();
        Timer optimisticNeighbourTimer = new Timer();

        preferredNeighbourTimer.scheduleAtFixedRate(
                new SelectPreferredNeighborTimer(state, log),
                0,
                commonConfig.unChokingInterval * 1000);

        optimisticNeighbourTimer.scheduleAtFixedRate(
                new SelectOptimisticallyUnChokedNeighborTimer(state, log),
                0,
                commonConfig.optimisticUnChokingInterval * 1000);

        for (Thread thread : peerHandlerThreadPool) {
            thread.start();
        }
        for (Thread thread : peerHandlerThreadPool) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        preferredNeighbourTimer.cancel();
        optimisticNeighbourTimer.cancel();
        for (Map.Entry<Integer, SocketMessageReadWrite> set : readWriteHandlersMap.entrySet()) {
            try {
                set.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.complete(peerId);
        log.close();
    }

    public Socket connect(final String ipAddress, final int port) throws IOException {
        return new Socket(ipAddress, port);
    }

    private void makeConnections(final ArrayList<PeerInfo> servers) {
        for (PeerInfo peerServerInfo : servers) {
            try {
                Socket clientSocket = connect(peerServerInfo.ipAddress, peerServerInfo.port);
                readWriteHandlers.add(new SocketMessageReadWrite(clientSocket));
                log.makesConnection(peerId, peerServerInfo.peerID);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(peerId + " Cant connect to " + peerServerInfo.peerID);
                System.out.println(e.getMessage());
                return;
            }
        }
    }

    private void performHandshake(final PeerInfo currentPeerInfo, final ArrayList<PeerInfo> after) {
        int i = 0;
        for (SocketMessageReadWrite io : readWriteHandlers) {
            try {
                io.writeHandShakeMessage(currentPeerInfo.peerID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (SocketMessageReadWrite io : readWriteHandlers) {
            try {
                int receivedPeerID = io.readHandShakeMessage();
                readWriteHandlersMap.put(receivedPeerID, io);
                Optional<PeerInfo> clientPeer =
                        after.stream().filter(peerInfo -> peerInfo.peerID == receivedPeerID).findFirst();
                if (clientPeer.isPresent()) {
                    log.isConnected(currentPeerInfo.peerID, receivedPeerID);
                }
            } catch (IOException | HandShakeException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBitField() {
        for (Map.Entry<Integer, SocketMessageReadWrite> set : readWriteHandlersMap.entrySet()) {
            try {
                byte[] bitFieldOfPeer = state.getBitFieldOfPeer(peerId);
                set.getValue().writeBitField(bitFieldOfPeer);
                log.sendBitField(set.getKey());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}






