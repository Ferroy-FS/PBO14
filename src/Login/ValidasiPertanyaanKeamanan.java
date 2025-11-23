/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Login;

import javax.persistence.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author LEGION
 */
public class ValidasiPertanyaanKeamanan extends javax.swing.JFrame {
    
    private EntityManagerFactory emf;
    private EntityManager em;
    private String username;
    private boolean validationSuccess = false;
    private String tipeValidasi; // "USERNAME" atau "PASSWORD"

    private JLabel lblPertanyaan;
    private JTextField txtJawaban;
    private JButton btnSubmit;
    private JButton btnBatal;

    public ValidasiPertanyaanKeamanan(String username, String tipeValidasi) {
        this.username = username;
        this.tipeValidasi = tipeValidasi;
        initComponents();
        connectToDatabase();
        centerFrame();
        loadPertanyaanKeamanan();
    }

    private void connectToDatabase() {
        try {
            emf = Persistence.createEntityManagerFactory("PBOpertemuan6PU");
            em = emf.createEntityManager();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal terhubung ke database: " + e.getMessage());
        }
    }

    private void centerFrame() {
        setLocationRelativeTo(null);
    }

    private void loadPertanyaanKeamanan() {
        try {
            String query = "SELECT s FROM SecurityQuestion s WHERE s.username = :username";
            TypedQuery<SecurityQuestion> typedQuery = em.createQuery(query, SecurityQuestion.class);
            typedQuery.setParameter("username", username);

            SecurityQuestion securityQuestion = typedQuery.getSingleResult();

            if (securityQuestion != null) {
                // Tampilkan pertanyaan keamanan
                lblPertanyaan.setText("<html><body style='width: 400px; padding: 10px;'>" + 
                    securityQuestion.getQuestion() + "</body></html>");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Tidak ditemukan pertanyaan keamanan untuk username ini.");
                dispose();
            }

        } catch (NoResultException e) {
            JOptionPane.showMessageDialog(this, 
                "Tidak ditemukan pertanyaan keamanan untuk username: " + username);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error memuat pertanyaan keamanan: " + e.getMessage());
            dispose();
        }
    }

    private void validasiJawaban() {
        String jawaban = txtJawaban.getText().trim();

        if (jawaban.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Jawaban harus diisi!");
            return;
        }

        try {
            String query = "SELECT s FROM SecurityQuestion s WHERE s.username = :username";
            TypedQuery<SecurityQuestion> typedQuery = em.createQuery(query, SecurityQuestion.class);
            typedQuery.setParameter("username", username);

            SecurityQuestion securityQuestion = typedQuery.getSingleResult();

            if (securityQuestion != null) {
                boolean isValid = SecurityUtil.verifyAnswer(jawaban, securityQuestion.getAnswerHash());

                if (isValid) {
                    validationSuccess = true;
                    JOptionPane.showMessageDialog(this, 
                        "Validasi berhasil! Identitas Anda telah dikonfirmasi.");

                    // Arahkan ke form yang sesuai berdasarkan tipe validasi
                    if ("USERNAME".equals(tipeValidasi)) {
                        // Buka form ubah username
                        UbahUsername ubahUsernameForm = new UbahUsername(username);
                        ubahUsernameForm.setVisible(true);
                    } else if ("PASSWORD".equals(tipeValidasi)) {
                        // Buka form ubah password
                        LupaPassword lupaPasswordForm = new LupaPassword(username);
                        lupaPasswordForm.setVisible(true);
                    }
                    
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Jawaban salah! Silakan coba lagi.");
                    txtJawaban.setText("");
                    txtJawaban.requestFocus();
                }
            }

        } catch (NoResultException e) {
            JOptionPane.showMessageDialog(this, 
                "Tidak ditemukan data pertanyaan keamanan untuk username: " + username);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error validasi: " + e.getMessage());
        }
    }

    public boolean isValidationSuccess() {
        return validationSuccess;
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        // Komponen GUI - tambahkan komponen yang diperlukan
        lblPertanyaan = new JLabel();
        txtJawaban = new JTextField();
        btnSubmit = new JButton("Submit");
        btnBatal = new JButton("Batal");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Validasi Pertanyaan Keamanan");

        // Setup layout dan komponen
        Container container = getContentPane();
        container.setLayout(new BorderLayout(10, 10));

        // Panel untuk pertanyaan
        JPanel panelPertanyaan = new JPanel(new BorderLayout());
        panelPertanyaan.setBorder(BorderFactory.createTitledBorder("Pertanyaan Keamanan"));
        lblPertanyaan.setText("Memuat pertanyaan...");
        panelPertanyaan.add(lblPertanyaan, BorderLayout.CENTER);
        
        // Panel untuk jawaban
        JPanel panelJawaban = new JPanel(new BorderLayout());
        panelJawaban.setBorder(BorderFactory.createTitledBorder("Jawaban"));
        panelJawaban.add(txtJawaban, BorderLayout.CENTER);

        // Panel untuk tombol
        JPanel panelTombol = new JPanel(new FlowLayout());
        panelTombol.add(btnSubmit);
        panelTombol.add(btnBatal);

        // Tambahkan semua panel ke container
        container.add(panelPertanyaan, BorderLayout.NORTH);
        container.add(panelJawaban, BorderLayout.CENTER);
        container.add(panelTombol, BorderLayout.SOUTH);

        // Event listeners
        btnSubmit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validasiJawaban();
            }
        });

        btnBatal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                // Kembali ke login
                Login loginForm = new Login();
                loginForm.setVisible(true);
            }
        });

        // Tekan Enter di textfield untuk submit
        txtJawaban.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validasiJawaban();
            }
        });

        pack();
        setSize(500, 300);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}