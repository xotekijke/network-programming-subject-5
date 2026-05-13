package org.example;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Downloader extends JFrame {
    private DefaultListModel<String> listModel;
    private JTextField urlField;
    private JTextField dirField;
    private final Map<String, DownloadThread> activeDownloads;
    private final java.util.concurrent.ExecutorService executor;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private final Map<String, DownloadInfo> downloadInfoMap;
    
    public Downloader() {
        executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        activeDownloads = new HashMap<>();
        downloadInfoMap = new HashMap<>();
        
        setTitle("Karamavrov lab 5.2");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        createUI();
    }
    
    private void createUI() {
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        urlField = new JTextField();
        dirField = new JTextField(System.getProperty("user.home") + "/Downloads");
        inputPanel.add(new JLabel("URL:"));
        inputPanel.add(urlField);
        inputPanel.add(new JLabel("Save to:"));
        inputPanel.add(dirField);

        JButton addBtn = new JButton("Add to List");
        JButton startBtn = new JButton("Start All");
        JButton pauseBtn = new JButton("Pause All");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(startBtn);
        buttonPanel.add(pauseBtn);
        
        listModel = new DefaultListModel<>();
        JList<String> downloadList = new JList<>(listModel);
        downloadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Ready", JLabel.CENTER);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(downloadList), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        
        addBtn.addActionListener(e -> addDownload());
        startBtn.addActionListener(e -> startAll());
        pauseBtn.addActionListener(e -> pauseAll());
    }
    
    private void addDownload() {
        String url = urlField.getText().trim();
        String dir = dirField.getText().trim();
        
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter URL: ");
            return;
        }
        
        File f = new File(dir);
        if (!f.exists()) {
            f.mkdirs();
        }
        
        String fileName = extractFileName(url);
        String display = fileName + " - " + url;
        listModel.addElement(display);
        
        DownloadInfo info = new DownloadInfo();
        info.url = url;
        info.dir = dir;
        info.fileName = fileName;
        info.display = display;
        
        downloadInfoMap.put(display, info);
        
        urlField.setText("");
        statusLabel.setText("Added: " + fileName);
    }
    
    private String extractFileName(String url) {
        String name = url.substring(url.lastIndexOf("/") + 1);
        if (!name.contains(".")) {
            name = "file_" + System.currentTimeMillis();
        }
        if (name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }
        return name;
    }
    
    private void startAll() {
        for (int i = 0; i < listModel.size(); i++) {
            String display = listModel.get(i);
            if (!activeDownloads.containsKey(display)) {
                DownloadInfo info = downloadInfoMap.get(display);
                DownloadThread thread = new DownloadThread(info, i);
                activeDownloads.put(display, thread);
                executor.submit(thread);
            }
        }
        startTimer();
    }
    
    private void pauseAll() {
        for (DownloadThread thread : activeDownloads.values()) {
            thread.pause();
        }
        statusLabel.setText("Paused all downloads");
    }
    
    private void startTimer() {
        new Timer(500, e -> {
            int total = 0;
            int count = 0;
            for (DownloadThread thread : activeDownloads.values()) {
                if (thread.getProgress() > 0) {
                    total += thread.getProgress();
                    count++;
                }
            }
            if (count > 0) {
                progressBar.setValue(total / count);
            }
            
            boolean allDone = true;
            for (DownloadThread thread : activeDownloads.values()) {
                if (!thread.isFinished()) {
                    allDone = false;
                    break;
                }
            }
            if (allDone && !activeDownloads.isEmpty()) {
                statusLabel.setText("All downloads completed!");
                activeDownloads.clear();
                ((Timer)e.getSource()).stop();
            }
        }).start();
    }
    
    static class DownloadInfo {
        String url;
        String dir;
        String fileName;
        String display;
    }
    
    class DownloadThread implements Runnable {
        private final DownloadInfo info;
        private final int listIndex;
        private volatile boolean paused = false;
        private final boolean running = true;
        private int progress = 0;
        private boolean finished = false;
        
        DownloadThread(DownloadInfo info, int index) {
            this.info = info;
            this.listIndex = index;
        }
        
        void pause() {
            paused = true;
        }
        
        int getProgress() {
            return progress;
        }
        
        boolean isFinished() {
            return finished;
        }
        
        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile file = null;
            InputStream input = null;
            
            try {
                String path = info.dir + File.separator + info.fileName;
                File output = new File(path);
                
                URL url = new URL(info.url);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.connect();
                long totalSize = conn.getContentLength();
                conn.disconnect();
                
                if (output.exists() && output.length() == totalSize && totalSize > 0) {
                    SwingUtilities.invokeLater(() -> {
                        listModel.set(listIndex, "✓ " + info.fileName + " (already exists)");
                        statusLabel.setText("Skipped: " + info.fileName);
                    });
                    finished = true;
                    return;
                }
                
                long existing = output.exists() ? output.length() : 0;
                
                conn = (HttpURLConnection) url.openConnection();
                if (existing > 0) {
                    conn.setRequestProperty("Range", "bytes=" + existing + "-");
                }
                conn.connect();
                
                input = conn.getInputStream();
                file = new RandomAccessFile(output, "rw");
                if (existing > 0) {
                    file.seek(existing);
                }
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long downloaded = existing;
                
                while (running && (bytesRead = input.read(buffer)) != -1) {
                    while (paused) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    file.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    if (totalSize > 0) {
                        progress = (int) (downloaded * 100 / totalSize);
                        SwingUtilities.invokeLater(() -> {
                            listModel.set(listIndex, info.fileName + " - " + progress + "%");
                        });
                    }
                }
                
                if (!running) {
                    SwingUtilities.invokeLater(() -> 
                        listModel.set(listIndex, "⏸ " + info.fileName + " (paused)"));
                } else {
                    SwingUtilities.invokeLater(() -> {
                        listModel.set(listIndex, "✓ " + info.fileName + " (completed)");
                    });
                }
                
                finished = true;
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    listModel.set(listIndex, "✗ " + info.fileName + " - " + e.getMessage()));
                finished = true;
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                    if (file != null) {
                        file.close();
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                activeDownloads.remove(info.display);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Downloader().setVisible(true));
    }
}