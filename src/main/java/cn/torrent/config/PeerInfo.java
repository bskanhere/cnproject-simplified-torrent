package cn.torrent.config;

public class PeerInfo {
    public final int peerID;
    public final String ipAddress;
    public final int port;
    public final boolean hasFile;

    public PeerInfo(int peerID, String ipAddress, int port, boolean hasFile) {
        this.peerID = peerID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.hasFile = hasFile;
    }
}