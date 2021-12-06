package cn.torrent.peer;

import cn.torrent.SocketMessageReadWrite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class ConnectionRequestHandler implements Runnable {
    final int port;
    int expectedConnections;
    final ArrayList<SocketMessageReadWrite> readWriteHandlers;

    ConnectionRequestHandler(int port, int expectedConnections, ArrayList<SocketMessageReadWrite> readWriteHandlers) {
        this.port = port;
        this.expectedConnections = expectedConnections;
        this.readWriteHandlers = readWriteHandlers;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port, 200);
            while (expectedConnections-- > 0) {
                Socket clientSocket = serverSocket.accept();
                readWriteHandlers.add(new SocketMessageReadWrite(clientSocket));
            }
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("cant close server");
            e.printStackTrace();
        }
    }
}
