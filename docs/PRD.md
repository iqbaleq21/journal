# PRD — jOURnal

| | |
|---|---|
| **Nama** | **jOURnal**: "OUR" ditulis kapital untuk menekankan catatan bersama |
| **Status** | v0.6, M0–M7 selesai |
| **Tanggal** | 2026-10-20 |
| **Platform** | Android saja |
| **Package ID** | `com.iqbal.journal` |
| **Pemilik** | Iqbal |

Dokumen ini adalah sumber kebenaran untuk scope dan arsitektur. Kalau implementasi menyimpang dari dokumen ini, perbarui dokumen ini dulu.

**Riwayat perubahan**
- v0.6: Catatan mode checklist (US-17), berbagi akses edit per catatan (US-18), dan pilihan tema terang/gelap di Pengaturan (US-19).
- v0.5: Tidak ada logout di Pengaturan: akun anonim yang di-logout tidak bisa dipulihkan. Diganti peringatan bahwa identitas terikat ke HP ini (§5.1, §9).
- v0.4: Login Google dihapus. Diganti profil sederhana (**nama + email**) di atas **Supabase Anonymous Sign-In** (§5.1).
- v0.3: Nama **jOURnal**. Catatan sendiri boleh dipilih di widget. Ditambah aturan retensi data (§5.6).
- v0.2 — Dari "tepat 2 orang" menjadi **grup beberapa orang**; widget menampilkan **1 catatan yang dipilih**; login **hanya Google**.
- v0.1 — Draft awal.

---

## 1. Ringkasan

Aplikasi catatan untuk **sekelompok kecil orang yang saling terhubung** (pasangan, keluarga, sahabat, tim kecil). Catatan yang ditulis di sebuah grup bisa ditampilkan sebagai **widget di home screen** setiap anggota. Setiap widget menampilkan **satu catatan yang dipilih** pengguna, dan ter-update otomatis saat catatan itu berubah, tanpa perlu membuka aplikasi.

## 2. Masalah & Tujuan

**Masalah:** Pesan di chat cepat "tenggelam". Tidak ada cara ringan untuk menempelkan catatan bersama yang *selalu terlihat* di HP orang-orang terdekat (mis. "Kode wifi rumah", "Jadwal jemput anak", "Aku sayang kamu").

**Tujuan:**
1. Catatan pilihan selalu terlihat di home screen lewat widget.
2. Perubahan sampai ke widget anggota lain dalam hitungan detik (target < 10 detik saat HP online).
3. Biaya operasional Rp0 (free tier).
4. Ringan: aplikasi native, startup cepat, hemat baterai.

**Bukan tujuan (out of scope v1):**
- iOS / web.
- Chat realtime, media (foto/voice), end-to-end encryption.
- Login dengan password, akun Google, atau verifikasi email (bisa ditambahkan nanti, lihat §5.1).
- Rilis ke Play Store (v1 dibagikan via APK).

## 3. Pengguna & Konsep

- **Pengguna**: orang yang mengisi nama & email di HP-nya. Identitasnya terikat ke instalasi aplikasi (§5.1).
- **Grup**: kumpulan **2–10 anggota** yang berbagi catatan. Satu pengguna bisa ikut **beberapa grup** (mis. grup "Rumah" dan grup "Kita Berdua").
- **Pemilik grup (owner)**: pembuat grup. Bisa mengganti nama grup, membuat ulang kode undangan, dan mengeluarkan anggota.
- **Catatan**: teks atau checklist milik satu grup, ditulis oleh satu anggota. Penulis bisa mengizinkan anggota lain mengeditnya.
- **Widget**: setiap widget yang dipasang menampilkan satu catatan yang dipilih. Pengguna bisa memasang beberapa widget dengan catatan berbeda.

## 4. User Stories

