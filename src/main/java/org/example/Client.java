package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private static final int TOTAL_SESSIONS = 100;
    private static final int MESSAGES_PER_SESSION = 5;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting " + TOTAL_SESSIONS + " client sessions...");
        
        long startTime = System.currentTimeMillis();
        AtomicInteger completed = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> times = new ConcurrentLinkedQueue<>();
        
        ExecutorService pool = Executors.newFixedThreadPool(50);
        
        for (int i = 0; i < TOTAL_SESSIONS; i++) {
            final int sessionId = i;
            pool.execute(() -> {
                try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    
                    for (int j = 0; j < MESSAGES_PER_SESSION; j++) {
                        String msg = "Session_" + sessionId + "_Msg_" + j;
                        long send = System.nanoTime();
                        out.println(msg);
                        in.readLine();
                        long receive = System.nanoTime();
                        times.add((receive - send) / 1000);
                    }
                    
                    int done = completed.incrementAndGet();
                    if (done % 10 == 0) {
                        System.out.println("Progress: " + done + "/" + TOTAL_SESSIONS);
                    }
                    
                } catch (IOException e) {
                    System.err.println("Session " + sessionId + " failed: " + e.getMessage());
                }
            });
        }
        
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        double avg = times.stream().mapToLong(l -> l).average().orElse(0);
        
        System.out.println("\n=== RESULTS ===");
        System.out.println("Completed sessions: " + completed.get() + "/" + TOTAL_SESSIONS);
        System.out.println("Total messages: " + times.size());
        System.out.println("Total time: " + (endTime - startTime) + " ms");
        System.out.println("Average processing: " + String.format("%.2f", avg) + " microseconds");
        System.out.println("Throughput: " + String.format("%.2f", (times.size() * 1000.0) / (endTime - startTime)) + " msg/sec");
    }
}
