package org.example;

import java.io.*;
import java.net.*;

public class SingleThread {
    
    public static void downloadFile(String fileURL, String saveDir) {
        try {
            URL url = new URL(fileURL);
            URLConnection connection = url.openConnection();
            
            String fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
            if (fileName.isEmpty()) {
                fileName = "downloaded_file";
            }
            
            String savePath = saveDir + File.separator + fileName;
            
            int fileSize = connection.getContentLength();
            System.out.println("Starting download: " + fileName);
            System.out.println("File size: " + (fileSize / 1024) + " KB");
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(savePath)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalBytesRead = 0;
                int lastProgress = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (fileSize > 0) {
                        int progress = (totalBytesRead * 100) / fileSize;
                        if (progress >= lastProgress + 10) {
                            System.out.println("Progress: " + progress + "%");
                            lastProgress = progress;
                        }
                    }
                }
            }
            
            System.out.println("Download completed: " + savePath);
            
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Download error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SingleThreadDownloader <file-url> <save-directory>");
            System.out.println("Example: java SingleThreadDownloader https://example.com/file.zip ./downloads");
            return;
        }
        
        String fileURL = args[0];
        String saveDir = args[1];
        
        new File(saveDir).mkdirs();
        
        long startTime = System.currentTimeMillis();
        downloadFile(fileURL, saveDir);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Time taken: " + (endTime - startTime) / 1000 + " seconds");
    }
}