| ID | Sebagai pengguna, saya ingin… | Prioritas |
|---|---|---|
| US-1 | mulai memakai app cukup dengan mengisi nama dan email | P0 |
| US-2 | membuat grup dan mendapat kode undangan untuk dibagikan | P0 |
| US-3 | bergabung ke grup dengan memasukkan kode undangan | P0 |
| US-4 | menulis catatan baru di sebuah grup | P0 |
| US-5 | mengedit dan menghapus catatan milik saya | P0 |
| US-6 | melihat semua catatan di sebuah grup | P0 |
| US-7 | memasang widget dan **memilih catatan** yang ditampilkan | P0 |
| US-8 | widget ter-update otomatis saat catatan yang ditampilkan diubah | P0 |
| US-9 | mengganti catatan yang ditampilkan widget tanpa memasang ulang | P0 |
| US-10 | berpindah antar grup yang saya ikuti | P0 |
| US-11 | mendapat notifikasi saat ada catatan baru di grup saya | P1 |
| US-12 | tetap bisa membuka aplikasi dan menulis saat offline (tersinkron saat online) | P1 |
| US-13 | keluar dari grup | P1 |
| US-14 | (owner) mengeluarkan anggota, mengganti nama grup, membuat ulang kode undangan | P1 |
| US-15 | memilih warna latar catatan (ikut tampil di widget) | P2 |
| US-16 | mode widget "catatan terbaru di grup" (otomatis berganti) | P2 |
| US-17 | menulis catatan sebagai checklist yang bisa dicentang, bukan hanya teks bebas | P2 |
| US-18 | mengizinkan anggota lain mengedit catatan saya, dan mencabutnya lagi | P2 |
| US-19 | memilih tema terang, gelap, atau ikut sistem | P2 |

## 5. Kebutuhan Fungsional

### 5.1 Identitas (tanpa login)
- Tidak ada login. Saat pertama dibuka, app menampilkan layar **Kenalan**: isi **nama** (wajib, 1–40 karakter) dan **email** (wajib, format valid, **tidak diverifikasi** di v1).
- Di belakang layar app memanggil **Supabase Anonymous Sign-In** (`signInAnonymously`), sehingga setiap instalasi punya `auth.uid()` sendiri. Ini wajib agar RLS tetap bekerja; tanpa identitas, siapa pun yang memegang anon key bisa membaca semua data.
- Setelah sign-in anonim, app menyimpan nama & email ke `profiles` (insert milik sendiri, dijaga RLS). Keduanya bisa diubah di Pengaturan.
- Sesi disimpan; app tidak pernah meminta login lagi. Tidak ada logout (akun anonim tidak bisa dipulihkan setelah logout).
- Bila server mencabut sesi (mis. akun dihapus), app kembali ke Kenalan dengan nama & email lama terisi dan penjelasan singkat; "Mulai" membuat akun anonim baru, lalu pengguna gabung ulang ke grupnya dengan kode undangan. Semua request ke Supabase menunggu sesi selesai dimuat dari penyimpanan; tanpa sesi, request tidak dikirim (antrean offline tetap disimpan, cache tidak dikosongkan).
- **Konsekuensi**: identitas terikat ke instalasi. Uninstall, hapus data app, atau ganti HP = akun baru. Pengguna harus gabung grup lagi; catatan lamanya tetap ada tetapi tidak bisa diedit oleh akun baru.
- **Jalan upgrade (nanti)**: akun anonim bisa dijadikan permanen dengan menautkan email terverifikasi (`updateUser(email)` + OTP) tanpa kehilangan data. Butuh SMTP sendiri karena email bawaan Supabase sangat dibatasi.
- Setting Supabase: *Authentication → Sign In / Providers → Allow anonymous sign-ins* = aktif.

