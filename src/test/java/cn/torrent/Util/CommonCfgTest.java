package cn.torrent.Util;

import cn.torrent.CommonInfo;
import org.junit.Assert;
import org.junit.Test;

public class CommonCfgTest {
    @Test
    public void readCfgFile(){
        CommonInfo commonCfg = CommonInfo.from("src/test/resources/Common.cfg");
        Assert.assertEquals(commonCfg.NumberOfPreferredNeighbors, 2);
        Assert.assertEquals(commonCfg.UnChokingInterval, 5);
        Assert.assertEquals(commonCfg.OptimisticUnChokingInterval, 15);
        Assert.assertEquals(commonCfg.FileName, "TheFile.txt");
        Assert.assertEquals(commonCfg.FileSize, 10000232);
        Assert.assertEquals((commonCfg.PieceSize), 16384);
    }

}