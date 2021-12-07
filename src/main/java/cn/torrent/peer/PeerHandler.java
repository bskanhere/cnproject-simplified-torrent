package cn.torrent.peer;

import cn.torrent.FileHandler;
import cn.torrent.Log;
import cn.torrent.Piece;
import cn.torrent.SocketMessageReadWrite;
import cn.torrent.enums.ChokeStatus;
import cn.torrent.enums.MessageType;
import cn.torrent.enums.PieceStatus;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

import static cn.torrent.enums.MessageType.UNDEFINED;
import static cn.torrent.enums.MessageType.values;

public class PeerHandler implements Runnable {
    private final SocketMessageReadWrite readWrite;
    private final FileHandler fileHandler;
    private final PeerState state;
    private final int peerID;
    private final Log log;
    private final ArrayList<Integer> havePieces = new ArrayList<>();
    private final ArrayList<Integer> requested = new ArrayList<>();

    public PeerHandler(final PeerState state, int peerID, FileHandler fileHandler, Log log) {
        this.state = state;
        this.fileHandler = fileHandler;
        this.readWrite = state.getIOHandlerPeer(peerID);
        this.peerID = peerID;
        this.log = log;
    }

    @Override
    public void run() {
        try {
            while (!state.areAllDone()) {
                MessageType type = UNDEFINED;
                int len = 0;
                try {
                    len = readWrite.readInt();
                    byte temp = (readWrite.readByte());
                    for (MessageType val : values()) {
                        if (val.value == temp)
                            type = val;
                    }
                } catch (SocketTimeoutException ignored) {

                } catch (EOFException ex) {
                    break;
                }

                switch (type) {
                    case BITFIELD: {
                        handleBitField(len);
                        break;
                    }
                    case INTERESTED: {
                        handleInterested();
                        break;
                    }
                    case NOT_INTERESTED: {
                        handleNotInterested();
                        break;
                    }
                    case CHOKE: {
                        handleChoke();
                        break;
                    }
                    case UNCHOKE: {
                        handleUnChoke();
                        break;
                    }
                    case REQUEST: {
                        handleRequest();
                        break;
                    }
                    case PIECE: {
                        handlePiece(len);
                        break;
                    }
                    case HAVE: {
                        handleHave();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                log.flush();
            }
            log.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("count write bitfield message: " + state.peerID);
        }
    }

    private void handleBitField(int len) throws IOException {
        byte[] bitFieldBytes = readWrite.readBitField(len);
        state.setBitFieldOfPeer(peerID, bitFieldBytes);
        log.receivedBitField(peerID);
        boolean interested = false;
        if (state.getHaveCounter(peerID) == state.numPieces) {
            for (int i = 0; i < state.numPieces; i++) {
                havePieces.add(i);
            }
            interested = true;
        }
        if (interested) {
            sendInterested();
        } else {
            sendNotInterested();
        }
    }

    private void sendInterested() throws IOException {
        log.sendInterested(state.peerID, peerID);
        readWrite.writeInterested();
    }

    private void handleInterested() {
        log.receivedInterested(state.peerID, peerID);
        state.addInterested(peerID);
    }

    private void sendHave(final int index) throws IOException {
        HashMap<Integer, SocketMessageReadWrite> ios = state.IOHandlers;
        for (Map.Entry<Integer, SocketMessageReadWrite> set : ios.entrySet()) {
            if (!(index > state.numPieces)) {
                set.getValue().writeHave(index);
                log.sendHave(set.getKey(), index);
            }
        }
    }

    private void sendNotInterested() throws IOException {
        log.sendNotInterested(state.peerID, peerID);
        readWrite.writeNotInterested();
    }

    private void handleNotInterested() {
        log.receivedNotInterested(state.peerID, peerID);
    }

    private void handleUnChoke() throws IOException {
        log.receivedUnChoke(state.peerID, peerID);
        state.myChokeStatus.put(peerID, ChokeStatus.UNCHOKED);
        sendRequest();
    }

    private void handleChoke() {
        log.receivedChoke(state.peerID, peerID);
        for (int requestedIndex : requested) {
            state.setMissingPiece(state.peerID, requestedIndex);
        }
        requested.clear();
        state.myChokeStatus.put(peerID, ChokeStatus.CHOKED);
    }

    private void sendRequest() throws IOException {
        if (state.myChokeStatus.get(peerID) == ChokeStatus.UNCHOKED) {

            Optional<Integer> requestIndex = findAPieceToRequest();
            if (requestIndex.isPresent()) {
                readWrite.writeRequest(requestIndex.get());
                requested.add(requestIndex.get());
                log.sendRequest(peerID, requestIndex.get());
            }
        }
    }

    private Optional<Integer> findAPieceToRequest() {
        Collections.shuffle(havePieces);
        for (int havePiece : havePieces) {
            boolean requested = state.checkMissingAndRequestIt(state.peerID, havePiece);
            if (requested) {
                return Optional.of(havePiece);
            }
        }
        return Optional.empty();
    }

    private void handleRequest() throws IOException {
        int requestIndex = readWrite.readRequest();
        log.receivedRequest(peerID, requestIndex);
        if (state.neighbourChokeStatus.get(peerID) == ChokeStatus.UNCHOKED) {
            sendPiece(requestIndex);
        }
    }

    private void sendPiece(int requestIndex) {
        byte[] pieceBytes =
                fileHandler.getBytes(requestIndex * state.commonConfig.pieceSize, state.commonConfig.pieceSize);
        try {
            readWrite.writePiece(new Piece(requestIndex, pieceBytes));
            log.sentPiece(peerID, requestIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePiece(final int len) {
        try {
            Piece piece = readWrite.readPiece(len);
            state.incrementDownloadCounter(peerID);
            state.setHavePiece(state.peerID, piece.pieceIndex);
            requested.remove(Integer.valueOf(piece.pieceIndex));
            byte[] bytes = new byte[piece.bytes.length + 1];
            fileHandler.setBytes(piece.bytes, piece.pieceIndex * state.commonConfig.pieceSize);
            havePieces.remove(Integer.valueOf(piece.pieceIndex));
            log.downloadedPiece(state.peerID, peerID, piece.pieceIndex, state.getHaveCounter(state.peerID));
            sendHave(piece.pieceIndex);
            sendRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleHave() throws IOException {
        int have = readWrite.readHave();
        if (have > state.numPieces) return;
        log.receivedHave(state.peerID, peerID, have);
        state.setHavePiece(peerID, have);
        if (state.getStatusOfPiece(state.peerID, have) == PieceStatus.MISSING) {
            if (!havePieces.contains(have)) {
                havePieces.add(have);
            }
        }
        if (havePieces.size() > 0) {
            sendInterested();
        } else {
            sendNotInterested();
        }
    }
}
