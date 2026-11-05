-- jOURnal: anggota yang boleh mengedit catatan bersama juga boleh menghapusnya (US-18).
-- Siapa yang boleh mengubah baris sudah dijaga policy `notes_update`; trigger tinggal menjaga akses tetap hak penulis.
create or replace function public.notes_guard_immutable_columns()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.group_id <> old.group_id or new.author_id <> old.author_id then
    raise exception 'NOTE_IMMUTABLE_COLUMN';
  end if;
  -- auth.uid() null = server/admin (mis. job pembersihan), bukan pengguna.
  if auth.uid() <> old.author_id and new.shared <> old.shared then
    raise exception 'NOTE_AUTHOR_ONLY';
  end if;
  return new;
end;
$$;
