package cn.torrent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonInfo {
    public int NumberOfPreferredNeighbors;
    public int UnChokingInterval;
    public int OptimisticUnChokingInterval;
    public int FileSize;
    public int PieceSize;
    public String FileName;

    private CommonInfo(final String CfgFilePath) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(CfgFilePath));

            String[] strings;

            String NumberOfPreferredNeighborsLine = bufferedReader.readLine();
            strings = NumberOfPreferredNeighborsLine.split("\\s+");
            NumberOfPreferredNeighbors = Integer.parseInt(strings[1]);

            String UnChokingIntervalLine = bufferedReader.readLine();
            strings = UnChokingIntervalLine.split("\\s+");
            UnChokingInterval = Integer.parseInt(strings[1]);

            String OptimisticUnChokingIntervalLine = bufferedReader.readLine();
            strings = OptimisticUnChokingIntervalLine.split("\\s+");
            OptimisticUnChokingInterval = Integer.parseInt(strings[1]);

            String FileNameLine = bufferedReader.readLine();
            strings = FileNameLine.split("\\s+");
            FileName = strings[1];

            String FileSizeLine = bufferedReader.readLine();
            strings = FileSizeLine.split("\\s+");
            FileSize = Integer.parseInt(strings[1]);

            String PieceSizeLine = bufferedReader.readLine();
            strings = PieceSizeLine.split("\\s+");
            PieceSize = Integer.parseInt(strings[1]);

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static CommonInfo from(final String CfgFilePath) {
        return new CommonInfo(CfgFilePath);
    }
}
