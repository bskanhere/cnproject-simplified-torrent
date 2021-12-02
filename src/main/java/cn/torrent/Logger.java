package cn.torrent;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Logger {

    private PrintWriter printWriter;

    public Logger(final String logFilePath) {
        try {
            printWriter = new PrintWriter(logFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized void makesConnection(final int self, final int peer) {
        printWriter.printf(
                "%s : Peer %s makes a connection to Peer %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void isConnected(final int self, final int peer) {
        printWriter.printf(
                "%s : Peer %s is connected from Peer %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void sendBitField(final int peer) {
        printWriter.printf(
                "%s : sent a bitfield to %s with file.\n", LocalDateTime.now(), peer);
    }

    public synchronized void receivedBitField(final int peer) {
        printWriter.printf(
                "%s : received a bitfield from %s with file.\n",
                LocalDateTime.now(), peer);
    }

    public synchronized void sendChoke(final int self, final int peer) {
        printWriter.printf("%s : Peer %s sent choke to %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void receivedChoke(final int self, final int peer) {
        printWriter.printf("%s : Peer %s is choked by %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void sendUnChoke(final int self, final int peer) {
        printWriter.printf("%s : Peer %s sent unchoke to %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void receivedUnChoke(final int self, final int peer) {
        printWriter.printf("%s : Peer %s is unchoked by %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void sendRequest(final int peer, final int pieceIndex) {
        printWriter.printf("%s : requesting piece %s from %s.\n", LocalDateTime.now(), pieceIndex, peer);
    }

    public synchronized void receivedRequest(final int peer, final int pieceIndex) {
        printWriter.printf("%s : Peer %s requested piece %s.\n", LocalDateTime.now(), peer, pieceIndex);
    }

    public synchronized void sendHave(final int peer, final int index) {
        printWriter.printf("%s : send have to %s index: %s \n", LocalDateTime.now(), peer, index);
    }

    public synchronized void receivedHave(final int self, final int peer, final int pieceIndex) {
        printWriter.printf(
                "%s : Peer %s received the ‘have’ message from %s for the piece %s.\n",
                LocalDateTime.now(), self, peer, pieceIndex);
    }

    public synchronized void sendInterested(final int self, final int peer) {
        printWriter.printf(
                "%s : Peer %s send ‘interested’ message to %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void receivedInterested(final int self, final int peer) {
        printWriter.printf(
                "%s : Peer %s received the ‘interested’ message from %s.\n",
                LocalDateTime.now(), self, peer);
    }

    public synchronized void sendNotInterested(final int self, final int peer) {
        printWriter.printf(
                "%s : Peer %s sent ‘not interested’ message to %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void receivedNotInterested(final int self, final int peer) {
        printWriter.printf(
                "%s : Peer %s received the ‘not interested’ message from %s.\n",
                LocalDateTime.now(), self, peer);
    }


    public synchronized void sentPiece(final int peer, final int pieceIndex) {
        printWriter.printf("%s : sent piece %s to %s \n", LocalDateTime.now(), pieceIndex, peer);
    }

    public synchronized void downloadedPiece(
            final int self, final int peer, final int pieceIndex, final int numPieces) {
        printWriter.printf(
                "%s : Peer %s has downloaded the piece %s from %s. Now the number of pieces it has is %s.\n",
                LocalDateTime.now(), self, pieceIndex, peer, numPieces);
    }

    public synchronized void complete(final int self) {
        printWriter.printf(
                "%s : Peer %s has downloaded the complete file.\n", LocalDateTime.now(), self);
    }


    public synchronized void changesPreferredNeighbors(final int peer, List<Integer> preferredNeighbors) {
        String preferredNeighborsString = preferredNeighbors.stream().map(Objects::toString).collect(Collectors.joining(", "));
        printWriter.printf(
                "%s : Peer %s has the preferred neighbors %s.\n", LocalDateTime.now(), peer, preferredNeighborsString);
    }

    public synchronized void changesOptimisticallyUnChokedNeighbor(final int self, final int peer) {
        printWriter.printf("%s: Peer %s has the optimistically unchoked neighbor %s.\n", LocalDateTime.now(), self, peer);
    }

    public synchronized void error(Exception e) {
        printWriter.printf(e.getMessage());
        e.printStackTrace(printWriter);
    }

    public void close() {
        printWriter.close();
    }

    public void flush() {
        printWriter.flush();
    }

}
