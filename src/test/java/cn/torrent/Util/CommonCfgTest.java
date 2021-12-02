package cn.torrent.Util;

import cn.torrent.config.CommonInfo;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CommonCfgTest {
    @Test
    public void readCfgFile() throws IOException {
        CommonInfo commonCfg = CommonInfo.from("src/test/resources/Common.cfg");
        Assert.assertEquals(commonCfg.numberOfPreferredNeighbors, 2);
        Assert.assertEquals(commonCfg.unChokingInterval, 5);
        Assert.assertEquals(commonCfg.optimisticUnChokingInterval, 15);
        Assert.assertEquals(commonCfg.fileName, "TheFile1.txt");
        Assert.assertEquals(commonCfg.fileSize, 10000232);
        Assert.assertEquals((commonCfg.pieceSize), 16384);
    }

}