# jOURnal

Aplikasi catatan Android (`com.iqbal.journal`) untuk grup kecil (2–10 orang). Setiap anggota bisa memasang widget home screen yang menampilkan satu catatan pilihan, dan widget ter-update otomatis saat catatan itu berubah.

**Baca `docs/PRD.md` sebelum mengerjakan apa pun**, dan `docs/DESIGN.md` sebelum menyentuh UI. Kalau implementasi berubah dari dokumen, perbarui dokumennya juga.

## Ringkasan stack
- Android native: Kotlin, Jetpack Compose (Material 3), Glance (widget), Room, Hilt, WorkManager
- Backend: Supabase (Postgres + RLS, Anonymous Auth + profil nama/email, Realtime, Edge Functions)
- Push: Firebase Cloud Messaging saja (tanpa Firestore)

## Aturan arsitektur
- Data satu arah: Supabase → Repository → Room → (UI | Widget). UI dan widget hanya membaca Room.
- Widget tidak boleh melakukan request jaringan.
- Semua akses data dijaga RLS. Perubahan keanggotaan grup hanya lewat RPC (`create_group`, `join_group`, `leave_group`, dll.).
- Jangan commit secret: `.env`, `local.properties`, `google-services.json`, keystore, service account FCM.
- Build memakai JDK 21 (default sistem JDK 27 belum didukung Gradle/AGP). Android SDK ada di `~/Android/Sdk`.

## Prinsip kode
- **Single responsibility**: satu kelas/fungsi satu tugas. ViewModel tidak memanggil Supabase langsung, selalu lewat Repository.
- **Lapisan jelas**: `ui` → `domain` ← `data`. Model `domain` adalah Kotlin murni, tanpa dependensi Android/Supabase/Room. Mapping DTO/Entity ↔ domain ada di `data`.
- **Dependency injection** lewat Hilt; dependensi diterima lewat konstruktor, bukan dibuat di dalam kelas. Repository diekspos sebagai interface.
- **State UI immutable**: setiap layar punya satu `data class XxxUiState` dalam `StateFlow`; event dari UI masuk lewat fungsi ViewModel. Composable layar dipisah dari composable stateless agar bisa di-preview.
- **Error eksplisit**: operasi yang bisa gagal mengembalikan `Result`/sealed class, bukan melempar exception ke UI. Pesan error untuk pengguna dalam bahasa Indonesia di `strings.xml`.
- **Nama bermakna, tanpa magic number**: konstanta bernama (mis. `MAX_NOTE_LENGTH = 500`, `LOCAL_NOTES_PER_GROUP = 200`).
- **KISS & YAGNI**: jangan menambah abstraksi atau fitur di luar PRD. DRY setelah pola muncul tiga kali, bukan sebelumnya.
- **Coroutines**: tidak ada `GlobalScope`; dispatcher di-inject agar bisa dites.
- **Tes**: unit test untuk ViewModel, Repository, dan logika domain; tes SQL untuk RLS & RPC (`supabase test db`).
- **Gaya**: ktlint + detekt harus bersih sebelum commit. Komentar menjelaskan *kenapa*, bukan *apa*.
- **Commit** kecil dan fokus, format Conventional Commits (`feat:`, `fix:`, `chore:` …).

## Perintah
Semua perintah Gradle dari folder `android/`, dengan `JAVA_HOME=/usr/lib/jvm/java-21-openjdk`.
- Build debug: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Cek gaya kode (wajib bersih sebelum commit): `./gradlew ktlintCheck detekt` (perbaiki format: `./gradlew ktlintFormat`)
- Unit test: `./gradlew testDebugUnitTest`
- Database lokal + tes SQL (dari root repo): `supabase db start` lalu `supabase test db`; matikan dengan `supabase stop`
- Kirim migrasi ke Supabase cloud: `supabase link --project-ref $SUPABASE_PROJECT_REF` (sekali), lalu `supabase db push` (password dari `.env`)
- Deploy Edge Function: `supabase functions deploy notify-members --use-api` (bundling di server, tanpa Docker). Secret-nya (`FCM_*`, `NOTIFY_WEBHOOK_SECRET`) dan isi Vault sudah terpasang di cloud; cara mengisi Vault ada di komentar migrasi `20261018130000_notify_members.sql`.

