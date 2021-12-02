package cn.torrent.config;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class PeersInfo {

    public final ArrayList<PeerInfo> peerList = new ArrayList<>();

    private PeersInfo(final ArrayList<PeerInfo> peersInfoList) {
        peerList.addAll(peersInfoList);
    }

    public Optional<PeerInfo> get(final int peerID) {
        return peerList.stream().filter(s -> s.peerID == peerID).findFirst();
    }

    public int size() {
        return peerList.size();
    }

    public ArrayList<PeerInfo> before(final int beforeMe) {
        ArrayList<PeerInfo> res = new ArrayList<>();
        for (PeerInfo peer : peerList) {
            if (peer.peerID == beforeMe) break;
            else {
                res.add(peer);
            }
        }
        return res;
    }

    public ArrayList<PeerInfo> after(final int afterMe) {
        ArrayList<PeerInfo> res = new ArrayList<>();
        boolean found = false;
        for (PeerInfo peer : peerList) {
            if (peer.peerID == afterMe) {
                found = true;
                continue;
            }
            if (found) {
                res.add(peer);
            }
        }
        return res;
    }

    public static PeersInfo from(final String peerInfoFilePath) throws IOException {
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
        return new PeersInfo(peerList);
    }
}