### 5.2 Grup & Undangan
- Pengguna tanpa grup melihat layar awal: **Buat grup** atau **Gabung dengan kode**.
- Membuat grup: isi nama grup → dapat kode undangan.
- Kode: 6 karakter huruf besar/angka (tanpa karakter ambigu `0 O 1 I L`), **bisa dipakai banyak orang**, berlaku **7 hari**. Owner bisa membuat ulang kode (kode lama langsung tidak berlaku).
- Maksimal **10 anggota** per grup. Kode kedaluwarsa / grup penuh / sudah jadi anggota → pesan error yang jelas.
- Keluar grup: data catatan tetap ada untuk anggota lain. Jika owner keluar, kepemilikan pindah ke anggota paling lama. Jika anggota terakhir keluar, grup diarsipkan.
- Semua perubahan keanggotaan lewat **fungsi RPC Postgres** (§8.2), bukan update tabel langsung.

### 5.3 Catatan
- Isi: teks polos, maks **500 karakter**.
- Mode (`notes.kind`): **teks** atau **checklist** (US-17). Checklist disimpan di `content` sebagai teks, satu item per baris (`[ ] item` / `[x] item`), jadi batas 500 karakter termasuk penandanya. Mengganti mode membawa isinya: tiap baris jadi item, atau sebaliknya. Widget, kartu, dan notifikasi menampilkan ☐/☑; centang hanya lewat app.
- Siapa pun di grup bisa membaca. Hanya **penulis** yang bisa menghapus.
- Berbagi akses (US-18): penulis menyalakan "Anggota lain boleh mengedit" (`notes.shared`) per catatan. Jika aktif, semua anggota grup bisa mengubah isi, warna, mode, dan centang. Menghapus dan mengatur akses tetap hak penulis (dijaga trigger `NOTE_AUTHOR_ONLY`). Edit bersamaan: yang terakhir menang. Edit offline yang tiba setelah akses dicabut ditolak dan diganti versi server.
- Hapus = soft delete (`deleted_at`) agar penghapusan ikut tersinkron ke HP lain.
- Daftar catatan per grup, terbaru di atas, dengan nama & foto penulis.

### 5.4 Widget
- Ukuran: minimal **2×2**, bisa di-resize hingga **4×4**.
- Saat dipasang, muncul **layar konfigurasi**: pilih grup → pilih catatan. Daftar berisi **semua catatan di grup, termasuk milik sendiri**.
- Bisa memasang **beberapa widget**, masing-masing menampilkan catatan berbeda.
- Ganti catatan (US-9): lewat **tahan widget → Atur ulang** (Android 12+, `widgetFeatures="reconfigurable"`) atau tombol kecil di widget yang membuka layar pilih catatan (untuk Android < 12).
- Isi widget: teks catatan, nama penulis, nama grup, waktu relatif ("5 menit lalu"), warna latar (P2).
- Tap widget → buka aplikasi ke catatan tersebut.
- State khusus:
  - Catatan dihapus → "Catatan ini sudah dihapus. Ketuk untuk memilih yang lain."
  - Keluar / dikeluarkan dari grup → "Kamu tidak lagi di grup ini."
  - Profil belum diisi → "Buka jOURnal untuk mulai."
- Pilihan per widget disimpan lokal di Room (`widget_selections`: `appWidgetId → note_id, group_id`), agar lapisan data tahu catatan mana yang dipakai widget (§5.6). `note_id` null = mode "catatan terbaru" (US-16): widget mengikuti catatan yang terakhir diubah di grup itu.
- Widget **hanya membaca data lokal** (Room). Widget tidak pernah melakukan request jaringan sendiri.

### 5.5 Sinkronisasi & Notifikasi
- Saat aplikasi terbuka: berlangganan **Supabase Realtime** pada tabel `notes` untuk grup-grup yang diikuti.
- Saat aplikasi tertutup: setiap perubahan catatan memicu **FCM data message (high priority)** ke **semua anggota grup selain penulis** → app bangun di background → ambil catatan → simpan ke Room → update widget yang menampilkan catatan itu.
- Fallback: **WorkManager** periodik (15 menit, minimum Android) untuk sinkron jika push terlewat.
- Notifikasi sistem (US-11) hanya untuk catatan **baru** (bukan edit); bisa dimatikan per grup di pengaturan.

