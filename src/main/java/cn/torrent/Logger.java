package cn.torrent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.ArrayList;

public class Logger {

  private PrintWriter printWriter;

  public Logger(final String logFilePath) {
    try {
      printWriter = new PrintWriter(logFilePath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public synchronized void connection(final int peer1, final int peer2) throws IOException {
    printWriter.printf(
        "%s : Peer %s makes a connection to Peer %s.\n", LocalTime.now(), peer1, peer2);
  }

  public synchronized void connected(final int peer1, final int peer2) {
    printWriter.printf(
        "%s : Peer %s is connected from Peer %s.\n", LocalTime.now(), peer1, peer2);
  }

  public synchronized void error(String message, Exception e) {
    printWriter.printf(message);
    e.printStackTrace(printWriter);
  }


  public synchronized void unChoke(final int peer1, final int neighbor) {
    printWriter.printf("%s : uf.Peer %s is unchoked by %s.\n", LocalTime.now(), peer1, neighbor);
  }

  public synchronized void sendUnChoke(final int peer1, final int neighbor) {
    printWriter.printf("%s : uf.Peer %s sent unchoke to %s.\n", LocalTime.now(), peer1, neighbor);
  }

  public synchronized void choke(final int peer1, final int neighbor) {
    printWriter.printf("%s : uf.Peer %s is choked by %s.\n", LocalTime.now(), peer1, neighbor);
  }

  public synchronized void sendChoke(final int peer1, final int neighbor) {
    printWriter.printf("%s : uf.Peer %s sent choke to %s.\n", LocalTime.now(), peer1, neighbor);
  }

  public synchronized void sendBitField(final int neigh, final BitField bitField) {
    printWriter.printf(
            "%s : sent a bitfield to %s with file %s \n",
            LocalTime.now(), neigh, bitField.isFull() ? "FULL" : "EMPTY");
  }

  public synchronized void gotBitField(final int neigh, final BitField bitField) {
    printWriter.printf(
            "%s : received a bitfield from %s with file %s \n",
            LocalTime.now(), neigh, bitField.isFull() ? "FULL" : "EMPTY");
  }

  public synchronized void request(final int peer1, final int request) {
    printWriter.printf("%s : uf.Peer %s requested %s.\n", LocalTime.now(), peer1, request);
  }

  public synchronized void sendRequest(final int peer1, final int request) {
    printWriter.printf("%s : requesting %s from %s.\n", LocalTime.now(), request, peer1);
  }

  public synchronized void optimisticallyUnChoking(final int peer1, final int neighbor) {
    printWriter.printf(
            "%s : uf.Peer %s has the optimistically unchoked neighbor %s.\n",
            LocalTime.now(), peer1, neighbor);
  }


  public synchronized void have(final int peer1, final int peer2, final int pieceIndex) {
    printWriter.printf(
            "%s : uf.Peer %s received the ‘have’ message from %s for the piece %s.\n",
            LocalTime.now(), peer1, peer2, pieceIndex);
  }

  public synchronized void sendInterested(final int peer1, final int peer2) {
    printWriter.printf(
            "%s : uf.Peer %s send ‘interested’ message to %s.\n", LocalTime.now(), peer1, peer2);
  }

  public synchronized void sendNotInterested(final int peer1, final int peer2) {
    printWriter.printf(
            "%s : uf.Peer %s sent ‘not interested’ message to %s.\n", LocalTime.now(), peer1, peer2);
  }

  public synchronized void interested(final int peer1, final int peer2) {
    printWriter.printf(
            "%s : uf.Peer %s received the ‘interested’ message from %s.\n",
            LocalTime.now(), peer1, peer2);
  }

  public synchronized void notInterested(final int peer1, final int peer2) {
    printWriter.printf(
            "%s : uf.Peer %s received the ‘not interested’ message from %s.\n",
            LocalTime.now(), peer1, peer2);
  }


  public synchronized void sentPiece(final int neigh, final int pieceIndex) {
    printWriter.printf("%s : sent piece %s to %s \n", LocalTime.now(), pieceIndex, neigh);
  }

  public synchronized void download(
          final int peer1, final int peer2, final int pieceIndex, final int numPieces) {
    printWriter.printf(
            "%s : uf.Peer %s has downloaded the piece %s from %s. Now the number of pieces it has is %s.\n",
            LocalTime.now(), peer1, pieceIndex, peer2, numPieces);
  }

  public synchronized void complete(final int peer1) {
    printWriter.printf(
            "%s : uf.Peer %s has downloaded the complete file.\n", LocalTime.now(), peer1);
  }

  public synchronized void sendHave(final int peer, final int index) {
    printWriter.printf("%s : send have to %s index: %s \n", LocalTime.now(), peer, index);
  }

  public synchronized void finishHandle(final int peer1, final int neigh) {
    printWriter.printf(
            "%s : uf.Peer %s has downloaded the complete file from %s\n", LocalTime.now(), peer1, neigh);
  }

  public void close() {
    printWriter.close();
  }

  public void flush() {
    printWriter.flush();
  }

}
