/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.CSVHandler to edit this template
 */
package Dialogue;

import javax.swing.*;
import javax.persistence.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author LEGION
 */
public class CSVHandler {

    private EntityManagerFactory emf;

    public CSVHandler(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // Method untuk download data ke CSV (TANPA HEADER)
    public void downloadToCSV() {
        EntityManager em = emf.createEntityManager();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Data sebagai CSV");
        fileChooser.setSelectedFile(new File("data_perangkat.csv"));
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        try {
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                
                // Ambil semua data detail perangkat
                String query = "SELECT d FROM DetailPerangkat d";
                List<DetailPerangkat> dataList = em.createQuery(query, DetailPerangkat.class).getResultList();
                
                // Tulis ke file CSV TANPA HEADER
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    // Langsung tulis data tanpa header
                    for (DetailPerangkat data : dataList) {
                        writer.printf("%d,%s,%s,%d,%d%n",
                            data.getIdDetail(),
                            data.getPerangkatElektronik().getNomorSeri(),
                            data.getWarna(),
                            data.getHarga(),
                            data.getStok()
                        );
                    }
                }
                
                JOptionPane.showMessageDialog(null, 
                    "Data berhasil diunduh!\n" +
                    "Jumlah data: " + dataList.size() + "\n" +
                    "Lokasi: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error download CSV: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    // Method untuk upload data dari CSV (TANPA HEADER)
    public void uploadFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File CSV untuk Upload");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File csvFile = fileChooser.getSelectedFile();
            processCSVUpload(csvFile);
        }
    }

    private void processCSVUpload(File csvFile) {
        EntityManager em = emf.createEntityManager();
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] data = line.split(",");
                if (data.length == 5) {
                    if (processCSVData(em, data, lineNumber)) {
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.append("Baris ").append(lineNumber).append(": Gagal - ").append(line).append("\n");
                    }
                } else {
                    errorCount++;
                    errorMessages.append("Baris ").append(lineNumber).append(": Format tidak valid - ").append(line).append("\n");
                }
            }
            
            // Tampilkan hasil
            showUploadResult(successCount, errorCount, errorMessages.toString());
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error membaca file CSV: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private boolean processCSVData(EntityManager em, String[] data, int lineNumber) {
        try {
            // Parse data dari CSV - langsung dari data tanpa header
            String idDetailStr = data[0].trim();
            String nomorSeri = data[1].trim();
            String warna = data[2].trim();
            long harga;
            int stok;
            
            // Validasi dan parse numeric values
            try {
                harga = Long.parseLong(data[3].trim());
                stok = Integer.parseInt(data[4].trim());
            } catch (NumberFormatException e) {
                System.out.println("Error parsing numeric value pada baris " + lineNumber + ": " + e.getMessage());
                return false;
            }
            
            // Cek apakah perangkat elektronik exists
            PerangkatElektronik perangkat = em.find(PerangkatElektronik.class, nomorSeri);
            if (perangkat == null) {
                System.out.println("Baris " + lineNumber + ": Perangkat dengan nomor seri '" + nomorSeri + "' tidak ditemukan");
                return false;
            }
            
            // Parse ID Detail
            Integer idDetail;
            try {
                idDetail = Integer.parseInt(idDetailStr);
            } catch (NumberFormatException e) {
                System.out.println("Baris " + lineNumber + ": ID Detail tidak valid: " + idDetailStr);
                return false;
            }
            
            // Cek apakah detail sudah ada
            DetailPerangkat existingDetail = em.find(DetailPerangkat.class, idDetail);
            
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }
            
            if (existingDetail != null) {
                // Update existing data
                existingDetail.setWarna(warna);
                existingDetail.setHarga(harga);
                existingDetail.setStok(stok);
                existingDetail.setPerangkatElektronik(perangkat);
                em.merge(existingDetail);
                System.out.println("Baris " + lineNumber + ": Data updated - ID: " + idDetail);
            } else {
                // Insert new data
                DetailPerangkat newDetail = new DetailPerangkat();
                newDetail.setIdDetail(idDetail);
                newDetail.setWarna(warna);
                newDetail.setHarga(harga);
                newDetail.setStok(stok);
                newDetail.setPerangkatElektronik(perangkat);
                em.persist(newDetail);
                System.out.println("Baris " + lineNumber + ": Data inserted - ID: " + idDetail);
            }
            
            em.getTransaction().commit();
            return true;
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Baris " + lineNumber + ": Error processing data - " + e.getMessage());
            return false;
        }
    }

    private void showUploadResult(int successCount, int errorCount, String errorMessages) {
        StringBuilder result = new StringBuilder();
        result.append("Hasil Upload CSV:\n\n");
        result.append("Data berhasil: ").append(successCount).append("\n");
        result.append("Data gagal: ").append(errorCount).append("\n");
        
        if (errorCount > 0 && !errorMessages.isEmpty()) {
            result.append("\nDetail Error:\n").append(errorMessages);
        }
        
        JOptionPane.showMessageDialog(null, result.toString(), "Hasil Upload", 
            errorCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    // Method untuk generate template CSV kosong
    public void generateTemplateCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Template CSV");
        fileChooser.setSelectedFile(new File("template_data_perangkat.csv"));
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        try {
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                
                // Buat template dengan contoh data
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    // Contoh data format
                    writer.println("1,T001,Graphite,22999900,7");
                    writer.println("2,T002,Blue-Gray,8200000,5");
                    writer.println("3,L001,Silver,8999000,3");
                }
                
                JOptionPane.showMessageDialog(null, 
                    "Template CSV berhasil dibuat!\n" +
                    "Format: id_detail,nomor_seri,warna,harga,stok\n" +
                    "Lokasi: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error membuat template: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class io {

        public io() {
        }

        static class BufferedReader {

            
        }

        static class File {

            public File() {
            }
        }
    }
}