- Offline (US-12): tulis/edit/hapus langsung tersimpan di Room dan masuk antrean `pending_ops`; dikirim berurutan saat online (langsung, saat app dibuka, atau lewat WorkManager). ID catatan baru dibuat di HP agar kirim ulang tidak menggandakan catatan. Selama ada perubahan tertunda, versi server tidak menimpa catatan itu; perubahan yang ditolak server dibuang dan diganti versi server.

### 5.6 Retensi Data
- **Server** menyimpan semua catatan tanpa batas.
- **HP (Room)** menyimpan **200 catatan terbaru per grup**. Catatan lebih lama diambil dari server lewat "Muat lebih banyak" (paginasi berdasarkan `updated_at`) dan tidak wajib disimpan permanen di Room.
- Pembersihan lokal dijalankan oleh `SyncWorker`. Catatan yang **sedang dipakai widget tidak boleh dihapus** dari Room meskipun di luar 200 terbaru.
- Catatan yang dihapus (soft delete) **dihapus permanen dari server setelah 30 hari**, lewat job harian `pg_cron` (§8.4). HP yang belum sinkron lebih dari 30 hari melakukan sinkron penuh untuk grup tersebut.

## 6. Kebutuhan Non-Fungsional

| Aspek | Target |
|---|---|
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Versi terbaru yang stabil |
| Cold start | < 1 detik di HP kelas menengah |
| Latensi update widget | < 10 detik (online, push terkirim) |
| Ukuran APK | < 15 MB |
| Baterai | Tidak ada service yang berjalan terus; hanya push + WorkManager |
| Keamanan | Semua akses data dibatasi **Row Level Security** Supabase |
| Biaya | Rp0 (Supabase Free + FCM gratis) |

## 7. Arsitektur

### 7.1 Tech Stack

| Lapisan | Pilihan | Alasan |
|---|---|---|
| Bahasa | **Kotlin** | Native, performa terbaik di Android |
| UI | **Jetpack Compose** + Material 3 | Standar modern Android |
| Widget | **Jetpack Glance** | API widget berbasis Compose |
| DB lokal | **Room** | Sumber data untuk UI & widget, mendukung offline |
| Backend client | **supabase-kt** (Auth, Postgrest, Realtime, Functions) + Ktor | SDK Kotlin untuk Supabase |
| DI | **Hilt** | Standar Android |
| Background | **WorkManager** | Sinkron periodik & job dari push |
| Push | **Firebase Cloud Messaging** (hanya FCM, tanpa Firestore) | Satu-satunya cara push andal di Android |
| Backend | **Supabase**: Postgres, Auth, Realtime, Edge Functions (Deno/TS) | Free tier cukup, SQL + RLS |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) | |

> Project Firebase dipakai **hanya untuk FCM**, tetap di paket gratis (Spark). Push dikirim dari Supabase Edge Function memakai FCM HTTP v1 API + service account.

### 7.2 Diagram

```
 HP A (penulis)                    Supabase                         HP B, C, ... (anggota lain)
┌──────────────┐  insert/update ┌──────────────────┐
│ App (Compose)│ ─────────────► │ Postgres (notes) │
│   Room       │                │   │ DB Webhook    │
└──────────────┘                │   ▼              │
                                │ Edge Function    │  FCM data msg   ┌──────────────────┐
                                │ "notify-members" │ ──────────────► │ FcmService       │
                                └──────────────────┘ (ke tiap device)│   ▼ fetch note   │
                                         ▲                           │ Room             │
                                         └────────── select ──────── │   ▼              │
                                                                     │ update widget    │
                                                                     │ yg menampilkan   │
                                                                     │ note tsb         │
                                                                     └──────────────────┘
```

### 7.3 Struktur Modul Aplikasi

