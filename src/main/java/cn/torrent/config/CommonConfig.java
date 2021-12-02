package cn.torrent.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonConfig {
    public int numberOfPreferredNeighbors;
    public int unChokingInterval;
    public int optimisticUnChokingInterval;
    public String fileName;
    public int fileSize;
    public int pieceSize;

    private CommonConfig(int numberOfPreferredNeighbors, int unChokingInterval, int optimisticUnChokingInterval, String fileName, int fileSize, int pieceSize) {
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unChokingInterval = unChokingInterval;
        this.optimisticUnChokingInterval = optimisticUnChokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
    }

    public static CommonConfig from(final String cfgFilePath) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(cfgFilePath));

        String numberOfPreferredNeighborsLine = bufferedReader.readLine();
        int numberOfPreferredNeighbors = Integer.parseInt(numberOfPreferredNeighborsLine.split("\\s+")[1]);

        String unChokingIntervalLine = bufferedReader.readLine();
        int unChokingInterval = Integer.parseInt(unChokingIntervalLine.split("\\s+")[1]);

        String optimisticUnChokingIntervalLine = bufferedReader.readLine();
        int optimisticUnChokingInterval = Integer.parseInt(optimisticUnChokingIntervalLine.split("\\s+")[1]);

        String fileNameLine = bufferedReader.readLine();
        String fileName = fileNameLine.split("\\s+")[1];

        String fileSizeLine = bufferedReader.readLine();
        int fileSize = Integer.parseInt(fileSizeLine.split("\\s+")[1]);

        String pieceSizeLine = bufferedReader.readLine();
        int pieceSize = Integer.parseInt(pieceSizeLine.split("\\s+")[1]);

        return new CommonConfig(numberOfPreferredNeighbors, unChokingInterval, optimisticUnChokingInterval, fileName, fileSize, pieceSize);

    }
}
