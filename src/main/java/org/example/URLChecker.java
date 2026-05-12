package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.Scanner;

public class URLChecker {
    
    public static boolean isURLReachable(String urlString) {
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("HEAD");
            
            connection.setConnectTimeout(5000);
            
            connection.setReadTimeout(5000);
            
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            
            return responseCode == HttpURLConnection.HTTP_OK;
            
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL format: " + e.getMessage());
            return false;
        } catch (SocketTimeoutException e) {
            System.err.println("Connection timeout: " + e.getMessage());
            return false;
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static boolean isURLReachableWithGet(String urlString) {
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            connection.setRequestProperty("Range", "bytes=0-0");
            
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            
            return responseCode == HttpURLConnection.HTTP_OK || 
                   responseCode == HttpURLConnection.HTTP_PARTIAL ||
                   responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                   responseCode == HttpURLConnection.HTTP_MOVED_TEMP;
                   
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter a URL to check reachability: ");
        
        String urlString = scanner.nextLine().trim();
        
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "https://" + urlString;
        }
        
        System.out.println("\nChecking URL: " + urlString);
        
        long startTime = System.currentTimeMillis();
        boolean isReachable = isURLReachable(urlString);
        long endTime = System.currentTimeMillis();
        
        if (isReachable) {
            System.out.println("URL is reachable");
            System.out.println("Response time: " + (endTime - startTime) + " ms");
        } else {
            System.out.println("URL is not reachable");
            
            System.out.println("\nTrying alternative method (GET request)...");
            boolean alternativeReachable = isURLReachableWithGet(urlString);
            
            if (alternativeReachable) {
                System.out.println("URL is reachable (via GET request)");
            } else {
                System.out.println("URL is not reachable (confirmed)");
            }
        }
        
        scanner.close();
    }
}