Satu modul `:app` dulu (cukup untuk skala ini):

```
app/src/main/java/com/iqbal/journal/
├── data/
│   ├── local/        # Room: entity, dao, database; DataStore pilihan widget
│   ├── remote/       # Supabase client, DTO
│   └── repository/   # AuthRepository, GroupRepository, NoteRepository
├── domain/           # model murni (Profile, Group, Member, Note)
├── ui/
│   ├── auth/
│   ├── groups/       # daftar grup, buat/gabung, detail & anggota
│   ├── notes/        # daftar & editor
│   ├── settings/
│   └── theme/
├── widget/           # GlanceAppWidget, receiver, layar konfigurasi (pilih catatan)
├── sync/             # SyncWorker, FcmService
└── di/               # modul Hilt
```

Pola: **MVVM + Repository**, data satu arah: Supabase → Repository → Room → (UI | Widget). UI dan widget **hanya membaca Room**.

## 8. Model Data (Supabase / Postgres)

```sql
create table profiles (            -- diisi app setelah anonymous sign-in
  id           uuid primary key references auth.users on delete cascade,
  display_name text not null check (char_length(display_name) between 1 and 40),
  email        text not null,       -- belum diverifikasi di v1
  avatar_url   text,
  updated_at   timestamptz not null default now()
);

create table devices (             -- satu user bisa punya beberapa HP
  fcm_token    text primary key,
  user_id      uuid not null references auth.users on delete cascade,
  updated_at   timestamptz not null default now()
);

create table groups (
  id                 uuid primary key default gen_random_uuid(),
  name               text not null check (char_length(name) between 1 and 50),
  owner_id           uuid not null references auth.users,
  invite_code        text unique,
  invite_expires_at  timestamptz,
  archived_at        timestamptz,
  created_at         timestamptz not null default now()
);

create table group_members (
  group_id   uuid not null references groups(id) on delete cascade,
  user_id    uuid not null references auth.users on delete cascade,
  notify     boolean not null default true,    -- notifikasi per grup
  joined_at  timestamptz not null default now(),
  primary key (group_id, user_id)
);

create table notes (
  id          uuid primary key default gen_random_uuid(),
  group_id    uuid not null references groups(id),
  author_id   uuid not null references auth.users,
  content     text not null check (char_length(content) between 1 and 500),
  color       text,                             -- P2
  kind        text not null default 'text',     -- 'text' | 'checklist'
  shared      boolean not null default false,   -- anggota lain boleh mengedit
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  deleted_at  timestamptz
);
create index on notes (group_id, updated_at desc);
```

### 8.1 Aturan RLS (ringkas)
- Helper `is_member(group_id uuid) returns boolean` (`security definer`) untuk dipakai di policy.
- `profiles`: `select` profil sendiri dan profil orang yang satu grup (nama & email terlihat oleh sesama anggota); `insert`/`update` hanya profil sendiri.
- `devices`: hanya baris milik sendiri (insert / update / delete).
- `groups`: `select` jika anggota; perubahan hanya via RPC.
- `group_members`: `select` jika anggota grup tsb; `update` kolom `notify` milik sendiri; insert/delete hanya via RPC.
- `notes`: `select` jika `is_member(group_id)` dan grup belum diarsip; `insert` jika `author_id = auth.uid()` dan anggota; `update` jika `author_id = auth.uid()` atau (`shared` dan anggota). Trigger menolak non-penulis yang mengubah `shared` atau `deleted_at`.

### 8.2 Fungsi RPC (`security definer`)
| Fungsi | Perilaku |
|---|---|
| `create_group(name text) → (group_id, invite_code)` | Buat grup, owner = pemanggil, tambahkan sebagai anggota. |
| `join_group(code text) → group_id` | Validasi kode valid, belum kedaluwarsa, anggota < 10, belum jadi anggota. |
| `leave_group(group_id uuid)` | Hapus keanggotaan; pindahkan owner jika perlu; arsipkan jika kosong. |
| `remove_member(group_id uuid, user_id uuid)` | Hanya owner. |
| `regenerate_invite(group_id uuid) → invite_code` | Hanya owner. Kode lama tidak berlaku. |
| `rename_group(group_id uuid, name text)` | Hanya owner. |

