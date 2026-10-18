-- Tes trigger webhook notify-members. Jalankan: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(5);

insert into auth.users (id, email) values ('00000000-0000-0000-0000-00000000000a', 'a@test.dev');
insert into public.profiles (id, display_name, email)
values ('00000000-0000-0000-0000-00000000000a', 'Ayah', 'a@test.dev');
insert into public.groups (id, name, owner_id)
values ('00000000-0000-0000-0000-0000000000a1', 'Rumah', '00000000-0000-0000-0000-00000000000a');

create temp table queued on commit drop as select count(*) as n from net.http_request_queue;

-- Tanpa konfigurasi Vault: catatan tetap tersimpan, tidak ada webhook.
insert into public.notes (id, group_id, author_id, content) values
  ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-0000000000a1',
   '00000000-0000-0000-0000-00000000000a', 'Galon habis');
select is((select count(*) from net.http_request_queue), (select n from queued), 'tanpa Vault tidak ada webhook');

select vault.create_secret('https://contoh.test/functions/v1/notify-members', 'notify_members_url');
select vault.create_secret('rahasia-uji', 'notify_members_secret');

insert into public.notes (id, group_id, author_id, content) values
  ('00000000-0000-0000-0000-0000000000b2', '00000000-0000-0000-0000-0000000000a1',
   '00000000-0000-0000-0000-00000000000a', 'Kode wifi');
select is(
  (select convert_from(body, 'utf8')::jsonb from net.http_request_queue order by id desc limit 1),
  jsonb_build_object(
    'note_id', '00000000-0000-0000-0000-0000000000b2', 'group_id', '00000000-0000-0000-0000-0000000000a1',
    'author_id', '00000000-0000-0000-0000-00000000000a', 'is_new', true),
  'catatan baru mengirim webhook dengan is_new = true');
select is(
  (select headers ->> 'x-webhook-secret' from net.http_request_queue order by id desc limit 1),
  'rahasia-uji', 'webhook membawa secret dari Vault');

update public.notes set deleted_at = now() where id = '00000000-0000-0000-0000-0000000000b2';
select is(
  (select (convert_from(body, 'utf8')::jsonb ->> 'is_new')::boolean from net.http_request_queue order by id desc limit 1),
  false, 'soft delete dikabarkan sebagai perubahan (is_new = false)');

select is(
  has_function_privilege('authenticated', 'public.notify_note_change()', 'execute'), false,
  'pengguna tidak bisa memanggil fungsi trigger langsung');

select * from finish();
rollback;
