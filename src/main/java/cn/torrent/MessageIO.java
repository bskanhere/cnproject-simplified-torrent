package cn.torrent;

import cn.torrent.exceptions.HandShakeException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MessageIO {
    public static final int INT_LENGTH = 4;
    public static final int TYPE_LENGTH = 1;
    public static final byte CHOKE = 0;
    public static final byte UNCHOKE = 1;
    public static final byte INTERESTED = 2;
    public static final byte NOT_INTERESTED = 3;
    public static final byte HAVE = 4;
    public static final byte BITFIELD = 5;
    public static final byte REQUEST = 6;
    public static final byte PIECE = 7;
    public static final byte INVALID = 8;
    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ0000000000";

    private final DataInputStream in;
    private final DataOutputStream out;
    private final Socket socket;

    public MessageIO(final Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(1000);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

    public void writeHandShakeMessage(final int peerID) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put(HANDSHAKE_HEADER.getBytes());
        byteBuffer.putInt(peerID);
        out.write(byteBuffer.array());
    }

    public int readHandShakeMessage() throws IOException, HandShakeException {
        byte[] headerBytes = new byte[28];
        int read = in.read(headerBytes);
        if (read != 28) {
            throw new HandShakeException();
        }
        String header = new String(headerBytes);
        if (!header.equals(HANDSHAKE_HEADER)) {
            throw new HandShakeException();
        }
        return in.readInt();
    }

    public byte[] readBitField(final int length) throws IOException {
        byte[] byteArray = new byte[length];
        int read = in.read(byteArray);
        if (read != length) {
            System.out.println("bit field read missing");
        }
        return byteArray;
    }

    public void writeBitField(final BitField bitField)
            throws IOException {
        byte[] byteArray = bitField.toByteArray();
        int length = INT_LENGTH + TYPE_LENGTH + byteArray.length;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(byteArray.length);
        buf.put(BITFIELD);
        buf.put(byteArray);
        out.write(buf.array());
    }

    public int readHave() throws IOException {
        return in.readInt();
    }

    public void writeHave(final int haveIndex) throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH + INT_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(INT_LENGTH);
        buf.put(HAVE);
        buf.putInt(haveIndex);
        out.write(buf.array());
    }

    public int readRequest() throws IOException {
        return in.readInt();
    }

    public void writeRequest(int requestIndex) throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH + INT_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(INT_LENGTH);
        buf.put(REQUEST);
        buf.putInt(requestIndex);
        out.write(buf.array());
    }

    public Piece readPiece(final int length) throws IOException {
        byte[] bytes = new byte[length - INT_LENGTH];
        int pieceIndex = in.readInt();
        int read = in.read(bytes);
        if (read != length - INT_LENGTH) {
            System.out.println("error reading piece");
        }
        return new Piece(pieceIndex, bytes);
    }

    public void writePiece(final Piece piece) throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH + INT_LENGTH + piece.bytes.length;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(INT_LENGTH + piece.bytes.length);
        buf.put(PIECE);
        buf.putInt(piece.pieceIndex);
        buf.put(piece.bytes);
        out.write(buf.array());
    }

    public synchronized void writeChoke() throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(0);
        buf.put(CHOKE);
        out.write(buf.array());
    }

    public synchronized void writeUnChoke() throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(0);
        buf.put(UNCHOKE);
        out.write(buf.array());
    }

    public void writeInterested() throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(0);
        buf.put(INTERESTED);
        out.write(buf.array());
    }

    public void writeNotInterested() throws IOException {
        int length = INT_LENGTH + TYPE_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.putInt(0);
        buf.put(NOT_INTERESTED);
        out.write(buf.array());
    }
}