### 8.3 Edge Function `notify-members`
- Dipicu trigger `notes_notify_members` (`after insert or update` pada `notes`) yang memanggil fungsi lewat `pg_net`. URL fungsi dan secret webhook disimpan di **Vault** (`notify_members_url`, `notify_members_secret`), bukan di repo; fungsi memeriksa header `x-webhook-secret` dan di-deploy dengan `verify_jwt = false`.
- Ambil semua `devices.fcm_token` milik anggota grup **selain pengubah** (field `author_id` di payload berisi `auth.uid()` pengubah, agar penulis ikut dikabari saat catatannya diedit anggota lain).
- Kirim FCM HTTP v1 **data message** priority `high`: `{ "type": "note_changed", "note_id": "...", "group_id": "...", "is_new": true|false }`.
- Hapus token dari `devices` jika FCM membalas token tidak valid (`UNREGISTERED`).
- Secret: `FCM_SERVICE_ACCOUNT_JSON`, `FCM_PROJECT_ID`, `NOTIFY_WEBHOOK_SECRET` (Supabase secrets; yang terakhir sama dengan isi Vault `notify_members_secret`).

### 8.4 Job Terjadwal (`pg_cron`)
```sql
-- setiap hari 03:00 UTC: hapus permanen catatan yang sudah di-soft-delete > 30 hari
select cron.schedule('purge-deleted-notes', '0 3 * * *',
  $$ delete from notes where deleted_at < now() - interval '30 days' $$);
```

## 9. Tampilan (MVP)

1. **Kenalan**: isi nama & email, tombol "Mulai".
2. **Awal (tanpa grup)**: "Buat grup" / "Gabung dengan kode".
3. **Beranda**: pemilih grup di atas (dropdown / tab), daftar catatan grup, FAB "Tulis".
4. **Editor**: textarea, counter 0/500, simpan / hapus.
5. **Detail grup**: nama, daftar anggota, kode undangan + share, keluar grup; aksi owner.
6. **Pengaturan**: ubah nama & email, tema (ikut sistem / terang / gelap; widget tetap mengikuti tema sistem), notifikasi per grup, tips optimasi baterai, peringatan identitas terikat ke HP ini. Tanpa logout (lihat riwayat v0.5).
7. **Konfigurasi widget**: pilih grup → pilih catatan.
8. **Widget**: satu catatan terpilih.

## 10. Rencana Rilis (Milestone)

| # | Milestone | Isi | Selesai jika | Status |
|---|---|---|---|---|
| M0 | Setup | Project Android, Supabase project, Firebase (FCM), migrasi SQL + RLS + RPC | App kosong build, migrasi jalan | ✅ 2026-10-18 |
| M1 | Auth & Grup | US-1, US-2, US-3, US-10 | 3 akun bisa bergabung ke satu grup | ✅ 2026-10-18 (diuji di emulator: 3 akun di satu grup) |
| M2 | Catatan | US-4, US-5, US-6, Room + Realtime | Catatan muncul di semua HP anggota saat app terbuka | ✅ 2026-10-18 (Realtime diuji: tambah/edit/hapus dari akun lain tampil < 2 detik). "Muat lebih banyak" (§5.6) belum dibuat. |
| M3 | Widget | US-7, US-9 | Widget menampilkan catatan pilihan dari Room; bisa diganti | ✅ 2026-10-18 (pasang, Atur ulang, tap membuka catatan, state dihapus; update saat app terbuka). Update saat app tertutup menyusul di M4. |
| M4 | Push & Sinkron background | US-8, US-11, Edge Function, WorkManager | Widget HP lain update < 10 detik saat app tertutup | ✅ 2026-10-18 (proses app dimatikan: widget berubah ±5 detik setelah edit; notifikasi catatan baru tampil) |
| M5 | Polish | US-12, US-13, US-14, state kosong/error, ikon | APK release ditandatangani & dibagikan | ✅ 2026-10-18 (APK release 4,1 MB, R8 aktif, diuji di emulator: Kenalan, gabung, tulis, Realtime, push saat app mati). Membagikan APK ke anggota dilakukan pemilik. |
| M6 | Nice to have | US-15, US-16 | — | ✅ 2026-10-18 (warna tersimpan di server & widget; widget mode "terbaru" berganti ±4 dtk saat app tertutup) |
| M7 | Checklist, berbagi, tema | US-17, US-18, US-19 | — | ✅ 2026-10-20 (diuji di emulator + REST di cloud: checklist, berbagi/cabut akses langsung berlaku, anggota tidak bisa hapus, tema gelap) |

