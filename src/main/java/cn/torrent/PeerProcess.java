package cn.torrent;

public class PeerProcess {
  public static void main(String[] args) {
      PeersInfo peersInfo = PeersInfo.from(Constants.PEER_INFO_PATH);
      CommonInfo commonInfo = CommonInfo.from(Constants.COMMON_CONFIG_PATH);
      Peer peer = new Peer(Integer.parseInt(args[0]), commonInfo, peersInfo, String.format(Constants.LOG_FILE_PATH, args[0]));
      peer.run();
  }
}
