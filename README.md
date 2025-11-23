# PBO14
Pengumpulan hasil Netbeans tugas PBO Pertemuan keempat belas

# Laporan Praktik Pertemuan Ke-14

Mata Kuliah: Pemrograman Berorientasi Objek
Dosen Pengampu: Bayu Adhi Nugroho, Ph.D.
Disusun oleh: Fernando Seroy (09040624087)

# 1. Perubahan Database

Menggunakan database PostgreSQL yang sama, namun menambahkan tabel baru security_question.

Setelah menjalankan query, tabel baru akan muncul saat di-refresh.

Pengisian tabel dilakukan dengan urutan:

Isi tabel perangkat_elektronik (tabel utama).

Isi detail_perangkat melalui aplikasi.

Kosongkan tabel login lama, lalu isi ulang login beserta security question melalui aplikasi Java.

# 2. Penambahan Coding / Fitur Baru
# 2.1 Penambahan Class Baru

Ditambahkan pada package login:

SecurityQuestion.java

SecurityUtil.java

ValidasiPertanyaanKeamanan.java

# 2.2 Penambahan di Package Dialogue

File pendukung baru:

CSVHandler

JasperReportHandler

MasterDataCSV

Perubahan utama:

Menambahkan button Download untuk mengunduh data tabel ke CSV.

Mengubah fungsi Upload agar mampu mengimpor kembali file CSV tersebut.

# 3. Output Program
# 3.1 Alur Program

Program berjalan dari Main, menampilkan menu Login terlebih dahulu.

# 3.2 Fitur Registrasi

Tersedia opsi “Belum punya akun? Klik di sini”

Form registrasi sudah menampilkan kolom untuk pertanyaan keamanan yang bisa diinput oleh user.

# 3.3 Fitur Pemulihan Akun

Lupa Password → memanfaatkan pertanyaan keamanan untuk reset.

Lupa Username → membutuhkan password untuk melacak username.

Catatan: kemungkinan duplikasi username & password masih ada karena belum ada validasi unik.

# 3.4 Proses Login

Login berhasil menampilkan menu utama aplikasi.

# 3.5 Cetak Laporan

Fitur cetak file .jasper berfungsi dengan baik.
4. Status

Smua source code dan fitur berjalan dengan lancar.
