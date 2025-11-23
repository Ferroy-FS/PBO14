/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.CSVHandler to edit this template
 */
package Dialogue;

import net.sf.jasperreports.engine.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 *
 * @author LEGION
 */
public class PenjualanPerangkatElektronik extends javax.swing.JFrame {

    private EntityManagerFactory emf;
    private EntityManager em;
    private CSVHandler csvHandler;
    private MasterDataCSVHandler masterDataCSVHandler;
    private JasperReportHandler jasperReportHandler;
    private static final Logger logger = Logger.getLogger(PenjualanPerangkatElektronik.class.getName());

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/PBO_Praktikum_5";
    private static final String USER = "postgres";
    private static final String PASS = "0000";

    public PenjualanPerangkatElektronik() {
        initComponents();
        connectToDatabase();
        loadDataToTable();
        setupTableSelectionListener();
        resetDetailSequence();
    }

    private void connectToDatabase() {
        try {
            emf = Persistence.createEntityManagerFactory("PBOpertemuan6PU");
            em = emf.createEntityManager();
            csvHandler = new CSVHandler(emf);
            masterDataCSVHandler = new MasterDataCSVHandler(emf);
            jasperReportHandler = new JasperReportHandler(emf);
            System.out.println("Koneksi database berhasil (JPA)!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal terhubung ke database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDataToTable() {
        try {
            if (em != null && em.isOpen()) {
                em.clear();
            }

            // Load data perangkat
            String queryPerangkat = "SELECT p FROM PerangkatElektronik p";
            TypedQuery<PerangkatElektronik> typedQueryPerangkat = em.createQuery(queryPerangkat, PerangkatElektronik.class);
            List<PerangkatElektronik> dataListPerangkat = typedQueryPerangkat.getResultList();

            DefaultTableModel modelPerangkat = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            modelPerangkat.addColumn("Nomor Seri");
            modelPerangkat.addColumn("Jenis Perangkat");
            modelPerangkat.addColumn("Merek Perangkat");
            modelPerangkat.addColumn("Nama Perangkat");
            modelPerangkat.addColumn("Model Perangkat");

            modelPerangkat.setRowCount(0);

            for (PerangkatElektronik data : dataListPerangkat) {
                Object[] row = {
                    data.getNomorSeri(),
                    data.getJenisPerangkat(),
                    data.getMerekPerangkat(),
                    data.getNamaPerangkat(),
                    data.getModelPerangkat()
                };
                modelPerangkat.addRow(row);
            }

            jTable1.setModel(modelPerangkat);

            // Load data detail
            String queryDetail = "SELECT d FROM DetailPerangkat d ORDER BY d.idDetail";
            TypedQuery<DetailPerangkat> typedQueryDetail = em.createQuery(queryDetail, DetailPerangkat.class);
            List<DetailPerangkat> dataListDetail = typedQueryDetail.getResultList();

            DefaultTableModel modelDetail = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            modelDetail.addColumn("ID Detail");
            modelDetail.addColumn("Nama Perangkat");
            modelDetail.addColumn("Warna");
            modelDetail.addColumn("Harga");
            modelDetail.addColumn("Stok");

            modelDetail.setRowCount(0);

            for (DetailPerangkat data : dataListDetail) {
                Object[] row = {
                    data.getIdDetail(),
                    data.getPerangkatElektronik().getNamaPerangkat(),
                    data.getWarna(),
                    formatRupiah(data.getHarga()),
                    data.getStok()
                };
                modelDetail.addRow(row);
            }

            jTable2.setModel(modelDetail);

            // Set column widths
            jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < modelPerangkat.getColumnCount(); i++) {
                jTable1.getColumnModel().getColumn(i).setPreferredWidth(150);
            }

            jTable2.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jTable2.getColumnModel().getColumn(0).setPreferredWidth(80);
            jTable2.getColumnModel().getColumn(1).setPreferredWidth(200);
            jTable2.getColumnModel().getColumn(2).setPreferredWidth(100);
            jTable2.getColumnModel().getColumn(3).setPreferredWidth(120);
            jTable2.getColumnModel().getColumn(4).setPreferredWidth(80);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void downloadReport() {
        try {
            // Compile Jasper Report
            JasperReport jasperReport = JasperCompileManager.compileReport("C/Work/Kuliah/SEMESTER 3/PBO/Pertemuan 13 Chrome/PBO13-main/Source Code/PBOpertemuan13/src/reports/laporan_gabungan_perangkat.jrxml");

            // Parameters jika ada
            Map<String, Object> parameters = new HashMap<>();

            // Data source
            JRDataSource dataSource = new JRBeanCollectionDataSource(getDataForReport());

            // Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Export to PDF
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Simpan Laporan");
            fileChooser.setSelectedFile(new File("laporan_perangkat.pdf"));

            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                JasperExportManager.exportReportToPdfFile(jasperPrint, file.getAbsolutePath());
                JOptionPane.showMessageDialog(null, "Laporan berhasil diunduh!");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<DetailPerangkat> getDataForReport() {
        // Ambil data dari database untuk report
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT d FROM DetailPerangkat d", DetailPerangkat.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private void downloadCombinedData() {
        try {
            jasperReportHandler.downloadCombinedDataToCSV();
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saat download data gabungan: " + e.getMessage(),
                    "Error Download",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void uploadCombinedData() {
        try {
            jasperReportHandler.uploadCombinedDataFromCSV();
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saat upload data gabungan: " + e.getMessage(),
                    "Error Upload",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private String formatRupiah(long harga) {
        try {
            NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatRupiah.format(harga);
        } catch (Exception e) {
            return "Rp " + String.format("%,d", harga);
        }
    }

    private boolean isNomorSeriExists(String nomorSeri) {
        try {
            String query = "SELECT COUNT(p) FROM PerangkatElektronik p WHERE p.nomorSeri = :nomorSeri";
            TypedQuery<Long> typedQuery = em.createQuery(query, Long.class);
            typedQuery.setParameter("nomorSeri", nomorSeri);
            Long count = typedQuery.getSingleResult();
            System.out.println("Cek nomor seri '" + nomorSeri + "': " + (count > 0 ? "ADA" : "TIDAK ADA"));
            return count > 0;
        } catch (Exception e) {
            System.out.println("Error cek nomor seri '" + nomorSeri + "': " + e.getMessage());
            return false;
        }
    }

    private void uploadCSVForTab1() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Pilih File CSV untuk Tabel Perangkat");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("File dipilih untuk Tab 1: " + selectedFile.getAbsolutePath());

                int recordsProcessed = processCSVForTab1(selectedFile);

                if (recordsProcessed > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Berhasil mengupload " + recordsProcessed + " record ke tabel perangkat!",
                            "Upload Berhasil",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Tidak ada data yang berhasil diupload.\n\nPastikan:\n• Format CSV sesuai\n• Data belum duplikat",
                            "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saat upload CSV: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void uploadCSVForTab2() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Pilih File CSV untuk Detail Perangkat");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("====== UPLOAD FILE: " + selectedFile.getName() + " ======");

                int recordsProcessed = processCSVForTab2(selectedFile);

                if (recordsProcessed > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Upload Berhasil!\n\n"
                            + "File: " + selectedFile.getName() + "\n"
                            + "Data diproses: " + recordsProcessed + " record\n"
                            + "Data telah disimpan ke tabel Detail Perangkat",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Tidak ada data yang berhasil diupload.\n\n"
                            + "File: " + selectedFile.getName() + "\n"
                            + "Kemungkinan penyebab:\n"
                            + "• Data sudah ada sebelumnya\n"
                            + "• Format tidak sesuai\n"
                            + "• Data tidak lengkap\n\n"
                            + "Lihat console untuk detail error.",
                            "Info Upload",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saat upload CSV: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void processCSVFolder(File folder) {
        try {
            System.out.println("=== PROCESSING FOLDER: " + folder.getAbsolutePath() + " ===");

            File[] csvFiles = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".csv");
                }
            });

            if (csvFiles == null || csvFiles.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "Tidak ada file CSV yang ditemukan dalam folder!",
                        "Folder Kosong",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            System.out.println("Ditemukan " + csvFiles.length + " file CSV:");
            for (File file : csvFiles) {
                System.out.println(" - " + file.getName());
            }

            int totalRecordsProcessed = 0;
            int totalFilesProcessed = 0;

            for (File csvFile : csvFiles) {
                System.out.println("\n=== PROCESSING FILE: " + csvFile.getName() + " ===");

                int recordsProcessed = processCSVForTab2(csvFile);

                if (recordsProcessed > 0) {
                    totalRecordsProcessed += recordsProcessed;
                    totalFilesProcessed++;
                    System.out.println("File " + csvFile.getName() + ": " + recordsProcessed + " record berhasil");
                } else {
                    System.out.println("File " + csvFile.getName() + ": Tidak ada record yang diproses");
                }
            }

            String resultMessage = String.format(
                    "Upload Folder Selesai!\n\n"
                    + "File yang diproses: %d dari %d\n"
                    + "Total record berhasil: %d\n\n"
                    + "Data telah ditambahkan ke Tabel Detail Perangkat (Tab 2)",
                    totalFilesProcessed, csvFiles.length, totalRecordsProcessed
            );

            JOptionPane.showMessageDialog(this,
                    resultMessage,
                    "Upload Folder Berhasil",
                    JOptionPane.INFORMATION_MESSAGE);

            refreshTable();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error memproses folder: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private int processCSVForTab1(File csvFile) {
        int recordsProcessed = 0;
        int recordsSkipped = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;
            List<String> headers = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isFirstLine) {
                    isFirstLine = false;
                    headers = Arrays.asList(line.split(","));

                    if (!isValidTab1CSVFormat(headers)) {
                        JOptionPane.showMessageDialog(this,
                                "Format CSV tidak sesuai untuk Tabel Perangkat!\n\n"
                                + "Header yang diharapkan:\n"
                                + "nomor_seri, jenis_perangkat, merek_perangkat, nama_perangkat, model_perangkat\n\n"
                                + "Header yang ditemukan:\n" + line,
                                "Format CSV Salah",
                                JOptionPane.ERROR_MESSAGE);
                        return 0;
                    }
                    continue;
                }

                String[] data = line.split(",");

                if (data.length >= 5) {
                    String nomorSeri = data[0].trim();
                    String jenis = data[1].trim();
                    String merek = data[2].trim();
                    String nama = data[3].trim();
                    String model = data[4].trim();

                    if (nomorSeri.isEmpty() || jenis.isEmpty() || merek.isEmpty() || nama.isEmpty()) {
                        System.out.println("Data tidak lengkap pada baris " + lineNumber + ", dilewati.");
                        recordsSkipped++;
                        continue;
                    }

                    if (!isNomorSeriExists(nomorSeri)) {
                        PerangkatElektronik newData = new PerangkatElektronik();
                        newData.setNomorSeri(nomorSeri);
                        newData.setJenisPerangkat(jenis);
                        newData.setMerekPerangkat(merek);
                        newData.setNamaPerangkat(nama);
                        newData.setModelPerangkat(model);

                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }

                        em.getTransaction().begin();
                        em.persist(newData);
                        em.getTransaction().commit();

                        recordsProcessed++;
                        System.out.println("Berhasil: " + nomorSeri + " - " + merek + " " + nama);
                    } else {
                        System.out.println("Nomor seri " + nomorSeri + " sudah ada, dilewati.");
                        recordsSkipped++;
                    }
                } else {
                    System.out.println("Baris " + lineNumber + " tidak valid (kurang dari 5 kolom): " + line);
                    recordsSkipped++;
                }
            }

            System.out.println("Upload Tab 1 selesai: " + recordsProcessed + " record diproses, " + recordsSkipped + " record dilewati.");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            JOptionPane.showMessageDialog(this,
                    "Error memproses file CSV: " + e.getMessage() + " (Baris: " + lineNumber + ")",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        return recordsProcessed;
    }

    private int processCSVForTab2(File csvFile) {
        int recordsProcessed = 0;
        int recordsSkipped = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;
            List<String> headers = new ArrayList<>();

            int nomorSeriIndex = -1;
            int warnaIndex = -1;
            int hargaIndex = -1;
            int stokIndex = -1;
            boolean hasIdDetail = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isFirstLine) {
                    isFirstLine = false;
                    headers = Arrays.asList(line.split(","));

                    System.out.println("Headers ditemukan: " + headers);

                    detectColumnIndexes(headers, csvFile.getName());

                    nomorSeriIndex = findColumnIndex(headers,
                            Arrays.asList("nomor_seri", "nomor seri", "seri", "noseri", "kode"));
                    warnaIndex = findColumnIndex(headers,
                            Arrays.asList("warna", "color", "colour", "varian"));
                    hargaIndex = findColumnIndex(headers,
                            Arrays.asList("harga", "price", "cost", "nilai"));
                    stokIndex = findColumnIndex(headers,
                            Arrays.asList("stok", "stock", "quantity", "qty", "jumlah"));
                    hasIdDetail = findColumnIndex(headers,
                            Arrays.asList("id_detail", "iddetail", "id", "detail_id")) != -1;

                    System.out.println("Format terdeteksi:");
                    System.out.println(" - Nomor Seri: kolom " + nomorSeriIndex + " (" + (nomorSeriIndex >= 0 ? headers.get(nomorSeriIndex) : "TIDAK DITEMUKAN") + ")");
                    System.out.println(" - Warna: kolom " + warnaIndex + " (" + (warnaIndex >= 0 ? headers.get(warnaIndex) : "TIDAK DITEMUKAN") + ")");
                    System.out.println(" - Harga: kolom " + hargaIndex + " (" + (hargaIndex >= 0 ? headers.get(hargaIndex) : "TIDAK DITEMUKAN") + ")");
                    System.out.println(" - Stok: kolom " + stokIndex + " (" + (stokIndex >= 0 ? headers.get(stokIndex) : "TIDAK DITEMUKAN") + ")");
                    System.out.println(" - ID Detail: " + (hasIdDetail ? "ADA" : "TIDAK ADA"));

                    if (nomorSeriIndex == -1 || warnaIndex == -1 || hargaIndex == -1) {
                        JOptionPane.showMessageDialog(this,
                                "Format CSV tidak dapat diproses!\n\n"
                                + "Kolom wajib tidak ditemukan:\n"
                                + (nomorSeriIndex == -1 ? "• Nomor Seri\n" : "")
                                + (warnaIndex == -1 ? "• Warna\n" : "")
                                + (hargaIndex == -1 ? "• Harga\n" : "")
                                + "\nHeader yang ditemukan:\n" + String.join(" | ", headers)
                                + "\n\nPastikan file memiliki kolom: nomor_seri, warna, harga",
                                "Format Tidak Didukung",
                                JOptionPane.ERROR_MESSAGE);
                        return 0;
                    }
                    continue;
                }

                String[] data = line.split(",");

                if (data.length <= Math.max(nomorSeriIndex, Math.max(warnaIndex, hargaIndex))) {
                    System.out.println("✗ Baris " + lineNumber + " tidak valid (data kurang): " + line);
                    recordsSkipped++;
                    continue;
                }

                String nomorSeri = data[nomorSeriIndex].trim();
                String warna = data[warnaIndex].trim();
                long harga = 0;
                int stok = 0;

                try {
                    harga = Long.parseLong(data[hargaIndex].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Format harga tidak valid pada baris " + lineNumber + ": " + data[hargaIndex]);
                    recordsSkipped++;
                    continue;
                }

                if (stokIndex != -1 && data.length > stokIndex) {
                    try {
                        stok = Integer.parseInt(data[stokIndex].trim());
                    } catch (NumberFormatException e) {
                        stok = 0;
                    }
                }

                if (nomorSeri.isEmpty() || warna.isEmpty()) {
                    System.out.println("Data tidak lengkap pada baris " + lineNumber + ", dilewati.");
                    recordsSkipped++;
                    continue;
                }

                if (!isNomorSeriExists(nomorSeri)) {
                    if (!createPerangkatOtomatis(nomorSeri, csvFile.getName())) {
                        recordsSkipped++;
                        continue;
                    }
                }

                if (isDetailExistsForPerangkat(nomorSeri, warna)) {
                    System.out.println(" Detail untuk " + nomorSeri + " - " + warna + " sudah ada, dilewati.");
                    recordsSkipped++;
                    continue;
                }

                if (saveDetailPerangkat(nomorSeri, warna, harga, stok)) {
                    recordsProcessed++;
                    System.out.println(" Berhasil: " + nomorSeri + " - " + warna + " - Rp " + harga + " - Stok: " + stok);
                } else {
                    recordsSkipped++;
                }
            }

            System.out.println("=== HASIL UPLOAD " + csvFile.getName() + " ===");
            System.out.println(" Record diproses: " + recordsProcessed);
            System.out.println(" Record dilewati: " + recordsSkipped);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            JOptionPane.showMessageDialog(this,
                    "Error memproses file " + csvFile.getName() + ":\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        return recordsProcessed;
    }

    private int findColumnIndex(List<String> headers, List<String> possibleNames) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).trim().toLowerCase().replaceAll("\\s+", "_");
            for (String possibleName : possibleNames) {
                if (header.contains(possibleName.toLowerCase())
                        || possibleName.toLowerCase().contains(header)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void detectColumnIndexes(List<String> headers, String fileName) {
        System.out.println(" Mendeteksi format untuk file: " + fileName);

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).trim().toLowerCase();
            System.out.println("  Kolom " + i + ": '" + header + "'");
        }
    }

    private boolean isValidTab1CSVFormat(List<String> headers) {

        List<String> expectedHeaders = Arrays.asList(
                "nomor_seri", "jenis_perangkat", "merek_perangkat", "nama_perangkat", "model_perangkat"
        );

        List<String> normalizedHeaders = headers.stream()
                .map(h -> h.trim().toLowerCase().replaceAll("\\s+", "_"))
                .collect(Collectors.toList());

        return normalizedHeaders.containsAll(expectedHeaders)
                || normalizedHeaders.contains("nomor seri")
                || normalizedHeaders.contains("jenis perangkat")
                || normalizedHeaders.contains("merek perangkat")
                || normalizedHeaders.contains("nama perangkat")
                || normalizedHeaders.contains("model perangkat");
    }

    private boolean isValidTab2CSVFormat(List<String> headers) {
        List<String> expectedHeaders = Arrays.asList(
                "nomor_seri", "warna", "harga", "stok"
        );

        List<String> expectedHeadersWithId = Arrays.asList(
                "id_detail", "nomor_seri", "warna", "harga", "stok"
        );

        List<String> normalizedHeaders = headers.stream()
                .map(h -> h.trim().toLowerCase().replaceAll("\\s+", "_"))
                .collect(Collectors.toList());

        System.out.println("Headers yang ditemukan: " + normalizedHeaders);

        boolean isValidWithId = normalizedHeaders.containsAll(expectedHeadersWithId);
        boolean isValidWithoutId = normalizedHeaders.containsAll(expectedHeaders);

        return isValidWithId || isValidWithoutId;
    }

    private boolean isDetailExistsForPerangkat(String nomorSeri, String warna) {
        try {
            String query = "SELECT COUNT(d) FROM DetailPerangkat d WHERE d.perangkatElektronik.nomorSeri = :nomorSeri AND d.warna = :warna";
            TypedQuery<Long> typedQuery = em.createQuery(query, Long.class);
            typedQuery.setParameter("nomorSeri", nomorSeri);
            typedQuery.setParameter("warna", warna);
            return typedQuery.getSingleResult() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean saveDetailPerangkat(String nomorSeri, String warna, long harga, int stok) {
        try {
            PerangkatElektronik perangkat = em.find(PerangkatElektronik.class, nomorSeri);
            if (perangkat == null) {
                System.out.println(" Perangkat dengan nomor seri " + nomorSeri + " tidak ditemukan!");
                return false;
            }

            DetailPerangkat detail = new DetailPerangkat();
            detail.setWarna(warna);
            detail.setHarga(harga);
            detail.setStok(stok);
            detail.setPerangkatElektronik(perangkat);

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            em.getTransaction().begin();
            em.persist(detail);
            em.getTransaction().commit();

            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println(" Gagal menyimpan detail: " + e.getMessage());
            return false;
        }
    }

    private void refreshAllTables() {
        try {
            // Clear entity manager cache
            if (em != null && em.isOpen()) {
                em.clear();
            }

            // Refresh kedua tabel
            loadDataToTable();

            // Optional: Tampilkan pesan sukses
            JOptionPane.showMessageDialog(null,
                    "Data berhasil diupload dan tabel telah di-refresh!",
                    "Refresh Berhasil",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            System.out.println("Error refreshing tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cetakLaporan() {
        try {
            String jrxmlPath = "src/reports/laporan_gabungan_perangkat.jrxml";
            String jasperPath = "laporan_gabungan_perangkat.jasper";

            File jrxmlFile = new File(jrxmlPath);
            File jasperFile = new File(jasperPath);

            System.out.println("=== CETAK LAPORAN GABUNGAN ===");
            System.out.println("File JRXML: " + jrxmlFile.getAbsolutePath());
            System.out.println("File JASPER: " + jasperFile.getAbsolutePath());

            if (!jasperFile.exists()) {
                System.out.println("File JASPER tidak ditemukan, compiling...");
                try {
                    JasperCompileManager.compileReportToFile(jrxmlPath, jasperPath);
                    System.out.println("✓ Compile berhasil!");

                    jasperFile = new File(jasperPath);
                    if (!jasperFile.exists()) {
                        JOptionPane.showMessageDialog(this, "Gagal membuat file JASPER setelah compile!");
                        return;
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Gagal compile laporan: " + e.getMessage()
                            + "\n\nPastikan file JRXML ada di: " + jrxmlFile.getAbsolutePath(),
                            "Error Compile",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            System.out.println("Testing database connection...");
            Connection jdbcConn = DriverManager.getConnection(DB_URL, USER, PASS);

            // ========== DEBUGGING DETAIL ==========
            Statement testStmt = jdbcConn.createStatement();

            // Test 1: Cek data di masing-masing tabel
            ResultSet rsPerangkat = testStmt.executeQuery("SELECT COUNT(*) as total, string_agg(nomor_seri, ', ') as nomor_seri_list FROM perangkat_elektronik");
            if (rsPerangkat.next()) {
                int totalPerangkat = rsPerangkat.getInt("total");
                String nomorSeriList = rsPerangkat.getString("nomor_seri_list");
                System.out.println("✓ Total perangkat_elektronik: " + totalPerangkat);
                System.out.println("✓ Nomor seri yang ada: " + (nomorSeriList != null ? nomorSeriList : "TIDAK ADA"));
            }
            rsPerangkat.close();

            ResultSet rsDetail = testStmt.executeQuery("SELECT COUNT(*) as total, string_agg(nomor_seri, ', ') as nomor_seri_list FROM detail_perangkat");
            if (rsDetail.next()) {
                int totalDetail = rsDetail.getInt("total");
                String nomorSeriDetailList = rsDetail.getString("nomor_seri_list");
                System.out.println("✓ Total detail_perangkat: " + totalDetail);
                System.out.println("✓ Nomor seri di detail: " + (nomorSeriDetailList != null ? nomorSeriDetailList : "TIDAK ADA"));
            }
            rsDetail.close();

            // Test 2: Cek apakah ada data yang match antara kedua tabel
            ResultSet rsMatch = testStmt.executeQuery(
                    "SELECT pe.nomor_seri as pe_seri, dp.nomor_seri as dp_seri "
                    + "FROM perangkat_elektronik pe "
                    + "FULL OUTER JOIN detail_perangkat dp ON pe.nomor_seri = dp.nomor_seri "
                    + "WHERE pe.nomor_seri IS NOT NULL AND dp.nomor_seri IS NOT NULL"
            );

            int matchCount = 0;
            System.out.println("=== DATA YANG MATCH ===");
            while (rsMatch.next()) {
                matchCount++;
                System.out.println("Match " + matchCount + ": "
                        + rsMatch.getString("pe_seri") + " = " + rsMatch.getString("dp_seri"));
            }
            rsMatch.close();

            System.out.println("Total data yang match: " + matchCount);

            // Test 3: Cek data gabungan dengan query yang sama seperti JRXML
            ResultSet rsGabungan = testStmt.executeQuery(
                    "SELECT pe.nomor_seri, pe.nama_perangkat, dp.id_detail, dp.warna, dp.harga, dp.stok "
                    + "FROM perangkat_elektronik pe "
                    + "INNER JOIN detail_perangkat dp ON pe.nomor_seri = dp.nomor_seri "
                    + "ORDER BY pe.nomor_seri, dp.id_detail"
            );

            int rowCount = 0;
            System.out.println("=== DATA GABUNGAN YANG DITEMUKAN ===");
            while (rsGabungan.next()) {
                rowCount++;
                System.out.println("Row " + rowCount + ": "
                        + rsGabungan.getString("nomor_seri") + " - "
                        + rsGabungan.getString("nama_perangkat") + " - "
                        + rsGabungan.getString("warna") + " - Rp "
                        + rsGabungan.getLong("harga") + " - Stok: "
                        + rsGabungan.getInt("stok"));
            }
            rsGabungan.close();
            testStmt.close();

            if (rowCount == 0) {
                // Tampilkan data yang tidak match
                Statement stmt2 = jdbcConn.createStatement();
                ResultSet rsUnmatched = stmt2.executeQuery(
                        "SELECT 'Perangkat tanpa detail' as type, pe.nomor_seri, pe.nama_perangkat "
                        + "FROM perangkat_elektronik pe "
                        + "LEFT JOIN detail_perangkat dp ON pe.nomor_seri = dp.nomor_seri "
                        + "WHERE dp.nomor_seri IS NULL "
                        + "UNION ALL "
                        + "SELECT 'Detail tanpa perangkat' as type, dp.nomor_seri, 'TIDAK ADA' as nama_perangkat "
                        + "FROM detail_perangkat dp "
                        + "LEFT JOIN perangkat_elektronik pe ON dp.nomor_seri = pe.nomor_seri "
                        + "WHERE pe.nomor_seri IS NULL"
                );

                StringBuilder unmatchedInfo = new StringBuilder();
                unmatchedInfo.append("Data yang tidak match:\n\n");

                boolean hasUnmatched = false;
                while (rsUnmatched.next()) {
                    hasUnmatched = true;
                    unmatchedInfo.append(rsUnmatched.getString("type"))
                            .append(": ")
                            .append(rsUnmatched.getString("nomor_seri"))
                            .append("\n");
                }
                rsUnmatched.close();
                stmt2.close();

                if (hasUnmatched) {
                    unmatchedInfo.append("\nPastikan nomor_seri di kedua tabel sama!");
                } else {
                    unmatchedInfo.append("Kedua tabel kosong!");
                }

                JOptionPane.showMessageDialog(this,
                        "Tidak ada data yang ditemukan untuk laporan!\n\n"
                        + unmatchedInfo.toString(),
                        "Data Kosong",
                        JOptionPane.WARNING_MESSAGE);
                jdbcConn.close();
                return;
            }

            System.out.println("✓ Total data untuk laporan: " + rowCount);
            // ========== END DEBUGGING ==========

            System.out.println("Memulai generate report gabungan...");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", "LAPORAN GABUNGAN PERANGKAT ELEKTRONIK");

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperPath,
                    parameters,
                    jdbcConn
            );

            if (jasperPrint.getPages().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Laporan berhasil digenerate tetapi tidak memiliki halaman.\n"
                        + "Kemungkinan data tidak sesuai dengan format yang diharapkan.",
                        "Laporan Kosong",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("Laporan Gabungan Perangkat Elektronik");
            viewer.setVisible(true);

            jdbcConn.close();
            System.out.println("=== LAPORAN GABUNGAN BERHASIL DICETAK ===");

        } catch (Exception e) {
            System.out.println("=== ERROR CETAK LAPORAN GABUNGAN ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();

            JOptionPane.showMessageDialog(this,
                    "Error saat mencetak laporan: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        try {
            if (em != null && em.isOpen()) {
                em.close();
            }
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    private boolean createPerangkatOtomatis(String nomorSeri, String fileName) {
        try {
            String jenisPerangkat = detectJenisPerangkat(fileName);
            String merek = detectMerekFromNomorSeri(nomorSeri);

            PerangkatElektronik newPerangkat = new PerangkatElektronik();
            newPerangkat.setNomorSeri(nomorSeri);
            newPerangkat.setJenisPerangkat(jenisPerangkat);
            newPerangkat.setMerekPerangkat(merek);
            newPerangkat.setNamaPerangkat(jenisPerangkat + " " + nomorSeri);
            newPerangkat.setModelPerangkat("Standard");

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            em.getTransaction().begin();
            em.persist(newPerangkat);
            em.getTransaction().commit();

            System.out.println(" Perangkat baru dibuat: " + nomorSeri + " (" + jenisPerangkat + ")");
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println(" Gagal membuat perangkat: " + nomorSeri + " - " + e.getMessage());
            return false;
        }
    }

    private String detectJenisPerangkat(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.contains("laptop")) {
            return "Laptop";
        }
        if (fileName.contains("tablet")) {
            return "Tablet";
        }
        if (fileName.contains("smartphone")) {
            return "Smartphone";
        }
        if (fileName.contains("handphone")) {
            return "Smartphone";
        }
        if (fileName.contains("tv")) {
            return "Televisi";
        }
        if (fileName.contains("televisi")) {
            return "Televisi";
        }
        return "Elektronik";
    }

    private String detectMerekFromNomorSeri(String nomorSeri) {
        if (nomorSeri.startsWith("L")) {
            return "Laptop Brand";
        }
        if (nomorSeri.startsWith("T")) {
            return "Tablet Brand";
        }
        if (nomorSeri.startsWith("S")) {
            return "Smartphone Brand";
        }
        if (nomorSeri.startsWith("H")) {
            return "Handphone Brand";
        }
        return "Unknown Brand";
    }

    private void setupTableSelectionListener() {
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = jTable1.getSelectedRow();
                if (row >= 0) {
                }
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
            }
        });
    }

    private void refreshTable() {
        try {
            if (em != null && em.isOpen() && !em.getTransaction().isActive()) {
                em.clear();
            }

            loadDataToTable();

            JOptionPane.showMessageDialog(this, "Tabel telah di-refresh dengan data terbaru!",
                    "Refresh Berhasil", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saat refresh: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private int getActiveTab() {
        return jTabbedPane1.getSelectedIndex();
    }

    private void showInsertDialog() {
        int activeTab = getActiveTab();

        if (activeTab == 0) {
            Dialog dialog = new Dialog();
            dialog.setOperation("INSERT");
            dialog.setFieldValues("", "", "", "", "");
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                refreshTable();
            }
        } else if (activeTab == 1) {
            DetailDialog detailDialog = new DetailDialog();
            detailDialog.setOperation("INSERT");
            detailDialog.setVisible(true);

            if (detailDialog.isConfirmed()) {
                refreshTable();
            }
        }
    }

    private void showUpdateDialog() {
        int activeTab = getActiveTab();

        if (activeTab == 0) {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Pilih data yang akan diupdate terlebih dahulu!");
                return;
            }

            String nomorSeri = jTable1.getValueAt(selectedRow, 0).toString();
            String jenis = jTable1.getValueAt(selectedRow, 1).toString();
            String merek = jTable1.getValueAt(selectedRow, 2).toString();
            String nama = jTable1.getValueAt(selectedRow, 3).toString();
            String model = jTable1.getValueAt(selectedRow, 4).toString();

            Dialog dialog = new Dialog();
            dialog.setOperation("UPDATE");
            dialog.setFieldValues(nomorSeri, jenis, merek, nama, model);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                refreshTable();
            }
        } else if (activeTab == 1) {
            int selectedRow = jTable2.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Pilih data detail yang akan diupdate terlebih dahulu!");
                return;
            }

            Integer idDetail = (Integer) jTable2.getValueAt(selectedRow, 0);

            String nomorSeriFound = findNomorSeriByIdDetail(idDetail);
            if (nomorSeriFound == null) {
                JOptionPane.showMessageDialog(this, "Gagal menemukan data perangkat!");
                return;
            }

            String warna = jTable2.getValueAt(selectedRow, 2).toString();
            String harga = extractNumericValue(jTable2.getValueAt(selectedRow, 3).toString());
            String stok = jTable2.getValueAt(selectedRow, 4).toString();

            DetailDialog detailDialog = new DetailDialog("UPDATE", idDetail, nomorSeriFound, warna, harga, stok);
            detailDialog.setVisible(true);

            if (detailDialog.isConfirmed()) {
                refreshTable();
            }
        }
    }

    private void showDeleteDialog() {
        int activeTab = getActiveTab();

        if (activeTab == 0) {
            int selectedRow = jTable1.getSelectedRow();
            String nomorSeri = "";

            if (selectedRow != -1) {
                nomorSeri = jTable1.getValueAt(selectedRow, 0).toString();
            } else {
                nomorSeri = JOptionPane.showInputDialog(this, "Masukkan nomor seri yang akan dihapus:");
                if (nomorSeri == null || nomorSeri.trim().isEmpty()) {
                    return;
                }
            }

            if (deletePerangkatWithDetails(nomorSeri)) {
                refreshTable();
            }

        } else if (activeTab == 1) {
            int selectedRow = jTable2.getSelectedRow();
            Integer idDetail = null;
            String namaPerangkat = "";

            if (selectedRow != -1) {
                idDetail = (Integer) jTable2.getValueAt(selectedRow, 0);
                namaPerangkat = jTable2.getValueAt(selectedRow, 1).toString();
            } else {
                String idDetailStr = JOptionPane.showInputDialog(this,
                        "Masukkan ID Detail yang akan dihapus:\n\n"
                        + "Catatan: ID Detail bisa dilihat di kolom pertama tabel");

                if (idDetailStr == null || idDetailStr.trim().isEmpty()) {
                    return;
                }

                try {
                    idDetail = Integer.parseInt(idDetailStr.trim());
                    namaPerangkat = findNamaPerangkatByIdDetail(idDetail);
                    if (namaPerangkat == null) {
                        JOptionPane.showMessageDialog(this, "ID Detail tidak ditemukan!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "ID Detail harus berupa angka!");
                    return;
                }
            }

            if (deleteDetailPerangkat(idDetail)) {
                refreshTable();
            }
        }
    }

    private void searchData() {
        int activeTab = getActiveTab();
        String keyword = JOptionPane.showInputDialog(this, "Masukkan kata kunci pencarian:");

        if (keyword == null) {
            return;
        }
        keyword = keyword.trim();

        try {
            if (activeTab == 0) {
                String query = "SELECT p FROM PerangkatElektronik p WHERE "
                        + "LOWER(p.nomorSeri) LIKE LOWER(:keyword) OR "
                        + "LOWER(p.jenisPerangkat) LIKE LOWER(:keyword) OR "
                        + "LOWER(p.merekPerangkat) LIKE LOWER(:keyword) OR "
                        + "LOWER(p.namaPerangkat) LIKE LOWER(:keyword) OR "
                        + "LOWER(p.modelPerangkat) LIKE LOWER(:keyword)";

                TypedQuery<PerangkatElektronik> typedQuery = em.createQuery(query, PerangkatElektronik.class);
                typedQuery.setParameter("keyword", "%" + keyword + "%");

                List<PerangkatElektronik> searchResults = typedQuery.getResultList();

                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                model.setRowCount(0);

                for (PerangkatElektronik data : searchResults) {
                    Object[] row = {
                        data.getNomorSeri(),
                        data.getJenisPerangkat(),
                        data.getMerekPerangkat(),
                        data.getNamaPerangkat(),
                        data.getModelPerangkat()
                    };
                    model.addRow(row);
                }
            } else if (activeTab == 1) {
                String query = "SELECT d FROM DetailPerangkat d WHERE "
                        + "LOWER(d.perangkatElektronik.namaPerangkat) LIKE LOWER(:keyword) OR "
                        + "LOWER(d.warna) LIKE LOWER(:keyword) OR "
                        + "CAST(d.harga AS text) LIKE :keyword OR "
                        + "CAST(d.stok AS text) LIKE :keyword OR "
                        + "LOWER(d.perangkatElektronik.nomorSeri) LIKE LOWER(:keyword)";

                TypedQuery<DetailPerangkat> typedQuery = em.createQuery(query, DetailPerangkat.class);
                typedQuery.setParameter("keyword", "%" + keyword + "%");

                List<DetailPerangkat> searchResults = typedQuery.getResultList();

                DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
                model.setRowCount(0);

                for (DetailPerangkat data : searchResults) {
                    Object[] row = {
                        data.getIdDetail(),
                        data.getPerangkatElektronik().getNamaPerangkat(),
                        data.getWarna(),
                        formatRupiah(data.getHarga()),
                        data.getStok()
                    };
                    model.addRow(row);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void uploadMultipleCSV() {
        try {
            jasperReportHandler.uploadMultipleFormatCSV();
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saat upload CSV: " + e.getMessage(),
                    "Error Upload",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        refreshTable();
    }

    private String findNomorSeriByIdDetail(Integer idDetail) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            String query = "SELECT d.perangkatElektronik.nomorSeri FROM DetailPerangkat d WHERE d.idDetail = :idDetail";
            TypedQuery<String> typedQuery = em.createQuery(query, String.class);
            typedQuery.setParameter("idDetail", idDetail);
            return typedQuery.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private String findNamaPerangkatByIdDetail(Integer idDetail) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            String query = "SELECT d.perangkatElektronik.namaPerangkat FROM DetailPerangkat d WHERE d.idDetail = :idDetail";
            TypedQuery<String> typedQuery = em.createQuery(query, String.class);
            typedQuery.setParameter("idDetail", idDetail);
            return typedQuery.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private String extractNumericValue(String hargaString) {
        try {
            String numericOnly = hargaString.replaceAll("[^\\d.]", "");
            return numericOnly;
        } catch (Exception e) {
            return "0";
        }
    }

    private boolean deletePerangkatWithDetails(String nomorSeri) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();

            PerangkatElektronik perangkat = em.find(PerangkatElektronik.class, nomorSeri);
            if (perangkat == null) {
                JOptionPane.showMessageDialog(this, "Data perangkat tidak ditemukan!");
                return false;
            }

            int detailCount = perangkat.getDetailPerangkatList() != null
                    ? perangkat.getDetailPerangkatList().size() : 0;

            String message = "Hapus perangkat dan semua detailnya?\n\n"
                    + "Nomor Seri: " + nomorSeri + "\n"
                    + "Nama Perangkat: " + perangkat.getNamaPerangkat() + "\n"
                    + "Jumlah Detail: " + detailCount + " data\n\n"
                    + "PERINGATAN: Semua data detail juga akan terhapus!";

            int confirm = JOptionPane.showConfirmDialog(this,
                    message,
                    "Konfirmasi Hapus dengan Cascade",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return false;
            }

            em.getTransaction().begin();

            em.remove(perangkat);

            em.getTransaction().commit();

            JOptionPane.showMessageDialog(this,
                    "Data perangkat dan " + detailCount + " data detail berhasil dihapus!",
                    "Hapus Berhasil",
                    JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            JOptionPane.showMessageDialog(this,
                    "Gagal menghapus data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private boolean deleteDetailPerangkat(Integer idDetail) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            DetailPerangkat detail = em.find(DetailPerangkat.class, idDetail);
            if (detail == null) {
                JOptionPane.showMessageDialog(this, "Data detail tidak ditemukan!");
                return false;
            }

            String info = "Hapus data detail?\n\n"
                    + "ID Detail: " + idDetail + "\n"
                    + "Nomor Seri: " + detail.getPerangkatElektronik().getNomorSeri() + "\n"
                    + "Warna: " + detail.getWarna() + "\n"
                    + "Harga: " + formatRupiah(detail.getHarga());

            int confirm = JOptionPane.showConfirmDialog(this,
                    info,
                    "Konfirmasi Hapus Detail",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return false;
            }

            em.getTransaction().begin();
            em.remove(detail);
            em.getTransaction().commit();

            resetDetailSequence();

            JOptionPane.showMessageDialog(this, "Data detail berhasil dihapus!");
            return true;

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            JOptionPane.showMessageDialog(this, "Gagal menghapus data detail: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void resetDetailSequence() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/PBO_Praktikum_5", "postgres", "0000");
            Statement stmt = conn.createStatement();

            String resetQuery = "SELECT setval('detail_perangkat_id_detail_seq', (SELECT COALESCE(MAX(id_detail), 0) + 1 FROM detail_perangkat), false)";
            stmt.execute(resetQuery);

            System.out.println("Sequence detail_perangkat_id_detail_seq berhasil direset!");

            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error reset sequence: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadCSV() {
        try {
            int selectedTab = jTabbedPane1.getSelectedIndex();

            if (selectedTab == 0) {
                masterDataCSVHandler.uploadMasterDataFromCSV();
            } else if (selectedTab == 1) {
                csvHandler.uploadFromCSV();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Pilih tab yang valid terlebih dahulu!",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE);
            }
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saat upload CSV: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnRefresh = new javax.swing.JButton();
        btnInsert = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        btnSearch = new javax.swing.JButton();
        btnCetak = new javax.swing.JButton();
        btnUpload = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        btnDownload = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(51, 204, 255));

        btnRefresh.setText("Refresh");
        btnRefresh.setMaximumSize(new java.awt.Dimension(70, 25));
        btnRefresh.setMinimumSize(new java.awt.Dimension(70, 25));
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnInsert.setText("Insert");
        btnInsert.setMaximumSize(new java.awt.Dimension(70, 25));
        btnInsert.setMinimumSize(new java.awt.Dimension(70, 25));
        btnInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInsertActionPerformed(evt);
            }
        });

        btnUpdate.setText("Update");
        btnUpdate.setMaximumSize(new java.awt.Dimension(70, 25));
        btnUpdate.setMinimumSize(new java.awt.Dimension(70, 25));
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnDelete.setText("Delete");
        btnDelete.setMaximumSize(new java.awt.Dimension(70, 25));
        btnDelete.setMinimumSize(new java.awt.Dimension(70, 25));
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Times New Roman", 1, 20)); // NOI18N
        jLabel1.setText("DATA PENJUALAN PERANGKAT");

        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnCetak.setText("Cetak");
        btnCetak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakActionPerformed(evt);
            }
        });

        btnUpload.setText("Upload");
        btnUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4", "title 5"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jTabbedPane1.addTab("Tabel Perangkat", jScrollPane1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        jTabbedPane1.addTab("Detail Perangkat", jScrollPane2);

        btnDownload.setText("Download");
        btnDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(307, 307, 307)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 911, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnInsert, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnSearch)))
                        .addGap(195, 195, 195)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnCetak)
                            .addComponent(btnUpload))
                        .addGap(98, 98, 98)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 202, Short.MAX_VALUE)
                                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(79, 79, 79))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(btnDownload)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap(9, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnDelete, btnInsert, btnUpdate});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 510, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnInsert, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnUpload)
                            .addComponent(btnDownload))))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch)
                    .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCetak))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnDelete, btnInsert, btnRefresh, btnUpdate});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
        showDeleteDialog();
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        // TODO add your handling code here:
        showUpdateDialog();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInsertActionPerformed
        // TODO add your handling code here:
        showInsertDialog();
    }//GEN-LAST:event_btnInsertActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        clearForm();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        // TODO add your handling code here:
        searchData();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakActionPerformed
        // TODO add your handling code here:
        cetakLaporan();
    }//GEN-LAST:event_btnCetakActionPerformed

    private void btnUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUploadActionPerformed
        // TODO add your handling code here:
        String[] options = {"Upload Tab Saat Ini", "Upload Multiple Format CSV", "Batal"};
        int choice = JOptionPane.showOptionDialog(this,
                "Pilih jenis upload:",
                "Upload Data",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0:
                uploadCSV();
                break;
            case 1:
                uploadMultipleCSV();
                break;
            default:
                break;
        }
    }//GEN-LAST:event_btnUploadActionPerformed

    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        // TODO add your handling code here:
        String[] options = {"Download Data Tab Saat Ini", "Download Data Gabungan (CSV)", "Batal"};
        int choice = JOptionPane.showOptionDialog(this,
                "Pilih jenis download:",
                "Download Data",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0:
                // Download data tab saat ini (fungsi lama)
                csvHandler.downloadToCSV();
                break;
            case 1:
                // Download data gabungan (fungsi baru)
                downloadCombinedData();
                break;
            default:
                break;
        }
    }//GEN-LAST:event_btnDownloadActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PenjualanPerangkatElektronik.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            Login.Login loginForm = new Login.Login();
            loginForm.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnDownload;
    private javax.swing.JButton btnInsert;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JButton btnUpload;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    // End of variables declaration//GEN-END:variables
}
