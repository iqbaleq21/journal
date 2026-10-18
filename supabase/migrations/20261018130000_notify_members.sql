-- jOURnal: kabari HP anggota lain saat catatan berubah (PRD §5.5, §8.3).
-- Trigger mengirim webhook ke Edge Function `notify-members` lewat pg_net (asinkron, tidak menahan transaksi).
--
-- URL fungsi dan secret webhook TIDAK disimpan di repo. Isi sekali per project lewat Vault:
--   select vault.create_secret('https://<ref>.supabase.co/functions/v1/notify-members', 'notify_members_url');
--   select vault.create_secret('<secret acak>', 'notify_members_secret');
-- Secret yang sama dipasang di Edge Function sebagai NOTIFY_WEBHOOK_SECRET.

create extension if not exists pg_net with schema extensions;

create function public.notify_note_change()
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
      'author_id', new.author_id,
      'is_new', tg_op = 'INSERT'
    ),
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-webhook-secret', v_secret),
    timeout_milliseconds := 5000
  );
  return null;
end;
$$;

revoke execute on function public.notify_note_change() from public, anon, authenticated;

-- Soft delete adalah update, jadi penghapusan ikut dikabarkan. Hard delete oleh job pembersihan tidak.
create trigger notes_notify_members
  after insert or update on public.notes
  for each row execute function public.notify_note_change();
