package cn.torrent;

public class Piece {
    public final int pieceIndex;
    public final byte[] bytes;

    public Piece(int pieceIndex, byte[] bytes) {
        this.pieceIndex = pieceIndex;
        this.bytes = bytes;
    }
}
