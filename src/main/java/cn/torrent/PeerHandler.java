package cn.torrent;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

public class PeerHandler implements Runnable {
  private final MessageIO io;
  private final FileHandler fileHandler;
  private final PeerState state;
  private final int peerID;
  private final Logger logger;
  private final ArrayList<Integer> gotHavePieceIndexes = new ArrayList<>();
  private final ArrayList<Integer> requested = new ArrayList<>();

  public PeerHandler(final PeerState state, int peerID, FileHandler fileHandler, Logger logger)
          throws IOException {
    this.state = state;
    this.fileHandler = fileHandler;
    this.io = state.getIOHandlerPeer(peerID);
    this.peerID = peerID;
    this.logger = logger;
  }

  @Override
  public void run() {
    try {
      while (!state.areAllDone()) {
        byte type = MessageIO.INVALID;
        int len = 0;
        try {
          len = io.readInt();
          type = io.readByte();
        } catch (SocketTimeoutException ignored) {

        } catch (EOFException ex) {
          break;
        }

        switch (type) {
          case MessageIO.BITFIELD: {
              handleBitField(len);
              break;
            }
          case MessageIO.INTERESTED: {
              handleInterested();
              break;
            }
          case MessageIO.NOT_INTERESTED: {
              handleNotInterested();
              break;
            }
          case MessageIO.CHOKE: {
              handleChoke();
              break;
            }
          case MessageIO.UNCHOKE: {
              handleUnchoke();
              break;
            }
          case MessageIO.REQUEST: {
              handleRequest();
              break;
            }
          case MessageIO.PIECE: {
              handlePiece(len);
              break;
            }
          case MessageIO.HAVE: {
              handleHave();
              break;
            }
          default: {
              break;
            }
        }
        logger.flush();
      }
      logger.finishHandle(state.peerID, peerID);
      logger.flush();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("count write bitfield message: " + state.peerID);
    }
  }

  private void handleBitField(int len) throws IOException {
    byte[] bitFieldBytes = io.readBitField(len);
    BitField receivedBitField = state.getBitFieldOfPeer(peerID);
    receivedBitField.setBitField(bitFieldBytes);
    logger.gotBitField(peerID, receivedBitField);
    boolean amIInterested = false;
    if (receivedBitField.isFull()) {
      for (int i = 0; i < state.numPieces; i++) {
        gotHavePieceIndexes.add(i);
      }
      amIInterested = true;
    }
    if (amIInterested) {
      sendInterested();
    } else {
      sendNotInterested();
    }
  }

  private void sendInterested() throws IOException {
    logger.sendInterested(state.peerID, peerID);
    io.writeInterested();
  }

  private void handleInterested() throws IOException {
    logger.interested(state.peerID, peerID);
    state.addInterested(peerID);
  }

  private void sendHave(final int index) throws IOException {
    HashMap<Integer, MessageIO> ios = state.IOHandlers;
    for (Map.Entry<Integer, MessageIO> set : ios.entrySet()) {
      set.getValue().writeHave(index);
      logger.sendHave(set.getKey(), index);
    }
  }

  private void sendNotInterested() throws IOException {
    logger.sendNotInterested(state.peerID, peerID);
    io.writeNotInterested();
  }

  private void handleNotInterested() throws IOException {
    logger.notInterested(state.peerID, peerID);
  }

  private void handleUnchoke() throws IOException {
    logger.unChoke(state.peerID, peerID);
      state.myChokeStatus.put(peerID, PeerState.ChokeStatus.UNCHOKED);
      sendRequest();
  }

  private void handleChoke() {
    logger.choke(state.peerID, peerID);
    for (int requestedIndex : requested) {
      state.setMissingPiece(state.peerID, requestedIndex);
    }
    requested.clear();
    state.myChokeStatus.put(peerID, PeerState.ChokeStatus.CHOKED);
  }

  private void sendRequest() throws IOException {
    if (state.myChokeStatus.get(peerID) == PeerState.ChokeStatus.UNCHOKED) {

      Optional<Integer> requestIndex = getAPieceToRequest();
      if (requestIndex.isPresent()) {
        io.writeRequest(requestIndex.get());
        requested.add(requestIndex.get());
        logger.sendRequest(peerID, requestIndex.get());
      }
    }
  }

  private Optional<Integer> getAPieceToRequest() {
    Collections.shuffle(gotHavePieceIndexes);
    for (int havePiece : gotHavePieceIndexes) {
      boolean requested = state.checkMissingAndRequestIt(state.peerID, havePiece);
      if (requested) {
        return Optional.of(havePiece);
      }
    }
    return Optional.empty();
  }

  private void handleRequest() throws IOException {
    int requestIndex = io.readRequest();
    logger.request(peerID, requestIndex);
    if (state.neighbourChokeStatus.get(peerID)== PeerState.ChokeStatus.UNCHOKED) {
      sendPiece(requestIndex);
    }
  }

  private void sendPiece(int requestIndex) {
    byte[] pieceBytes =
        fileHandler.get(requestIndex * state.commonInfo.PieceSize, state.commonInfo.PieceSize);
    try {
      io.writePiece(new Piece(requestIndex, pieceBytes));
      logger.sentPiece(peerID, requestIndex);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handlePiece(final int len) {
    try {
      Piece piece = io.readPiece(len);
      state.incrementDownloadCounter(peerID);
      state.setHavePiece(state.peerID, piece.pieceIndex);
      requested.remove(Integer.valueOf(piece.pieceIndex));
      byte[] bytes = new byte[piece.bytes.length+1];
      fileHandler.set(piece.bytes, piece.pieceIndex * state.commonInfo.PieceSize);
      gotHavePieceIndexes.remove(Integer.valueOf(piece.pieceIndex));
      logger.download(state.peerID, peerID, piece.pieceIndex, state.getHaveCounter(state.peerID));
      sendHave(piece.pieceIndex);
      sendRequest();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleHave() throws IOException {
    int have = io.readHave();
    logger.have(state.peerID, peerID, have);
    state.setHavePiece(peerID, have);
    if (state.getStatusOfPiece(state.peerID, have) == PieceStatus.MISSING) {
      if (!gotHavePieceIndexes.contains(have)) {
        gotHavePieceIndexes.add(have);
      }
    }
    if (gotHavePieceIndexes.size() > 0) {
      sendInterested();
    } else {
      sendNotInterested();
    }
  }
}
