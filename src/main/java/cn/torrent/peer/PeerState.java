package cn.torrent.peer;

import cn.torrent.SocketMessageReadWrite;
import cn.torrent.config.CommonConfig;
import cn.torrent.config.PeerInfo;
import cn.torrent.config.PeersConfig;
import cn.torrent.enums.ChokeStatus;
import cn.torrent.enums.PieceStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerState {
    public final int peerID;
    public final int numPieces;
    public final CommonConfig commonConfig;
    private final PeersConfig peersConfig;
    private final HashMap<Integer, ArrayList<PieceStatus>> bitField = new HashMap<>();
    private final HashMap<Integer, Integer> pieceCounter = new HashMap<>();
    private final ArrayList<Integer> interested = new ArrayList<>();
    private final ArrayList<DownloadCounterPeerIdPair> downloadCounterList = new ArrayList<>();
    public final ConcurrentHashMap<Integer, ChokeStatus> neighbourChokeStatus = new ConcurrentHashMap<>();
    private Random random = new Random();
    public final ConcurrentHashMap<Integer, ChokeStatus> myChokeStatus = new ConcurrentHashMap<>();
    public final HashMap<Integer, SocketMessageReadWrite> IOHandlers;
    private int doneCounter = 0;

    private PeerState(
            final int peerID,
            final CommonConfig commonConfig,
            final PeersConfig peersConfig,
            HashMap<Integer, SocketMessageReadWrite> IOHandlers) {
        this.peerID = peerID;
        this.commonConfig = commonConfig;
        this.peersConfig = peersConfig;
        this.IOHandlers = IOHandlers;
        numPieces =
                commonConfig.fileSize / commonConfig.pieceSize
                        + (commonConfig.fileSize % commonConfig.pieceSize == 0 ? 0 : 1);
        for (PeerInfo peerInfo : peersConfig.peersList) {
            ArrayList<PieceStatus> bitset = new ArrayList<>(Arrays.asList(new PieceStatus[numPieces]));
            if (peerInfo.hasFile) {
                pieceCounter.put(peerInfo.peerID, numPieces);
                Collections.fill(bitset, PieceStatus.HAVE);
                doneCounter++;
            } else {
                Collections.fill(bitset, PieceStatus.MISSING);
            }
            bitField.put(peerInfo.peerID, bitset);
            if (peerInfo.peerID != peerID) {
                neighbourChokeStatus.put(peerInfo.peerID, ChokeStatus.CHOKED);
                myChokeStatus.put(peerInfo.peerID, ChokeStatus.CHOKED);
                downloadCounterList.add(new DownloadCounterPeerIdPair(peerInfo.peerID));
            }
        }
    }

    public static PeerState from(
            final int peerID,
            final CommonConfig commonConfig,
            final PeersConfig peersConfig, final HashMap<Integer, SocketMessageReadWrite> IOHandlers) {
        return new PeerState(peerID, commonConfig, peersConfig, IOHandlers);
    }

    public ArrayList<PeerInfo> getPeersConfig() {
        return peersConfig.peersList;
    }

    public synchronized void setHavePiece(final int peerID, final int index) {
        ArrayList<PieceStatus> peerPieceStatus = bitField.get(peerID);
        if (peerPieceStatus.get(index) != PieceStatus.HAVE) {
            peerPieceStatus.set(index, PieceStatus.HAVE);
            pieceCounter.put(peerID, pieceCounter.getOrDefault(peerID, 0) + 1);
        }
        if (pieceCounter.get(peerID) == numPieces) {
            doneCounter++;
        }
    }

    public synchronized byte[] getBitFieldOfPeer(final int peerID) {
        byte[] set = new byte[numPieces];
        for (int i = 0; i < numPieces; i++) {
            if (bitField.get(peerID).get(i) == PieceStatus.HAVE) {
                set[i] = 1;
            }
        }
        return set;
    }

    public synchronized void setBitFieldOfPeer(final int peerID, byte[] bytes) {
        pieceCounter.put(peerID, 0);
        for (int i = 0; i < numPieces; i++) {
            if (bytes[i] == 1) {
                bitField.get(peerID).set(i, PieceStatus.HAVE);
                pieceCounter.put(peerID, pieceCounter.get(peerID) + 1);
            } else {
                bitField.get(peerID).set(i, PieceStatus.MISSING);
            }
        }
    }

    public synchronized PieceStatus getStatusOfPiece(final int peerID, final int index) {
        return bitField.get(peerID).get(index);
    }

    public SocketMessageReadWrite getIOHandlerPeer(final int peerID) {
        return IOHandlers.get(peerID);
    }

    public synchronized boolean areAllDone() {
        return doneCounter == peersConfig.size();
    }

    public synchronized boolean checkMissingAndRequestIt(final int peerID, final int index) {
        if (bitField.get(peerID).get(index) == PieceStatus.MISSING) {
            if (bitField.get(peerID).get(index) != PieceStatus.HAVE) {
                bitField.get(peerID).set(index, PieceStatus.REQUESTED);
                return true;
            }
        }
        return false;
    }

    public synchronized void setMissingPiece(final int peerID, final int index) {
        if (bitField.get(peerID).get(index) != PieceStatus.HAVE)
            bitField.get(peerID).set(index, PieceStatus.MISSING);
    }

    public synchronized int getHaveCounter(final int peerID) {
        return pieceCounter.get(peerID);
    }

    public synchronized void incrementDownloadCounter(final int peerID) {
        downloadCounterList.stream()
                .filter(counter -> counter.peerID == peerID)
                .forEach(DownloadCounterPeerIdPair::increment);
    }

    public void updatePreferredNeighbors() {
        synchronized (downloadCounterList) {
            Collections.sort(downloadCounterList);
        }
        neighbourChokeStatus.entrySet().forEach(status -> status.setValue(ChokeStatus.CHOKED));
        int numPreferredNeighbors = commonConfig.numberOfPreferredNeighbors;
        int unchokedCounter = 0;
        for (DownloadCounterPeerIdPair highDownloadPeer : downloadCounterList) {
            if (interested.contains(highDownloadPeer.peerID)) {
                int peerID = highDownloadPeer.peerID;
                neighbourChokeStatus.put(peerID, ChokeStatus.UNCHOKED);
                unchokedCounter++;
            }
            if (unchokedCounter == numPreferredNeighbors) {
                break;
            }
        }
        for (DownloadCounterPeerIdPair counterPeerIdPair : downloadCounterList) {
            counterPeerIdPair.reset();
        }
    }

    public Optional<Integer> updateOptimisticNeighbor() {
        ArrayList<Integer> interestedAndChoked = new ArrayList<>();
        for (int interestedPeer : interested) {
            if (neighbourChokeStatus.get(interestedPeer) == ChokeStatus.CHOKED) {
                interestedAndChoked.add(interestedPeer);
            }
        }
        if (interestedAndChoked.size() > 0) {
            int randID = random.nextInt(interestedAndChoked.size());
            int peerID = interestedAndChoked.get(randID);
            neighbourChokeStatus.put(peerID, ChokeStatus.UNCHOKED);
            return Optional.of(peerID);
        }
        return Optional.empty();
    }

    public void addInterested(final int peerID) {
        if (!interested.contains(peerID)) {
            interested.add(peerID);
        }
    }
}

