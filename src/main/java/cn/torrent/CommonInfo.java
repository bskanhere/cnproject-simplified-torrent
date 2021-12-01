package cn.torrent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonInfo {
    public int numberOfPreferredNeighbors;
    public int unChokingInterval;
    public int optimisticUnChokingInterval;
    public int fileSize;
    public int pieceSize;
    public String fileName;

    private CommonInfo(final String CfgFilePath) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(CfgFilePath));

            String[] strings;

            String numberOfPreferredNeighborsLine = bufferedReader.readLine();
            strings = numberOfPreferredNeighborsLine.split("\\s+");
            numberOfPreferredNeighbors = Integer.parseInt(strings[1]);

            String unChokingIntervalLine = bufferedReader.readLine();
            strings = unChokingIntervalLine.split("\\s+");
            unChokingInterval = Integer.parseInt(strings[1]);

            String optimisticUnChokingIntervalLine = bufferedReader.readLine();
            strings = optimisticUnChokingIntervalLine.split("\\s+");
            optimisticUnChokingInterval = Integer.parseInt(strings[1]);

            String fileNameLine = bufferedReader.readLine();
            strings = fileNameLine.split("\\s+");
            fileName = strings[1];

            String fileSizeLine = bufferedReader.readLine();
            strings = fileSizeLine.split("\\s+");
            fileSize = Integer.parseInt(strings[1]);

            String pieceSizeLine = bufferedReader.readLine();
            strings = pieceSizeLine.split("\\s+");
            pieceSize = Integer.parseInt(strings[1]);

        } catch (IOException e) {
            //Todo: Add Logger
        }
    }

    public static CommonInfo from(final String CfgFilePath) {
        return new CommonInfo(CfgFilePath);
    }
}
