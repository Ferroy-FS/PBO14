/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dialogue;

import javax.swing.*;
import javax.persistence.*;
import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.HashMap;   

/**
 *
 * @author LEGION
 */
public class MasterDataCSVHandler {
    
    private EntityManagerFactory emf;
    
    public MasterDataCSVHandler(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    // Method untuk upload data master dari CSV
     public void uploadMasterDataFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File CSV Data Master");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File csvFile = fileChooser.getSelectedFile();
            processMasterCSVUpload(csvFile);
        }
    }
    
    private void processMasterCSVUpload(File csvFile) {
        EntityManager em = emf.createEntityManager();
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;
            String[] headers = null;
            
            while ((line = br.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] data = line.split(",");
                
                if (isFirstLine) {
                    // Process header line
                    headers = data;
                    isFirstLine = false;
                    continue;
                }
                
                if (processMasterData(em, data, headers, successCount + errorCount + 1)) {
                    successCount++;
                } else {
                    errorCount++;
                    errorMessages.append("Baris ").append(successCount + errorCount)
                                .append(": Gagal - ").append(line).append("\n");
                }
            }
            
            // Tampilkan hasil
            showUploadResult(successCount, errorCount, errorMessages.toString(), "Data Master");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error membaca file CSV: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
    
    private boolean processMasterData(EntityManager em, String[] data, String[] headers, int lineNumber) {
        try {
            // Map data berdasarkan header
            Map<String, String> dataMap = mapDataToHeaders(data, headers);
            
            // Extract data
            String nomorSeri = getValue(dataMap, "nomor seri", "nomor_seri");
            String jenisPerangkat = getValue(dataMap, "jenis perangkat", "jenis_perangkat");
            String merekPerangkat = getValue(dataMap, "merek perangkat", "merek_perangkat");
            String namaPerangkat = getValue(dataMap, "nama perangkat", "nama_perangkat");
            String modelPerangkat = getValue(dataMap, "model perangkat", "model_perangkat");
            
            // Validasi data required
            if (nomorSeri == null || nomorSeri.isEmpty()) {
                System.out.println("Baris " + lineNumber + ": Nomor seri tidak boleh kosong");
                return false;
            }
            
            if (jenisPerangkat == null || jenisPerangkat.isEmpty()) {
                System.out.println("Baris " + lineNumber + ": Jenis perangkat tidak boleh kosong");
                return false;
            }
            
            // Cek apakah data sudah ada
            PerangkatElektronik existingPerangkat = em.find(PerangkatElektronik.class, nomorSeri);
            
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }
            
            if (existingPerangkat != null) {
                // Update existing data
                existingPerangkat.setJenisPerangkat(jenisPerangkat);
                existingPerangkat.setMerekPerangkat(merekPerangkat != null ? merekPerangkat : "");
                existingPerangkat.setNamaPerangkat(namaPerangkat != null ? namaPerangkat : "");
                existingPerangkat.setModelPerangkat(modelPerangkat != null ? modelPerangkat : "");
                em.merge(existingPerangkat);
                System.out.println("Baris " + lineNumber + ": Data master updated - " + nomorSeri);
            } else {
                // Insert new data
                PerangkatElektronik newPerangkat = new PerangkatElektronik();
                newPerangkat.setNomorSeri(nomorSeri);
                newPerangkat.setJenisPerangkat(jenisPerangkat);
                newPerangkat.setMerekPerangkat(merekPerangkat != null ? merekPerangkat : "");
                newPerangkat.setNamaPerangkat(namaPerangkat != null ? namaPerangkat : "");
                newPerangkat.setModelPerangkat(modelPerangkat != null ? modelPerangkat : "");
                em.persist(newPerangkat);
                System.out.println("Baris " + lineNumber + ": Data master inserted - " + nomorSeri);
            }
            
            em.getTransaction().commit();
            return true;
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Baris " + lineNumber + ": Error processing master data - " + e.getMessage());
            return false;
        }
    }
    
    // Helper method untuk map data ke header
    private Map<String, String> mapDataToHeaders(String[] data, String[] headers) {
        Map<String, String> dataMap = new HashMap<>();
        for (int i = 0; i < Math.min(headers.length, data.length); i++) {
            String header = headers[i].trim().toLowerCase();
            String value = data[i].trim();
            dataMap.put(header, value);
        }
        return dataMap;
    }
    
    // Helper method untuk get value dengan multiple possible keys
    private String getValue(Map<String, String> dataMap, String... keys) {
        for (String key : keys) {
            if (dataMap.containsKey(key)) {
                return dataMap.get(key);
            }
        }
        return null;
    }
    
    private void showUploadResult(int successCount, int errorCount, String errorMessages, String dataType) {
        StringBuilder result = new StringBuilder();
        result.append("Hasil Upload ").append(dataType).append(":\n\n");
        result.append("Data berhasil: ").append(successCount).append("\n");
        result.append("Data gagal: ").append(errorCount).append("\n");
        
        if (errorCount > 0 && !errorMessages.isEmpty()) {
            result.append("\nDetail Error:\n").append(errorMessages);
        }
        
        JOptionPane.showMessageDialog(null, result.toString(), "Hasil Upload " + dataType, 
            errorCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Method untuk generate template master data CSV
    public void generateMasterTemplateCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Template Data Master CSV");
        fileChooser.setSelectedFile(new File("template_data_master.csv"));
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        try {
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                
                // Buat template dengan header yang flexible
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    // Header dengan berbagai kemungkinan nama kolom
                    writer.println("nomor_seri,jenis_perangkat,merek_perangkat,nama_perangkat,model_perangkat");
                    // Contoh data
                    writer.println("L001,Laptop,Lenovo,IdeaPad Slim 5i,82LN004AID");
                    writer.println("T001,Tablet,Samsung,Galaxy Tab S9 Ultra,SM-X916");
                }
                
                JOptionPane.showMessageDialog(null, 
                    "Template Data Master CSV berhasil dibuat!\n" +
                    "Format yang didukung:\n" +
                    "- nomor_seri / Nomor Seri\n" +
                    "- jenis_perangkat / Jenis Perangkat\n" +
                    "- merek_perangkat / Merek Perangkat\n" +
                    "- nama_perangkat / Nama Perangkat\n" +
                    "- model_perangkat / Model Perangkat\n" +
                    "Lokasi: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error membuat template: " + e.getMessage());
            e.printStackTrace();
        }
    }
}