## 11. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| Supabase Free **di-pause otomatis** setelah 7 hari tanpa aktivitas | Pemakaian harian biasanya cukup; opsional cron ping mingguan (mis. GitHub Actions). |
| FCM tertunda karena Doze / battery optimizer vendor (Xiaomi, Oppo, dll.) | Priority `high`; WorkManager fallback; panduan di app untuk mematikan optimasi baterai. |
| Token FCM berubah / HP ganti | Tabel `devices`; upsert token di `onNewToken` & saat app dibuka; hapus token invalid. |
| Identitas hilang saat uninstall / hapus data / ganti HP (akun anonim) | Peringatan di Pengaturan; v2: tautkan email terverifikasi (§5.1). |
| Sesi dicabut server (akun dihapus, token tidak berlaku) | Kembali ke Kenalan dengan isian lama, masuk lagi sebagai akun baru, gabung ulang dengan kode (§5.1). |
| Penyalahgunaan anonymous sign-in (bot membuat akun) | Rate limit bawaan Supabase; opsional CAPTCHA jika app disebar luas. |
| Konflik edit | Hanya penulis, atau anggota pada catatan yang dibagikan, yang bisa mengedit; konflik hanya mungkin di catatan yang dibagikan dan diselesaikan last-write-wins. |
| Catatan yang ditampilkan widget dihapus | State khusus di widget + ajakan memilih catatan lain. |
| Kebocoran secret | Lihat §12. |

## 12. Konfigurasi & Secret

| Nama | Lokasi | Commit? |
|---|---|---|
| `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` | `.env` di root repo → `BuildConfig` | Tidak |
| `SUPABASE_PROJECT_REF`, `SUPABASE_DB_PASSWORD` | `.env` di root repo (template: `.env.example`) | Tidak |
| `google-services.json` | `app/` | Tidak |
| `FCM_SERVICE_ACCOUNT_JSON`, `FCM_PROJECT_ID` | Supabase secrets; sumbernya `firebase-service-account.json` di root repo (izin 600, tertangkap `.gitignore`) | Tidak |
| Keystore release | di luar repo | Tidak |

## 13. Struktur Repo (rencana)

```
journal/
├── CLAUDE.md            # petunjuk singkat untuk sesi AI
├── .env                 # secret lokal (tidak di-commit), template: .env.example
├── docs/PRD.md          # dokumen ini
├── android/             # project Android Studio
└── supabase/
    ├── migrations/      # SQL: tabel, RLS, RPC, trigger
    └── functions/
        └── notify-members/
```

## 14. Standar Kode & Desain

- Prinsip kode: lihat `CLAUDE.md` § Prinsip kode (wajib diikuti setiap sesi).
- Desain UI: lihat `docs/DESIGN.md` (token warna, tipografi, bentuk, dan daftar layar).

## 15. Pertanyaan Terbuka

Belum ada. Tambahkan di sini jika muncul saat implementasi.
