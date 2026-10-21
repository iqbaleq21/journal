# Desain UI — jOURnal

Mockup visual: https://claude.ai/artifact/3RnmmuEQCbK2q4LJ6kY63i (canvas berisi 7 layar + panduan gaya).

## Arah desain

Mengikuti **Material 3 Expressive** (bahasa desain Android terbaru): bentuk besar yang membulat, warna pastel yang hidup, tipografi tebal yang ekspresif, dan gerak yang terasa memantul. Catatan ditampilkan sebagai kartu berwarna seperti kertas tempel, disusun model **bento / masonry** dua kolom.

Prinsip:
- Konten (isi catatan) adalah tokoh utama. Chrome UI tipis dan netral.
- Satu aksi utama per layar (FAB "Tulis", "Simpan", "Pasang widget").
- Mendukung tema gelap dari awal (lihat widget gelap di mockup).
- Aksesibel: kontras teks ≥ 4.5:1, target sentuh ≥ 44 dp (48 dp di Android), semua ikon-tombol punya `contentDescription`.

## Token

### Warna (tema terang)
| Token | Hex | Pemakaian |
|---|---|---|
| `primary` | `#4F3BD1` | FAB, tombol utama, aksen wordmark |
| `onPrimary` | `#FFFFFF` | Teks di atas primary |
| `primaryContainer` | `#E6E0FF` | Kartu ringkasan grup, chip label grup |
| `onPrimaryContainer` | `#2F2580` | Teks di atas primaryContainer |
| `background` | `#F6F5FA` | Latar layar |
| `surface` | `#FFFFFF` | Kartu daftar, kartu putih |
| `surfaceVariant` | `#ECEAF3` | Tombol ikon bulat, tombol nonaktif |
| `outline` | `#DCD9E6` | Garis chip yang tidak dipilih |
| `onSurface` (ink) | `#1C1B22` | Teks utama, chip terpilih |
| `onSurfaceVariant` | `#5E5B6B` | Teks sekunder |
| `error` | `#B3261E` | Hapus, keluar grup |

### Warna catatan
Disimpan di kolom `notes.color` sebagai kunci, bukan hex, agar bisa diganti per tema:

| Kunci | Terang | Teks sekunder di atasnya |
|---|---|---|
| `butter` | `#FFE7A3` | `#5A4A20` |
| `mint` | `#C9F0D8` | `#2F4A3A` |
| `sky` | `#CDE2FF` | `#3E4A60` |
| `peach` | `#FFD3BF` | `#4D4040` |
| `lilac` | `#E2D8FF` | `#4A4566` |
| `plain` (default) | `#FFFFFF` | `#5E5B6B` |

Versi gelap tiap warna ditentukan saat implementasi tema gelap (turunkan lightness, pertahankan hue).

### Tipografi
| Peran | Font | Ukuran / berat |
|---|---|---|
| Display (wordmark, judul layar) | **Bricolage Grotesque** | 30–54 sp, 700–800 |
| Judul kartu / isi catatan di widget | Bricolage Grotesque | 18–26 sp, 600–700 |
| Teks UI & isi catatan di daftar | **Figtree** | 15–17 sp, 500–700 |
| Label & meta | Figtree | 12–14 sp, 600 |

Kedua font dari Google Fonts. Bundel sebagai resource (`res/font`), bukan downloadable font, agar widget tetap memakai font yang benar.

### Bentuk & spasi
| Elemen | Radius |
|---|---|
| Kartu catatan | 24 dp |
| Kartu besar / widget | 28–32 dp |
| FAB | 22 dp (rounded square) |
| Tombol & chip | pill (`50%`) |
| Input kode | 16 dp |

Spasi kelipatan 4 dp. Padding layar 20 dp, jarak antar kartu 12 dp.

### Wordmark
`j` + **`OUR`** (warna primary) + `nal`, Bricolage Grotesque 700–800.

## Daftar layar

| # | Layar | Catatan |
|---|---|---|
| 1 | Kenalan | Ilustrasi tumpukan catatan + bentuk ekspresif, input nama & email, tombol "Mulai" (tanpa login). Bila sesi dicabut server: isian lama terisi + teks penjelasan berwarna error |
| 2 | Mulai | Muncul jika belum punya grup: kartu "Buat grup baru" & "Gabung dengan kode" (6 kotak) |
| 3 | Beranda | Chip pemilih grup, kartu ringkasan grup dengan tumpukan avatar anggota (tap → Detail grup), tombol Pengaturan di kanan atas, grid masonry catatan, badge "Di widget" (M3), FAB "Tulis" |
| 4 | Editor | Kartu catatan besar sesuai warna, counter 0/500, pemilih warna (6 lingkaran 48 dp, ring 3 dp untuk yang terpilih; disembunyikan saat keyboard terbuka agar kartu memakai seluruh ruang). Tombol pengaturan di top bar membuka bottom sheet "Pengaturan catatan": segmented button Teks / Checklist, switch "Anggota lain boleh mengedit" (hanya penulis), hapus dengan konfirmasi (hanya penulis). Checklist: baris checkbox + teks + tombol hapus item, "Tambah item", Enter di item terakhir menambah item, item tercentang dicoret. Catatan orang lain yang tidak dibagikan tampil read-only. |
| 5 | Detail grup | Kode undangan (bagikan / buat ulang), daftar anggota, keluar grup. Aksi pemilik (ganti nama, buat ulang kode, keluarkan anggota lewat menu ⋮) hanya tampil untuk pemilik; semua aksi berisiko memakai dialog konfirmasi. |
| 5b | Pengaturan | Tidak ada di mockup; mengikuti gaya kartu yang sama: Profil (nama, email, peringatan identitas), Tampilan (segmented: Ikuti sistem / Terang / Gelap), Notifikasi per grup (switch), Optimasi baterai (status + tombol ke pengaturan sistem), versi app. |
| 6 | Pilih catatan widget | Preview widget di atas, pilih grup → radio: "Catatan terbaru" (otomatis, paling atas) atau catatan tertentu, "Pasang widget" / "Ganti catatan" |
| 7 | Widget | Ukuran 4×2 dan 2×2, varian terang/gelap, state "catatan dihapus". Batasan Glance: font memakai sans-serif sistem (Glance tidak mendukung font dari `res/font`), sudut membulat hanya di Android 12+, dan waktu relatif baru diperbarui saat widget di-render ulang. |

## Gerak (motion)
- Gunakan spring dari Material 3 Expressive (`MotionScheme.expressive()`) untuk transisi layar dan FAB.
- Kartu baru masuk dengan fade + scale ringan; hapus dengan collapse.
- Hormati pengaturan "Kurangi animasi" sistem.
