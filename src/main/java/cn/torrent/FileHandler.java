package cn.torrent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class FileHandler {
  public FileHandler(final String filepath) {
    try {
      randomAccessFile = new RandomAccessFile(filepath, "rw");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public synchronized byte[] get(int index, int length) {
    byte[] piece = new byte[length];
    try {
      randomAccessFile.seek(index);
      int bytesRead = 0;
      bytesRead = randomAccessFile.read(piece);
      if (bytesRead == length) {
        return piece;
      }
      else if (bytesRead == -1){
        System.out.println("cant read from file, index: " + index + " length: " + length);
      }
      else {
        return Arrays.copyOf(piece, bytesRead);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
    return null;
  }

  public synchronized void set(byte[] arr, int index) {
    try {
      randomAccessFile.seek(index);
      randomAccessFile.write(arr);
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  public void close()
  {
    try {
      randomAccessFile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  private RandomAccessFile randomAccessFile;
}
