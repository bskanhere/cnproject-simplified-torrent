package cn.torrent.peer;

public class DownloadCounterPeerIdPair implements Comparable<DownloadCounterPeerIdPair> {

    private int downloadedPieces = 0;
    public final int peerID;

    DownloadCounterPeerIdPair(int peerID) {
        this.peerID = peerID;
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
