package cn.torrent;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class PeersInfo {

    public final ArrayList<PeerInfo> peerInfoList = new ArrayList<>();

    private PeersInfo(final String peerInfoFilePath) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(peerInfoFilePath));
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] lineSplit = currentLine.split("\\s+");
                final int peerID = Integer.parseInt(lineSplit[0]);
                final String ipAddress = lineSplit[1];
                final int port = Integer.parseInt(lineSplit[2]);
                final boolean hasFile = lineSplit[3].equals("1");
                PeerInfo peerInfo = new PeerInfo(peerID, ipAddress, port, hasFile);
                peerInfoList.add(peerInfo);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private PeersInfo(final ArrayList<PeerInfo> peersInfoList) {
        peerInfoList.addAll(peersInfoList);
    }

    public Optional<PeerInfo> get(final int peerID) {
        return peerInfoList.stream().filter(s -> s.peerID == peerID).findFirst();
    }

    public int size() {
        return peerInfoList.size();
    }

    public ArrayList<PeerInfo> before(final int beforeMe) {
        ArrayList<PeerInfo> res = new ArrayList<>();
        for (PeerInfo peerInfo : peerInfoList) {
            if (peerInfo.peerID == beforeMe) break;
            else {
                res.add(peerInfo);
            }
        }
        return res;
    }

    public ArrayList<PeerInfo> after(final int afterMe) {
        ArrayList<PeerInfo> res = new ArrayList<>();
        boolean found = false;
        for (PeerInfo peerInfo : peerInfoList) {
            if (peerInfo.peerID == afterMe) {
                found = true;
                continue;
            }
            if (found) {
                res.add(peerInfo);
            }
        }
        return res;
    }

    public static PeersInfo from(final String peerInfoFilePath) {
        return new PeersInfo(peerInfoFilePath);
    }

    public static PeersInfo from(final ArrayList<PeerInfo> peersInfoList) {
        return new PeersInfo(peersInfoList);
    }
}
