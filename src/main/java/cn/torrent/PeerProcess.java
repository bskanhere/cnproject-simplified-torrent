package cn.torrent;

import cn.torrent.config.CommonConfig;
import cn.torrent.config.PeersConfig;
import cn.torrent.peer.Peer;

import java.io.IOException;

public class PeerProcess {

    public static final String PEER_INFO_PATH = "src/main/resources/PeerInfo.cfg";
    public static final String COMMON_CONFIG_PATH = "src/main/resources/Common.cfg";
    public static final String LOG_FILE_PATH = "logger%s.log";

    public static void main(String[] args) throws IOException {
        PeersConfig peersConfig = PeersConfig.from(PEER_INFO_PATH);
        CommonConfig commonConfig = CommonConfig.from(COMMON_CONFIG_PATH);
        Peer peer = new Peer(Integer.parseInt(args[0]), commonConfig, peersConfig, String.format(LOG_FILE_PATH, args[0]));
        peer.run();
    }
}
