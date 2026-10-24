# Desain UI — jOURnal

Mockup visual: https://claude.ai/artifact/3RnmmuEQCbK2q4LJ6kY63i (canvas berisi 7 layar + panduan gaya).

## Arah desain

Dipilih 2026-10-21 dari kanvas opsi UI (https://claude.ai/artifact/WCX2YhCRkqAzzQnBgNmR5k):
- **Tema terang = opsi D "Berani"** (neo-brutalism ringan): latar abu terang, tinta hitam, kuning sebagai aksen, kartu bergaris hitam 2 dp dengan bayangan padat 4 dp ke kanan-bawah, warna catatan cerah.
- **Tema gelap = opsi C "Malam"**: hitam pekat, satu aksen lime, kartu bergaris tipis 1 dp tanpa bayangan, warna catatan berupa tint gelap.

Tata letak dan tipografi (Bricolage Grotesque + Figtree) sama di kedua tema; yang berbeda hanya token warna dan `CardStyle`.

Prinsip:
- Konten (isi catatan) adalah tokoh utama. Chrome UI tipis dan netral.
- Satu aksi utama per layar (FAB "Tulis", "Simpan", "Pasang widget").
- Aksesibel: kontras teks ≥ 4.5:1, target sentuh ≥ 48 dp, semua ikon-tombol punya `contentDescription`.

## Token

### Warna
| Token | Terang (D) | Gelap (C) | Pemakaian |
|---|---|---|---|
| `primary` / `onPrimary` | `#111111` / `#FFFFFF` | `#D4FF3F` / `#0E0E10` | Tombol utama (Simpan), chip terpilih |
| `primaryContainer` / `on…` | `#FFD84D` / `#111111` | `#D4FF3F` / `#0E0E10` | FAB, kartu ringkasan grup, sorotan "OUR" di wordmark |
| `background` | `#F4F4F0` | `#0E0E10` | Latar layar |
| `surface` / `onSurface` | `#FFFFFF` / `#111111` | `#17171A` / `#F2F2F3` | Kartu, teks utama |
| `surfaceVariant` / `onSurfaceVariant` | `#E8E8E2` / `#444444` | `#2A2A30` / `#A0A0A8` | Tombol ikon, teks sekunder |
| `secondaryContainer` | `#E8E8E2` | `#2A2A30` | Tombol ikon tonal, segmen terpilih |
| `surfaceContainer*` | putih (tertinggi `#E8E8E2`) | `#0E0E10`–`#2A2A30` | Bottom sheet & dialog (wajib diisi agar tidak ungu bawaan Material) |
| `outline` | `#111111` | `#6E6E76` | Tepi chip, input, thumb switch mati |
| `error` | `#B3261E` | `#FF8A80` | Hapus, keluar grup |

### Garis & bayangan kartu (`CardStyle`)
| | Terang | Gelap |
|---|---|---|
| Garis | 2 dp `#111111` | 1 dp `#2A2A30` |
| Bayangan | padat 4 dp kanan-bawah, `#111111` (`Modifier.hardShadow`) | tidak ada |

Dipakai di kartu catatan, kartu Editor, kartu ringkasan grup, kartu Mulai, kartu Pengaturan/Detail grup/Pilih widget, dan FAB.

### Warna catatan
Disimpan di kolom `notes.color` sebagai kunci, bukan hex, agar bisa diganti per tema (widget memakai token yang sama):

| Kunci | Terang | Teks sekunder | Gelap | Teks sekunder |
|---|---|---|---|---|
| `butter` | `#FFD84D` | `#4A3D00` | `#2E2914` | `#E8DDB8` |
| `mint` | `#7CE0A3` | `#1E4A30` | `#16291E` | `#C3E3CF` |
| `sky` | `#8CB8FF` | `#1F3A66` | `#172233` | `#C6D6F0` |
| `peach` | `#FF9F7A` | `#5A2A14` | `#301F18` | `#F0CDBE` |
| `lilac` | `#C3A6FF` | `#3D2A70` | `#231D36` | `#D9D0F5` |
| `plain` (default) | `#FFFFFF` | `#444444` | `#17171A` | `#A0A0A8` |

### Tipografi
| Peran | Font | Ukuran / berat |
|---|---|---|
| Display (wordmark, judul layar) | **Bricolage Grotesque** | 30–54 sp, 700–800 |
| Judul kartu / isi catatan di widget | Bricolage Grotesque | 18–26 sp, 600–700 |

Judul catatan (US-20): di kartu Beranda `titleLarge` maks 2 baris di atas isi (isi lalu selalu `bodyMedium`); di widget 1 baris tebal, isi 4 sp lebih kecil. Mockup: board "Judul catatan" di kanvas redesain.
| Teks UI & isi catatan di daftar | **Figtree** | 15–17 sp, 500–700 |
| Label & meta | Figtree | 12–14 sp, 600 |

Kedua font dari Google Fonts. Bundel sebagai resource (`res/font`), bukan downloadable font, agar widget tetap memakai font yang benar.

### Bentuk & spasi
| Elemen | Radius |
|---|---|
| Kartu catatan | 18 dp |
| Kartu besar / widget | 24 dp |
| FAB | 20 dp (rounded square) |
| Tombol & chip | pill (`50%`) |
| Input | 14 dp |

Spasi kelipatan 4 dp. Padding layar 20 dp, jarak antar kartu 12 dp.

### Wordmark
`j` + **`OUR`** (latar `primaryContainer`: kuning/lime) + `nal`, Bricolage Grotesque 700–800.

## Daftar layar

| # | Layar | Catatan |
|---|---|---|
| 1 | Kenalan | Ilustrasi tumpukan catatan + bentuk ekspresif, input nama & email, tombol "Mulai" (tanpa login). Bila sesi dicabut server: isian lama terisi + teks penjelasan berwarna error |
| 2 | Mulai | Muncul jika belum punya grup: kartu "Buat grup baru" & "Gabung dengan kode" (6 kotak) |
| 3 | Beranda | Chip pemilih grup, kartu ringkasan grup dengan tumpukan avatar anggota (tap → Detail grup), tombol Pengaturan di kanan atas, grid masonry catatan, badge "Di widget" (M3), FAB "Tulis" |
| 4 | Editor | Top bar: kembali (menyimpan dulu) dan tombol pengaturan catatan; tidak ada tombol Simpan (simpan otomatis). Kartu catatan besar sesuai warna; kolom judul opsional (`headlineLarge`, placeholder "Judul", fokus pertama di catatan baru) di atas isi (`bodyLarge`); counter 0/500 hanya untuk isi, pemilih warna (6 lingkaran 48 dp, ring 3 dp untuk yang terpilih; disembunyikan saat keyboard terbuka agar kartu memakai seluruh ruang). Tombol pengaturan di top bar membuka bottom sheet "Pengaturan catatan": segmented button Teks / Checklist, switch "Anggota lain boleh mengedit" (hanya penulis), hapus dengan konfirmasi (hanya penulis). Checklist: baris checkbox + teks + tombol hapus item, "Tambah item", Enter di item terakhir menambah item, item tercentang dicoret. Catatan orang lain yang tidak dibagikan tampil read-only. |
| 5 | Detail grup | Kode undangan (bagikan / buat ulang), daftar anggota, keluar grup. Aksi pemilik (ganti nama, buat ulang kode, keluarkan anggota lewat menu ⋮) hanya tampil untuk pemilik; semua aksi berisiko memakai dialog konfirmasi. |
| 5b | Pengaturan | Tidak ada di mockup; mengikuti gaya kartu yang sama: Profil (nama, email, peringatan identitas), Tampilan (segmented: Ikuti sistem / Terang / Gelap), Bahasa (segmented: Indonesia / English; nama bahasa tidak diterjemahkan), Notifikasi per grup (switch), Optimasi baterai (status + tombol ke pengaturan sistem), versi app. |
| 6 | Pilih catatan widget | Preview widget di atas, pilih grup → radio: "Catatan terbaru" (otomatis, paling atas) atau catatan tertentu, "Pasang widget" / "Ganti catatan" |
| 7 | Widget | Ukuran 4×2 dan 2×2, varian terang/gelap, state "catatan dihapus". Batasan Glance: font memakai sans-serif sistem (Glance tidak mendukung font dari `res/font`), sudut membulat hanya di Android 12+, dan waktu relatif baru diperbarui saat widget di-render ulang. |

## Gerak (motion)
- Gunakan spring dari Material 3 Expressive (`MotionScheme.expressive()`) untuk transisi layar dan FAB.
- Kartu baru masuk dengan fade + scale ringan; hapus dengan collapse.
- Hormati pengaturan "Kurangi animasi" sistem.
