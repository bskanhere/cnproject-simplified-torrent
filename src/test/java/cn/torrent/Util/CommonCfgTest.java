package cn.torrent.Util;

import cn.torrent.config.CommonConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CommonCfgTest {
    @Test
    public void readCfgFile() throws IOException {
        CommonConfig commonCfg = CommonConfig.from("src/test/resources/Common.cfg");
        Assert.assertEquals(commonCfg.numberOfPreferredNeighbors, 2);
        Assert.assertEquals(commonCfg.unChokingInterval, 5);
        Assert.assertEquals(commonCfg.optimisticUnChokingInterval, 15);
        Assert.assertEquals(commonCfg.fileName, "TheFile.dat");
        Assert.assertEquals(commonCfg.fileSize, 10000232);
        Assert.assertEquals((commonCfg.pieceSize), 16384);
    }

}