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

Konfigurasi app (`SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`) dibaca Gradle dari `.env` di root repo.

## Progres
Status milestone dicatat di `docs/PRD.md` bagian 10.
- **M0 (setup) ✅ selesai 2026-10-18**: project Android build bersih (Compose, Hilt, Supabase client, tema dari DESIGN.md); migrasi awal + 16 tes pgTAP lulus di lokal; migrasi sudah di-push ke cloud (project `anwqobtneghvjzfivqsg`, sudah di-link); anonymous sign-in aktif.
- **M1 (auth & grup) ✅ selesai 2026-10-18**: layar Kenalan (anonymous sign-in + profil), Mulai (buat grup / gabung dengan kode), Beranda dengan chip pemilih grup + lembar undangan; Room (`my_profile`, `groups`) + DataStore untuk grup terpilih. Diuji di emulator AVD `jadwal`: 3 akun bergabung ke satu grup, error kode salah/sudah anggota tampil.
- Berikutnya: **M2** (catatan: tulis/edit/hapus, Room + Realtime).
