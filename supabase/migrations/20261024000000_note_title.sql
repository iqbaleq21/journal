-- jOURnal: judul catatan opsional (PRD US-20). String kosong = tanpa judul; app lama yang tidak mengirim
-- kolom ini tetap bisa menulis.
alter table public.notes
  add column title text not null default '' check (char_length(title) <= 60);
