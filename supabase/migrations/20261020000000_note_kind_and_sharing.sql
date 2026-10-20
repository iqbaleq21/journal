-- jOURnal: catatan checklist dan berbagi akses edit (PRD §5.3).
-- Checklist tetap disimpan di `content` sebagai teks, satu item per baris: `[ ] item` / `[x] item`.

alter table public.notes
  add column kind   text    not null default 'text' check (kind in ('text', 'checklist')),
  add column shared boolean not null default false;

-- Anggota lain boleh mengedit catatan yang dibagikan, tapi menghapus dan mengatur akses tetap hak penulis.
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
  if auth.uid() <> old.author_id
     and (new.shared <> old.shared or new.deleted_at is distinct from old.deleted_at) then
    raise exception 'NOTE_AUTHOR_ONLY';
  end if;
  return new;
end;
$$;

drop policy notes_update_own on public.notes;
create policy notes_update on public.notes
  for update to authenticated
  using (author_id = auth.uid() or (shared and public.is_member(group_id)))
  with check (public.is_member(group_id));

-- Pengubah bisa bukan penulis: yang dikecualikan dari push adalah pengubahnya, agar penulis ikut dikabari.
create or replace function public.notify_note_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_url    text;
  v_secret text;
begin
  select decrypted_secret into v_url from vault.decrypted_secrets where name = 'notify_members_url';
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'notify_members_secret';
  -- Belum dikonfigurasi (mis. database lokal): catatan tetap tersimpan, hanya push yang dilewati.
  if v_url is null or v_secret is null then
    return null;
  end if;

  perform net.http_post(
    url := v_url,
    body := jsonb_build_object(
      'note_id', new.id,
      'group_id', new.group_id,
      'author_id', coalesce(auth.uid(), new.author_id),
      'is_new', tg_op = 'INSERT'
    ),
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-webhook-secret', v_secret),
    timeout_milliseconds := 5000
  );
  return null;
end;
$$;
