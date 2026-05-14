package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    @Override
    public void run() {
        try {
            initializeStreams();
            processClient();
            
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void initializeStreams() throws IOException {
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new PrintWriter(clientSocket.getOutputStream(), true);
    }
    
    private void processClient() throws IOException {
        String message;
        while ((message = input.readLine()) != null) {
            processMessage(message);
            
            if (shouldExit(message)) {
                break;
            }
        }
    }
    
    private void processMessage(String message) {
        System.out.println("Received: " + message);
        
        long startTime = System.nanoTime();
        String response = generateResponse(message);
        long endTime = System.nanoTime();
        
        long processingTime = (endTime - startTime) / 1000;
        output.println(response);
        
        System.out.println("Response sent in " + processingTime + " microseconds");
    }
    
    private String generateResponse(String message) {
        return "SERVER[" + System.currentTimeMillis() + "]: " + message.toUpperCase();
    }
    
    private boolean shouldExit(String message) {
        return message.equalsIgnoreCase("exit");
    }
    
    private void closeConnection() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
