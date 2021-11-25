package cn.torrent;

import cn.torrent.exceptions.AlreadyHavePieceException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerState {
  public final int peerID;
  public final int numPieces;
  public final CommonInfo commonInfo;
  private final PeersInfo peersInfo;
  private final HashMap<Integer, BitField> bitFieldMap = new HashMap<>();
  private final ArrayList<Integer> interested = new ArrayList<>();
  private final ArrayList<DownloadCounterPeerIdPair> downloadCounterList = new ArrayList<>();
  public final ConcurrentHashMap<Integer, ChokeStatus> neighbourChokeStatus =
      new ConcurrentHashMap<>();
  private Random random = new Random();
  public final ConcurrentHashMap<Integer, ChokeStatus> myChokeStatus = new ConcurrentHashMap<>();
  // Once initialized no update/inserts are allowed, read-only map does not need synchronized.
  public final HashMap<Integer, MessageIO> IOHandlers;
  private boolean allDone = false;
  private int doneCounter = 0;

  public enum ChokeStatus {
    CHOKED,
    UNCHOKED
  }

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
        commonInfo.FileSize / commonInfo.PieceSize
            + (commonInfo.FileSize % commonInfo.PieceSize == 0 ? 0 : 1);
    for (PeerInfo peerInfo : peersInfo.peerInfoList) {
      BitField bitField = BitField.from(numPieces, peerInfo.hasFile);
      if (peerInfo.hasFile) doneCounter++;
      bitFieldMap.put(peerInfo.peerID, bitField);
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
      final PeersInfo peerInfo,
      final HashMap<Integer, MessageIO> IOHandlers) {
    return new PeerState(peerID, commonInfo, peerInfo, IOHandlers);
  }

  public ArrayList<PeerInfo> getPeersInfo() {
    return peersInfo.peerInfoList;
  }

  public synchronized void setRequestedPiece(final int peerID, final int index) {
    try {
      bitFieldMap.get(peerID).setRequested(index);
    } catch (AlreadyHavePieceException e) {
      e.printStackTrace();
      System.out.println(peerID + " Already has piece " + index);
    }
  }

  public synchronized void setHavePiece(final int peerID, final int index) {
    BitField bitField = bitFieldMap.get(peerID);
    try {
      bitField.setHave(index);
    } catch (AlreadyHavePieceException e) {
      e.printStackTrace();
      System.out.println(peerID + " Already has piece " + index);
      return;
    }
    if (bitField.isFull()) {
      doneCounter++;
      if (doneCounter == peersInfo.size()) {
        allDone = true;
      }
    }
  }

  public synchronized BitField getBitFieldOfPeer(final int peerID) {
    return bitFieldMap.get(peerID);
  }

  public synchronized PieceStatus getStatusOfPiece(final int peerID, final int index) {
    BitField bitField = bitFieldMap.get(peerID);
    return bitField.getStatus(index);
  }

  public MessageIO getIOHandlerPeer(final int peerID) {
    return IOHandlers.get(peerID);
  }

  public synchronized boolean areAllDone() {
    return allDone;
  }

  public synchronized boolean checkMissingAndRequestIt(final int peerID, final int index) {
    BitField bitField = bitFieldMap.get(peerID);
    if (bitField.getStatus(index) == PieceStatus.MISSING) {
      try {
        bitField.setRequested(index);
        return true;
      } catch (AlreadyHavePieceException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  public synchronized void setMissingPiece(final int peerID, final int index) {
    BitField bitField = bitFieldMap.get(peerID);
    try {
      bitField.setMissing(index);
    } catch (AlreadyHavePieceException e) {
      e.printStackTrace();
    }
  }

  public synchronized int getHaveCounter(final int peerID) {
    return bitFieldMap.get(peerID).haveCounter;
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
    int numPreferredNeighbors = commonInfo.NumberOfPreferredNeighbors;
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

  private int downloadedPieces = 0;
  public final int peerID;
}