Emulator (AVD `jadwal`): RAM mesin ini terbatas dan sesi pernah di-kill `systemd-oomd`. Build dulu lalu `./gradlew --stop`, baru jalankan `emulator -avd jadwal -memory 2048 -no-window -no-snapshot-save`; matikan dengan `adb shell sync && adb emu kill` (tanpa `sync`, tulisan SQLite terakhir bisa hilang).

APK release: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`. Signing dibaca dari `android/keystore.properties` (di-gitignore) yang menunjuk ke `~/.android-keystores/journal-release.jks`. Keystore dan password-nya **wajib dibackup**: tanpa keystore yang sama, update APK tidak bisa dipasang di atas versi lama. Tanpa `keystore.properties`, APK release dibangun tanpa tanda tangan. Debug dan release beda tanda tangan, jadi berpindah di antara keduanya butuh uninstall (data lokal hilang).

Konfigurasi app (`SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`) dibaca Gradle dari `.env` di root repo.

## Progres
Status milestone dicatat di `docs/PRD.md` bagian 10.
- **M0 (setup) ✅ selesai 2026-10-18**: project Android build bersih (Compose, Hilt, Supabase client, tema dari DESIGN.md); migrasi awal + 16 tes pgTAP lulus di lokal; migrasi sudah di-push ke cloud (project `anwqobtneghvjzfivqsg`, sudah di-link); anonymous sign-in aktif.
- **M1 (auth & grup) ✅ selesai 2026-10-18**: layar Kenalan (anonymous sign-in + profil), Mulai (buat grup / gabung dengan kode), Beranda dengan chip pemilih grup + lembar undangan; Room (`my_profile`, `groups`) + DataStore untuk grup terpilih. Diuji di emulator AVD `jadwal`: 3 akun bergabung ke satu grup, error kode salah/sudah anggota tampil.
- **M2 (catatan) ✅ selesai 2026-10-18**: Beranda grid masonry + FAB Tulis, Editor (tulis/edit/hapus, read-only untuk catatan orang lain); Room v2 (`notes`, `authors`, AutoMigration 1→2); `ForegroundSync` (ProcessLifecycleOwner) refresh + Realtime selama app terlihat. Diuji di emulator dengan akun kedua lewat REST: tambah/edit/hapus tampil langsung, RLS menolak edit catatan orang lain.
- **M3 (widget) ✅ selesai 2026-10-18**: widget Glance (`widget/`) membaca Room lewat `NoteWidgetStateLoader` (Flow, dikumpulkan di `provideContent` karena `update()` hanya me-recompose sesi yang hidup); `WidgetConfigActivity` untuk pasang & Atur ulang; Room v3 `widget_selections`; `NoteWidgetUpdater` me-render ulang widget saat cache berubah; badge "Di widget". Realtime: topik channel unik per langganan dan refresh baru setelah pesan sistem `postgres_changes` (keduanya menutup celah perubahan yang hilang, sudah diuji).
- **M4 (push & sinkron background) ✅ selesai 2026-10-18**: trigger `notes` → `pg_net` → Edge Function `notify-members` → FCM data message → `FcmService` → `NoteSyncWorker` (expedited) memperbarui Room, widget, dan notifikasi catatan baru; token di `devices` (didaftarkan saat app dibuka & `onNewToken` lewat `RegisterDeviceWorker`); `PeriodicSyncWorker` 15 menit + rapikan cache. Diuji di emulator dengan proses app dimatikan (`am kill`, bukan force-stop): widget berubah ±5 detik. Catatan: setelah `adb install` app harus dibuka sekali sebelum bisa menerima FCM.
- **M5 (polish) ✅ selesai 2026-10-18**: Detail grup (US-13/14) + anggota di Room (`members`); Pengaturan (profil, notifikasi per grup, optimasi baterai, tanpa logout); offline (US-12) lewat antrean `pending_ops` + `NoteOutbox` + `OutboxWorker`; Room v5; navigasi kembali tidak pernah membuang layar awal. APK release ditandatangani (R8, 4,1 MB) dan diuji di emulator.
- MVP (M0–M5) selesai. M6 (US-15 warna catatan, US-16 widget "catatan terbaru") opsional.
