package org.example;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MultiThread {
    
    private static final int DEFAULT_THREAD_COUNT = 4;
    private static final int BUFFER_SIZE = 8192;
    
    static class DownloadTask implements Runnable {
        private final String fileURL;
        private final String savePath;
        private final int threadId;
        private final int startByte;
        private final int endByte;
        private final CountDownLatch latch;
        private final ProgressTracker progress;
        
        public DownloadTask(String fileURL, String savePath, int threadId, 
                           int startByte, int endByte, CountDownLatch latch, 
                           ProgressTracker progress) {
            this.fileURL = fileURL;
            this.savePath = savePath;
            this.threadId = threadId;
            this.startByte = startByte;
            this.endByte = endByte;
            this.latch = latch;
            this.progress = progress;
        }
        
        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
                connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
                connection.connect();
                
                try (RandomAccessFile file = new RandomAccessFile(savePath, "rw");
                     InputStream inputStream = connection.getInputStream()) {
                    
                    file.seek(startByte);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    int downloaded = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        file.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        progress.addProgress(threadId, bytesRead);
                    }
                    
                    System.out.println("Thread " + threadId + " completed: " + 
                                     (downloaded / 1024) + " KB downloaded");
                }
                
            } catch (IOException e) {
                System.err.println("Thread " + threadId + " error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }
    
    static class ProgressTracker {
        private final int totalSize;
        private final int[] threadProgress;
        private int totalDownloaded = 0;
        private int lastProgress = 0;
        
        public ProgressTracker(int totalSize, int threadCount) {
            this.totalSize = totalSize;
            this.threadProgress = new int[threadCount];
        }
        
        public synchronized void addProgress(int threadId, int bytes) {
            threadProgress[threadId] += bytes;
            totalDownloaded += bytes;
            
            if (totalSize > 0) {
                int progress = (totalDownloaded * 100) / totalSize;
                if (progress >= lastProgress + 5) {
                    System.out.println("Overall Progress: " + progress + "% " +
                                     "(" + (totalDownloaded / 1024) + " KB / " + 
                                     (totalSize / 1024) + " KB)");
                    lastProgress = progress;
                }
            }
        }
    }
    
    public static void downloadFile(String fileURL, String saveDir, int threadCount) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            int fileSize = connection.getContentLength();
            
            if (fileSize <= 0) {
                System.err.println("Cannot determine file size or file is empty");
                return;
            }
            
            String fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
            if (fileName.isEmpty()) {
                fileName = "downloaded_file";
            }
            
            String savePath = saveDir + File.separator + fileName;
            
            try (RandomAccessFile file = new RandomAccessFile(savePath, "rw")) {
                file.setLength(fileSize);
            }
            
            System.out.println("Starting multi-thread download: " + fileName);
            System.out.println("File size: " + (fileSize / 1024) + " KB");
            System.out.println("Threads: " + threadCount);
            
            int partSize = fileSize / threadCount;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ProgressTracker progress = new ProgressTracker(fileSize, threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                int startByte = i * partSize;
                int endByte = (i == threadCount - 1) ? fileSize - 1 : (i + 1) * partSize - 1;
                
                DownloadTask task = new DownloadTask(fileURL, savePath, i + 1, 
                                                     startByte, endByte, latch, progress);
                executor.submit(task);
            }
            
            latch.await();
            executor.shutdown();
            
            System.out.println("Download completed: " + savePath);
            
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Download error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Download interrupted: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java MultiThreadDownloader <file-url> <save-directory> [thread-count]");
            System.out.println("Example: java MultiThreadDownloader https://example.com/file.zip ./downloads 4");
            return;
        }
        
        String fileURL = args[0];
        String saveDir = args[1];
        int threadCount = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_THREAD_COUNT;
        
        new File(saveDir).mkdirs();
        
        long startTime = System.currentTimeMillis();
        downloadFile(fileURL, saveDir, threadCount);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Total time taken: " + (endTime - startTime) / 1000 + " seconds");
    }
}