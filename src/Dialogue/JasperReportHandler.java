/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dialogue;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *
 * @author LEGION
 */
public class JasperReportHandler {

    private java.sql.Connection getJDBCConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return java.sql.DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/PBO_Praktikum_5",
                    "postgres",
                    "0000"
            );
        } catch (Exception e) {
            throw new RuntimeException("Error getting JDBC connection: " + e.getMessage(), e);
        }
    }

    private EntityManagerFactory emf;
    private boolean lastActionWasDownload = false;
    private File lastDownloadedFile = null;

    public JasperReportHandler(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void handleDataAction() {
        String[] options = {"Download Data Gabungan", "Upload Data Gabungan", "Batal"};
        int choice = JOptionPane.showOptionDialog(null,
                "Pilih aksi yang ingin dilakukan:",
                "Download atau Upload Data Gabungan",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0:
                downloadCombinedDataToCSV();
                break;
            case 1:
                uploadCombinedDataFromCSV();
                break;
            default:
                break;
        }
    }

    public void downloadCombinedDataToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Data Gabungan sebagai CSV");
        fileChooser.setSelectedFile(new File("data_gabungan_perangkat_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

        try {
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }

                generateCombinedCSVReport(file);

                JOptionPane.showMessageDialog(null,
                        "Data gabungan berhasil diunduh!\n\n"
                        + "File: " + file.getName() + "\n"
                        + "Lokasi: " + file.getAbsolutePath() + "\n\n"
                        + "Data berisi informasi dari kedua tab (Master dan Detail).",
                        "Download Berhasil",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error download data gabungan: " + e.getMessage(),
                    "Error Download",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void uploadCombinedDataFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File CSV untuk Upload ke Database");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File csvFile = fileChooser.getSelectedFile();
            processCombinedCSVUpload(csvFile);
        }
    }

    private void generateCombinedCSVReport(File outputFile) {
        EntityManager em = emf.createEntityManager();
        try {
            String jrxmlPath = "src/reports/laporan_gabungan_perangkat_csv.jrxml";
            String jasperPath = "laporan_gabungan_perangkat_csv.jasper";

            File jasperFile = new File(jasperPath);
            if (!jasperFile.exists()) {
                System.out.println("Compiling CSV report...");
                JasperCompileManager.compileReportToFile(jrxmlPath, jasperPath);
                System.out.println("CSV report compiled successfully!");
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", "DATA GABUNGAN PERANGKAT ELEKTRONIK");

            java.sql.Connection jdbcConn = getJDBCConnection();

            System.out.println("Filling CSV report...");
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperPath, parameters, jdbcConn);

            // Ekspor ke CSV dengan format yang sesuai
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                // Tulis header
                writer.println("nomor_seri,jenis_perangkat,merek_perangkat,nama_perangkat,model_perangkat,id_detail,warna,harga,stok");

                // Ambil data langsung dari database untuk kontrol penuh
                String dataQuery = "SELECT pe.nomor_seri, pe.jenis_perangkat, pe.merek_perangkat, "
                        + "pe.nama_perangkat, pe.model_perangkat, dp.id_detail, dp.warna, dp.harga, dp.stok "
                        + "FROM perangkat_elektronik pe "
                        + "LEFT JOIN detail_perangkat dp ON pe.nomor_seri = dp.nomor_seri "
                        + "ORDER BY pe.nomor_seri, dp.id_detail";

                PreparedStatement stmt = jdbcConn.prepareStatement(dataQuery);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String nomorSeri = rs.getString("nomor_seri");
                    String jenis = rs.getString("jenis_perangkat");
                    String merek = rs.getString("merek_perangkat");
                    String nama = rs.getString("nama_perangkat");
                    String model = rs.getString("model_perangkat");
                    int idDetail = rs.getInt("id_detail");
                    String warna = rs.getString("warna");
                    long harga = rs.getLong("harga");
                    int stok = rs.getInt("stok");

                    // Format data dengan handling null values
                    writer.printf("%s,%s,%s,%s,%s,%d,%s,%d,%d%n",
                            nomorSeri != null ? nomorSeri : "",
                            jenis != null ? jenis : "",
                            merek != null ? merek : "",
                            nama != null ? nama : "",
                            model != null ? model : "",
                            idDetail,
                            warna != null ? warna : "",
                            harga,
                            stok
                    );
                }

                rs.close();
                stmt.close();
            }

            jdbcConn.close();
            System.out.println("CSV export completed successfully!");

        } catch (Exception e) {
            throw new RuntimeException("Error generating combined CSV report: " + e.getMessage(), e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void processCombinedCSVUpload(File csvFile) {
        EntityManager em = emf.createEntityManager();
        int masterSuccessCount = 0;
        int masterErrorCount = 0;
        int detailSuccessCount = 0;
        int detailErrorCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;
            Map<String, Integer> columnIndexes = new HashMap<>();
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",");

                if (isFirstLine) {
                    columnIndexes = detectColumnIndexes(data);
                    isFirstLine = false;
                    continue;
                }

                boolean masterSuccess = processMasterDataFromRow(em, data, columnIndexes, lineNumber);
                boolean detailSuccess = processDetailDataFromRow(em, data, columnIndexes, lineNumber);

                if (masterSuccess) {
                    masterSuccessCount++;
                } else {
                    masterErrorCount++;
                }

                if (detailSuccess) {
                    detailSuccessCount++;
                } else {
                    detailErrorCount++;
                }
            }

            showCombinedUploadResult(masterSuccessCount, masterErrorCount,
                    detailSuccessCount, detailErrorCount,
                    csvFile.getName());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error membaca file CSV: " + e.getMessage(),
                    "Error Upload",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public void uploadMultipleFormatCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File CSV (Format: Data_Laptop, Laptop, atau data_perangkat_2)");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File csvFile = fileChooser.getSelectedFile();
            detectAndProcessCSVFormat(csvFile);
        }
    }

    private void detectAndProcessCSVFormat(File csvFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String firstLine = br.readLine();
            if (firstLine == null) {
                JOptionPane.showMessageDialog(null, "File CSV kosong!");
                return;
            }

            String[] firstData = firstLine.split(",");
            int columnCount = firstData.length;

            System.out.println("Deteksi format CSV:");
            System.out.println("Jumlah kolom: " + columnCount);
            System.out.println("Baris pertama: " + firstLine);

            // Deteksi format berdasarkan jumlah kolom dan konten
            CSVFormat format = detectCSVFormat(firstLine, columnCount);
            System.out.println("Format terdeteksi: " + format);

            switch (format) {
                case DATA_LAPTOP:
                    processDataLaptopFormat(csvFile);
                    break;
                case LAPTOP:
                    processLaptopFormat(csvFile);
                    break;
                case DATA_PERANGKAT_2:
                    processDataPerangkat2Format(csvFile);
                    break;
                case COMBINED:
                    processCombinedCSVUpload(csvFile);
                    break;
                default:
                    JOptionPane.showMessageDialog(null,
                            "Format CSV tidak dikenali!\n\n"
                            + "Format yang didukung:\n"
                            + "1. Data_Laptop.csv (5 kolom: nomor_seri,jenis,merek,nama,model)\n"
                            + "2. Laptop.csv (5 kolom dengan header: id_detail,nomor_seri,warna,harga,stok)\n"
                            + "3. data_perangkat_2.csv (9 kolom dalam quotes)\n"
                            + "4. Combined format (9 kolom dengan header lengkap)");
                    break;
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error membaca file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private enum CSVFormat {
        DATA_LAPTOP, LAPTOP, DATA_PERANGKAT_2, COMBINED, UNKNOWN
    }

    private CSVFormat detectCSVFormat(String firstLine, int columnCount) {
        firstLine = firstLine.trim().toLowerCase();

        // Format 1: Master Data (5 kolom, tanpa header id_detail)
        if (columnCount == 5 && !firstLine.contains("id_detail")) {
            return CSVFormat.DATA_LAPTOP;
        }

        // Format 2: Detail Data (5 kolom, dengan header id_detail)  
        if (columnCount == 5 && firstLine.contains("id_detail")) {
            return CSVFormat.LAPTOP;
        }

        // Format 3: Data dalam quotes
        if (firstLine.startsWith("\"") && firstLine.endsWith("\"")) {
            return CSVFormat.DATA_PERANGKAT_2;
        }

        // Format 4: Combined data (banyak kolom)
        if (columnCount >= 8) {
            return CSVFormat.COMBINED;
        }

        return CSVFormat.UNKNOWN;
    }

    private void processDataLaptopFormat(File csvFile) {
        EntityManager em = emf.createEntityManager();
        int successCount = 0;
        int errorCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",");
                if (data.length == 5) {
                    String nomorSeri = data[0].trim();
                    String jenis = data[1].trim();
                    String merek = data[2].trim();
                    String nama = data[3].trim();
                    String model = data[4].trim();

                    if (processMasterData(em, nomorSeri, jenis, merek, nama, model, lineNumber)) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                } else {
                    System.out.println("Baris " + lineNumber + ": Format tidak valid - " + line);
                    errorCount++;
                }
            }

            showUploadResult(successCount, errorCount, "Format Data_Laptop");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error processing Data_Laptop format: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void processLaptopFormat(File csvFile) {
        EntityManager em = emf.createEntityManager();
        int successCount = 0;
        int errorCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                String[] data = line.split(",");
                if (data.length == 5) {
                    String idDetailStr = data[0].trim();
                    String nomorSeri = data[1].trim();
                    String warna = data[2].trim();
                    String hargaStr = data[3].trim();
                    String stokStr = data[4].trim();

                    try {
                        Integer idDetail = Integer.parseInt(idDetailStr.replaceAll("[^\\d]", ""));
                        long harga = Long.parseLong(hargaStr.replaceAll("[^\\d]", ""));
                        int stok = Integer.parseInt(stokStr.replaceAll("[^\\d]", ""));

                        if (processDetailData(em, idDetail, nomorSeri, warna, harga, stok, lineNumber)) {
                            successCount++;
                        } else {
                            errorCount++;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Baris " + lineNumber + ": Format angka tidak valid - " + line);
                        errorCount++;
                    }
                } else {
                    System.out.println("Baris " + lineNumber + ": Format tidak valid - " + line);
                    errorCount++;
                }
            }

            showUploadResult(successCount, errorCount, "Format Laptop");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error processing Laptop format: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void processDataPerangkat2Format(File csvFile) {
        EntityManager em = emf.createEntityManager();
        int masterSuccessCount = 0;
        int masterErrorCount = 0;
        int detailSuccessCount = 0;
        int detailErrorCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Remove quotes and split
                String cleanLine = line.trim();
                if (cleanLine.startsWith("\"") && cleanLine.endsWith("\"")) {
                    cleanLine = cleanLine.substring(1, cleanLine.length() - 1);
                }

                String[] data = cleanLine.split(",");
                if (data.length == 9) {
                    // Process master data (first 5 columns)
                    String nomorSeri = data[0].trim();
                    String jenis = data[1].trim();
                    String merek = data[2].trim();
                    String nama = data[3].trim();
                    String model = data[4].trim();

                    boolean masterSuccess = processMasterData(em, nomorSeri, jenis, merek, nama, model, lineNumber);
                    if (masterSuccess) {
                        masterSuccessCount++;
                    } else {
                        masterErrorCount++;
                    }

                    // Process detail data (last 4 columns)
                    String idDetailStr = data[5].trim();
                    String warna = data[6].trim();
                    String hargaStr = data[7].trim();
                    String stokStr = data[8].trim();

                    try {
                        Integer idDetail = Integer.parseInt(idDetailStr.replaceAll("[^\\d]", ""));
                        long harga = Long.parseLong(hargaStr.replaceAll("[^\\d]", ""));
                        int stok = Integer.parseInt(stokStr.replaceAll("[^\\d]", ""));

                        boolean detailSuccess = processDetailData(em, idDetail, nomorSeri, warna, harga, stok, lineNumber);
                        if (detailSuccess) {
                            detailSuccessCount++;
                        } else {
                            detailErrorCount++;
                        }

                    } catch (NumberFormatException e) {
                        System.out.println("Baris " + lineNumber + ": Format angka tidak valid - " + line);
                        detailErrorCount++;
                    }
                } else {
                    System.out.println("Baris " + lineNumber + ": Format tidak valid - " + line);
                    masterErrorCount++;
                    detailErrorCount++;
                }
            }

            showCombinedUploadResult(masterSuccessCount, masterErrorCount, detailSuccessCount, detailErrorCount, csvFile.getName());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error processing data_perangkat_2 format: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private boolean processMasterData(EntityManager em, String nomorSeri, String jenis, String merek, String nama, String model, int lineNumber) {
        try {
            PerangkatElektronik existing = em.find(PerangkatElektronik.class, nomorSeri);

            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }

            if (existing != null) {
                // Update existing
                existing.setJenisPerangkat(jenis);
                existing.setMerekPerangkat(merek);
                existing.setNamaPerangkat(nama);
                existing.setModelPerangkat(model);
                em.merge(existing);
                System.out.println("Baris " + lineNumber + ": Master data updated - " + nomorSeri);
            } else {
                // Insert new
                PerangkatElektronik newData = new PerangkatElektronik();
                newData.setNomorSeri(nomorSeri);
                newData.setJenisPerangkat(jenis);
                newData.setMerekPerangkat(merek);
                newData.setNamaPerangkat(nama);
                newData.setModelPerangkat(model);
                em.persist(newData);
                System.out.println("Baris " + lineNumber + ": Master data inserted - " + nomorSeri);
            }

            em.getTransaction().commit();
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Baris " + lineNumber + ": Error master data - " + e.getMessage());
            return false;
        }
    }

    private boolean processDetailData(EntityManager em, Integer idDetail, String nomorSeri, String warna, long harga, int stok, int lineNumber) {
        try {
            // Cek apakah perangkat master ada
            PerangkatElektronik perangkat = em.find(PerangkatElektronik.class, nomorSeri);
            if (perangkat == null) {
                System.out.println("Baris " + lineNumber + ": Perangkat tidak ditemukan - " + nomorSeri);
                return false;
            }

            DetailPerangkat existingDetail = em.find(DetailPerangkat.class, idDetail);

            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }

            if (existingDetail != null) {
                // Update existing
                existingDetail.setWarna(warna);
                existingDetail.setHarga(harga);
                existingDetail.setStok(stok);
                existingDetail.setPerangkatElektronik(perangkat);
                em.merge(existingDetail);
                System.out.println("Baris " + lineNumber + ": Detail data updated - ID: " + idDetail);
            } else {
                // Insert new
                DetailPerangkat newDetail = new DetailPerangkat();
                newDetail.setIdDetail(idDetail);
                newDetail.setWarna(warna);
                newDetail.setHarga(harga);
                newDetail.setStok(stok);
                newDetail.setPerangkatElektronik(perangkat);
                em.persist(newDetail);
                System.out.println("Baris " + lineNumber + ": Detail data inserted - ID: " + idDetail);
            }

            em.getTransaction().commit();
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Baris " + lineNumber + ": Error detail data - " + e.getMessage());
            return false;
        }
    }

    

    private void showUploadResult(int successCount, int errorCount, String formatType) {
        StringBuilder result = new StringBuilder();
        result.append("Hasil Upload (").append(formatType).append("):\n\n");
        result.append("Data berhasil: ").append(successCount).append("\n");
        result.append("Data gagal: ").append(errorCount).append("\n");

        JOptionPane.showMessageDialog(null, result.toString(), "Hasil Upload",
                errorCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    private Map<String, Integer> detectColumnIndexes(String[] headers) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            if (header.contains("nomor_seri") || header.contains("noseri")) {
                indexes.put("nomor_seri", i);
            } else if (header.contains("jenis") || header.contains("type")) {
                indexes.put("jenis_perangkat", i);
            } else if (header.contains("merek") || header.contains("brand")) {
                indexes.put("merek_perangkat", i);
            } else if (header.contains("nama") || header.contains("name")) {
                indexes.put("nama_perangkat", i);
            } else if (header.contains("model")) {
                indexes.put("model_perangkat", i);
            } else if (header.contains("id_detail") || header.contains("iddetail")) {
                indexes.put("id_detail", i);
            } else if (header.contains("warna") || header.contains("color")) {
                indexes.put("warna", i);
            } else if (header.contains("harga") || header.contains("price")) {
                indexes.put("harga", i);
            } else if (header.contains("stok") || header.contains("stock")) {
                indexes.put("stok", i);
            }
        }
        return indexes;
    }

    /*private boolean hasMasterDataColumns(Map<String, Integer> columnIndexes) {
        return columnIndexes.containsKey("nomor_seri")
                && columnIndexes.containsKey("jenis_perangkat");
    }

    private boolean hasDetailDataColumns(Map<String, Integer> columnIndexes) {
        return columnIndexes.containsKey("nomor_seri")
                && columnIndexes.containsKey("warna")
                && columnIndexes.containsKey("harga");
    }

    private boolean processCombinedDataRow(EntityManager em, String[] data, Map<String, Integer> columnIndexes) {
        try {
            boolean masterSuccess = true;
            boolean detailSuccess = true;

            if (hasMasterDataColumns(columnIndexes)) {
                masterSuccess = processMasterDataFromRow(em, data, columnIndexes);
            }

            if (hasDetailDataColumns(columnIndexes)) {
                detailSuccess = processDetailDataFromRow(em, data, columnIndexes);
            }

            return masterSuccess && detailSuccess;

        } catch (Exception e) {
            System.out.println("Error processing combined data row: " + e.getMessage());
            return false;
        }
    }*/
    private boolean processMasterDataFromRow(EntityManager em, String[] data, Map<String, Integer> columnIndexes, int lineNumber) {
        try {
            String nomorSeri = getValue(data, columnIndexes, "nomor_seri");
            String jenisPerangkat = getValue(data, columnIndexes, "jenis_perangkat");

            if (nomorSeri == null || nomorSeri.isEmpty() || jenisPerangkat == null || jenisPerangkat.isEmpty()) {
                System.out.println("Baris " + lineNumber + ": Data master tidak lengkap, dilewati");
                return false;
            }

            PerangkatElektronik existingPerangkat = em.find(PerangkatElektronik.class, nomorSeri);

            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }

            if (existingPerangkat != null) {
                existingPerangkat.setJenisPerangkat(jenisPerangkat);
                existingPerangkat.setMerekPerangkat(getValue(data, columnIndexes, "merek_perangkat"));
                existingPerangkat.setNamaPerangkat(getValue(data, columnIndexes, "nama_perangkat"));
                existingPerangkat.setModelPerangkat(getValue(data, columnIndexes, "model_perangkat"));
                em.merge(existingPerangkat);
                System.out.println("Baris " + lineNumber + ": Data master updated - " + nomorSeri);
            } else {
                PerangkatElektronik newPerangkat = new PerangkatElektronik();
                newPerangkat.setNomorSeri(nomorSeri);
                newPerangkat.setJenisPerangkat(jenisPerangkat);
                newPerangkat.setMerekPerangkat(getValue(data, columnIndexes, "merek_perangkat"));
                newPerangkat.setNamaPerangkat(getValue(data, columnIndexes, "nama_perangkat"));
                newPerangkat.setModelPerangkat(getValue(data, columnIndexes, "model_perangkat"));
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

    private boolean processDetailDataFromRow(EntityManager em, String[] data, Map<String, Integer> columnIndexes, int lineNumber) {
        try {
            String nomorSeri = getValue(data, columnIndexes, "nomor_seri");
            String warna = getValue(data, columnIndexes, "warna");
            String hargaStr = getValue(data, columnIndexes, "harga");

            if (nomorSeri == null || nomorSeri.isEmpty() || warna == null || warna.isEmpty() || hargaStr == null) {
                System.out.println("Baris " + lineNumber + ": Data detail tidak lengkap, dilewati");
                return false;
            }

            long harga;
            int stok = 0;
            try {
                harga = Long.parseLong(hargaStr.replaceAll("[^\\d]", ""));
                String stokStr = getValue(data, columnIndexes, "stok");
                if (stokStr != null && !stokStr.isEmpty()) {
                    stok = Integer.parseInt(stokStr.replaceAll("[^\\d]", ""));
                }
            } catch (NumberFormatException e) {
                System.out.println("Baris " + lineNumber + ": Format angka tidak valid");
                return false;
            }

            PerangkatElektronik perangkat = em.find(PerangkatElektronik.class, nomorSeri);
            if (perangkat == null) {
                System.out.println("Baris " + lineNumber + ": Perangkat dengan nomor seri '" + nomorSeri + "' tidak ditemukan");
                return false;
            }

            String idDetailStr = getValue(data, columnIndexes, "id_detail");
            boolean isUpdate = false;
            DetailPerangkat existingDetail = null;

            if (idDetailStr != null && !idDetailStr.isEmpty()) {
                try {
                    Integer idDetail = Integer.parseInt(idDetailStr);
                    existingDetail = em.find(DetailPerangkat.class, idDetail);
                    isUpdate = (existingDetail != null);
                } catch (NumberFormatException e) {
                }
            }

            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }

            if (isUpdate) {
                existingDetail.setWarna(warna);
                existingDetail.setHarga(harga);
                existingDetail.setStok(stok);
                existingDetail.setPerangkatElektronik(perangkat);
                em.merge(existingDetail);
                System.out.println("Baris " + lineNumber + ": Data detail updated - ID: " + existingDetail.getIdDetail());
            } else {
                DetailPerangkat newDetail = new DetailPerangkat();
                newDetail.setWarna(warna);
                newDetail.setHarga(harga);
                newDetail.setStok(stok);
                newDetail.setPerangkatElektronik(perangkat);
                em.persist(newDetail);
                System.out.println("Baris " + lineNumber + ": Data detail inserted - " + nomorSeri + " - " + warna);
            }

            em.getTransaction().commit();
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Baris " + lineNumber + ": Error processing detail data - " + e.getMessage());
            return false;
        }
    }

    private String getValue(String[] data, Map<String, Integer> columnIndexes, String columnName) {
        if (columnIndexes.containsKey(columnName)) {
            int index = columnIndexes.get(columnName);
            if (index < data.length) {
                return data[index].trim();
            }
        }
        return null;
    }

    private void showCombinedUploadResult(int masterSuccess, int masterError,
            int detailSuccess, int detailError, String fileName) {
        StringBuilder result = new StringBuilder();
        result.append("Hasil Upload Data Gabungan\n");
        result.append("File: ").append(fileName).append("\n\n");

        result.append("Data Master (Tab 1):\n");
        result.append("Berhasil: ").append(masterSuccess).append("\n");
        result.append("Gagal: ").append(masterError).append("\n\n");

        result.append("Data Detail (Tab 2):\n");
        result.append("Berhasil: ").append(detailSuccess).append("\n");
        result.append("Gagal: ").append(detailError).append("\n\n");

        result.append("Keterangan:\n");
        result.append("   - Data yang sudah ada akan diupdate\n");
        result.append("   - Data baru akan ditambahkan\n");
        result.append("   - Lihat console untuk detail error");

        JOptionPane.showMessageDialog(null, result.toString(), "Hasil Upload Gabungan",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
