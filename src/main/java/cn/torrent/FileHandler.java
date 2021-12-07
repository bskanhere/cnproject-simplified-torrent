package cn.torrent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class FileHandler {
    private RandomAccessFile file;

    public FileHandler(final String filepath) {
        try {
            file = new RandomAccessFile(filepath, "rw");
        } catch (FileNotFoundException e) {
            System.out.println("File not Found");
        }
    }

    public synchronized byte[] getBytes(int index, int length) {
        byte[] piece = new byte[length];
        try {
            file.seek(index);
            int bytesRead = 0;
            bytesRead = file.read(piece);
            if (bytesRead == length) {
                return piece;
            } else if (bytesRead == -1) {
                System.out.println("Cannot read from file at index: " + index + " for length: " + length);
            } else {
                return Arrays.copyOf(piece, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    public synchronized void setBytes(byte[] arr, int index) {
        try {
            file.seek(index);
            file.write(arr);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }


}
