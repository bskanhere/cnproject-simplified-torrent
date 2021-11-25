package cn.torrent;

public class Piece {
    final int pieceIndex;
    final byte[] bytes;

    public Piece(int pieceIndex, byte[] bytes) {
        this.pieceIndex = pieceIndex;
        this.bytes = bytes;
    }
}
