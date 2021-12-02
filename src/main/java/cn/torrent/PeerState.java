package cn.torrent;

import cn.torrent.config.CommonInfo;
import cn.torrent.config.PeerInfo;
import cn.torrent.config.PeersInfo;
import cn.torrent.enums.ChokeStatus;
import cn.torrent.enums.PieceStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerState {
    public final int peerID;
    public final int numPieces;
    public final CommonInfo commonInfo;
    private final PeersInfo peersInfo;
    private final HashMap<Integer, ArrayList<PieceStatus>> bitField = new HashMap<>();
    private final HashMap<Integer,Integer> pieceCounter = new HashMap<>();

    private final ArrayList<Integer> interested = new ArrayList<>();
    private final ArrayList<DownloadCounterPeerIdPair> downloadCounterList = new ArrayList<>();
    public final ConcurrentHashMap<Integer, ChokeStatus> neighbourChokeStatus =
            new ConcurrentHashMap<>();
    private Random random = new Random();
    public final ConcurrentHashMap<Integer, ChokeStatus> myChokeStatus = new ConcurrentHashMap<>();
    public final HashMap<Integer, MessageIO> IOHandlers;
    private int doneCounter = 0;

    private PeerState(
            final int peerID,
            final CommonInfo commonInfo,
            final PeersInfo peersInfo,
            HashMap<Integer, MessageIO> IOHandlers) {
        this.peerID = peerID;
        this.commonInfo = commonInfo;
        this.peersInfo = peersInfo;
        this.IOHandlers = IOHandlers;
        numPieces =
                commonInfo.fileSize / commonInfo.pieceSize
                        + (commonInfo.fileSize % commonInfo.pieceSize == 0 ? 0 : 1);
        for (PeerInfo peerInfo : peersInfo.peerList) {
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
            final CommonInfo commonInfo,
            final PeersInfo peersInfo, final HashMap<Integer, MessageIO> IOHandlers) {
        return new PeerState(peerID, commonInfo, peersInfo, IOHandlers);
    }

    public ArrayList<PeerInfo> getPeersInfo() {
        return peersInfo.peerList;
    }

    public synchronized void setHavePiece(final int peerID, final int index) {
        ArrayList<PieceStatus> peerPieceStatus = bitField.get(peerID);
        if (peerPieceStatus.get(index) != PieceStatus.HAVE) {
            peerPieceStatus.set(index, PieceStatus.HAVE);
            pieceCounter.put(peerID, pieceCounter.getOrDefault(peerID, 0)+1);
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

    public MessageIO getIOHandlerPeer(final int peerID) {
        return IOHandlers.get(peerID);
    }

    public synchronized boolean areAllDone() {
        return doneCounter == peersInfo.size();
    }

    public synchronized boolean checkMissingAndRequestIt(final int peerID, final int index) {
        if (bitField.get(peerID).get(index) == PieceStatus.MISSING ) {
            if(bitField.get(peerID).get(index) != PieceStatus.HAVE) {
                bitField.get(peerID).set(index, PieceStatus.REQUESTED);
                return true;
            }
        }
        return false;
    }

    public synchronized void setMissingPiece(final int peerID, final int index) {
        if(bitField.get(peerID).get(index) != PieceStatus.HAVE)
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
        int numPreferredNeighbors = commonInfo.numberOfPreferredNeighbors;
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

class DownloadCounterPeerIdPair implements Comparable<DownloadCounterPeerIdPair> {

    private int downloadedPieces = 0;
    public final int peerID;

    DownloadCounterPeerIdPair(int peerID) {
        this.peerID = peerID;
    }

    public int getDownloadedPieces() {
        return downloadedPieces;
    }

    public void increment() {
        downloadedPieces++;
    }

    public void reset() {
        downloadedPieces = 0;
    }

    @Override
    public int compareTo(DownloadCounterPeerIdPair o) {
        return Integer.compare(o.downloadedPieces, downloadedPieces);
    }

}
