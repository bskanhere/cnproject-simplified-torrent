package cn.torrent.Util;

import cn.torrent.PeerInfo;
import cn.torrent.PeersInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

public class PeersInfoTest {

  public final String peerInfoFilePath = "src/test/resources/PeerInfoTestData.cfg";
  PeersInfo peersInfo = PeersInfo.from(peerInfoFilePath);

  @Test
  public void readPeerInfoFile() {
    PeerInfo expectedPeerInfo = new PeerInfo(1001, "127.0.0.1", 3000, true);
    Optional<PeerInfo> peerInfo = peersInfo.get(1001);
    Assert.assertTrue(peerInfo.isPresent());
    Assert.assertEquals(expectedPeerInfo.ipAddress, peerInfo.get().ipAddress);
    Assert.assertEquals(expectedPeerInfo.port, peerInfo.get().port);
    Assert.assertEquals(expectedPeerInfo.hasFile, peerInfo.get().hasFile);
  }

  @Test
  public void beforeTest(){
    ArrayList<PeerInfo> before = peersInfo.before(1003);
    Assert.assertEquals(2, before.size());
    Assert.assertEquals(1001, before.get(0).peerID);
    Assert.assertEquals(1002, before.get(1).peerID);
  }

  @Test
  public void afterTest(){
    ArrayList<PeerInfo> after = peersInfo.after(1003);
    Assert.assertEquals(3, after.size());
    Assert.assertEquals(1004, after.get(0).peerID);
    Assert.assertEquals(1005, after.get(1).peerID);
    Assert.assertEquals(1006, after.get(2).peerID);
  }

}
