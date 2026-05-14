package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("Client started. Type 'exit' to quit.");

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to server: " + SERVER_ADDRESS + ":" + PORT);

            String userMessage;
            while (true) {
                System.out.print("> ");
                userMessage = userInput.readLine();
                if (userMessage == null) break;

                out.println(userMessage);
                if (userMessage.equalsIgnoreCase("exit")) break;

                String response = in.readLine();
                System.out.println("Server response: " + response);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
