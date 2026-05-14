package org.example;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 100;
    
    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);
        System.out.println("Thread pool size: " + THREAD_POOL_SIZE);
        
        Server server = new Server();
        server.start();
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from: " + clientSocket.getInetAddress());
                
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
            }
            
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
