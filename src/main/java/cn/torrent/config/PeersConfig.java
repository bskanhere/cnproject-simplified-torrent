package cn.torrent.config;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PeersConfig {

    public final ArrayList<PeerInfo> peersList = new ArrayList<>();

    private PeersConfig(final ArrayList<PeerInfo> peersInfoList) {
        peersList.addAll(peersInfoList);
    }

    public Optional<PeerInfo> get(final int peerID) {
        return peersList.stream().filter(s -> s.peerID == peerID).findFirst();
    }

    public List<PeerInfo> getPeersList() {
        return peersList;
    }

    public int size() {
        return peersList.size();
    }

    public static PeersConfig from(final String peerInfoFilePath) throws IOException {
        ArrayList<PeerInfo> peerList = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(peerInfoFilePath));
        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] lineSplit = currentLine.split("\\s+");
            final int peerID = Integer.parseInt(lineSplit[0]);
            final String ipAddress = lineSplit[1];
            final int port = Integer.parseInt(lineSplit[2]);
            final boolean hasFile = lineSplit[3].equals("1");
            PeerInfo peer = new PeerInfo(peerID, ipAddress, port, hasFile);
            peerList.add(peer);
        }
        return new PeersConfig(peerList);
    }
}
