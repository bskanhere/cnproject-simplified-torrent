package cn.torrent;

public class PeerProcess {

    public static final String PEER_INFO_PATH = "src/main/resources/PeerInfo.cfg";
    public static final String COMMON_CONFIG_PATH = "src/main/resources/Common.cfg";
    public static final String LOG_FILE_PATH = "logger%s.log";

    public static void main(String[] args) {
        PeersInfo peersInfo = PeersInfo.from(PEER_INFO_PATH);
        CommonInfo commonInfo = CommonInfo.from(COMMON_CONFIG_PATH);
        Peer peer = new Peer(Integer.parseInt(args[0]), commonInfo, peersInfo, String.format(LOG_FILE_PATH, args[0]));
        peer.run();
    }
}
