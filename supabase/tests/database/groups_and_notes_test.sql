-- Tes RLS & RPC grup/catatan. Jalankan: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(20);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test.dev'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test.dev'),
  ('00000000-0000-0000-0000-00000000000c', 'c@test.dev');

create function pg_temp.act_as(p_user uuid) returns void language sql as $$
  select set_config('role', 'authenticated', true),
         set_config('request.jwt.claims', json_build_object('sub', p_user, 'role', 'authenticated')::text, true);
$$;

-- User A: profil lalu buat grup
select pg_temp.act_as('00000000-0000-0000-0000-00000000000a');
insert into public.profiles (id, display_name, email) values (auth.uid(), 'Ayah', 'a@test.dev');
create temp table invite on commit drop as select * from public.create_group('Rumah');
grant select on invite to authenticated;
select is(length((select invite_code from invite)), 6, 'kode undangan 6 karakter');
select is((select count(*) from public.group_members)::int, 1, 'pembuat langsung jadi anggota');

-- User B: wajib punya profil sebelum gabung
select pg_temp.act_as('00000000-0000-0000-0000-00000000000b');
select throws_ok(
  format('select public.join_group(%L)', (select invite_code from invite)),
  'PROFILE_REQUIRED', 'gabung tanpa profil ditolak');
insert into public.profiles (id, display_name, email) values (auth.uid(), 'Ibu', 'b@test.dev');
select throws_ok($$ select public.join_group('ZZZZZZ') $$, 'INVITE_NOT_FOUND', 'kode salah ditolak');
select lives_ok(
  format('select public.join_group(%L)', lower((select invite_code from invite))),
  'gabung dengan kode (huruf kecil pun) berhasil');
select throws_ok(
  format('select public.join_group(%L)', (select invite_code from invite)),
  'ALREADY_MEMBER', 'gabung dua kali ditolak');
select is((select count(*) from public.profiles)::int, 2, 'anggota bisa melihat profil sesama anggota');

-- A menulis catatan; B bisa membaca tapi tidak bisa mengubah
select pg_temp.act_as('00000000-0000-0000-0000-00000000000a');
insert into public.notes (group_id, author_id, content)
values ((select group_id from invite), auth.uid(), 'Kode wifi: rumahbahagia2026');
select pg_temp.act_as('00000000-0000-0000-0000-00000000000b');
select is((select count(*) from public.notes)::int, 1, 'anggota bisa membaca catatan grup');
update public.notes set content = 'diubah B';
select pg_temp.act_as('00000000-0000-0000-0000-00000000000a');
select is((select content from public.notes), 'Kode wifi: rumahbahagia2026', 'catatan orang lain tidak bisa diubah');
select throws_ok(
  format($$ insert into public.notes (group_id, author_id, content) values (%L, '00000000-0000-0000-0000-00000000000b', 'palsu') $$,
         (select group_id from invite)),
  '42501', null, 'tidak bisa menulis atas nama orang lain');

-- A membagikan catatannya: B boleh mengedit, tapi tidak boleh menghapus atau mencabut akses
update public.notes set shared = true;
select pg_temp.act_as('00000000-0000-0000-0000-00000000000b');
select lives_ok($$ update public.notes set content = 'diubah B', kind = 'checklist' $$, 'catatan yang dibagikan bisa diedit anggota');
select is((select content from public.notes), 'diubah B', 'hasil edit anggota tersimpan');
select throws_ok($$ update public.notes set deleted_at = now() $$, 'NOTE_AUTHOR_ONLY', 'hanya penulis yang bisa menghapus');
select throws_ok($$ update public.notes set shared = false $$, 'NOTE_AUTHOR_ONLY', 'hanya penulis yang bisa mengatur akses');

-- User C (bukan anggota) tidak melihat apa pun
select pg_temp.act_as('00000000-0000-0000-0000-00000000000c');
select is((select count(*) from public.notes)::int, 0, 'non-anggota tidak melihat catatan');
select is((select count(*) from public.groups)::int, 0, 'non-anggota tidak melihat grup');
select is((select count(*) from public.profiles)::int, 0, 'non-anggota tidak melihat profil orang lain');

-- Hak pemilik
select pg_temp.act_as('00000000-0000-0000-0000-00000000000b');
select throws_ok(
  format('select public.regenerate_invite(%L)', (select group_id from invite)),
  'NOT_OWNER', 'non-pemilik tidak bisa membuat ulang kode');
update public.groups set owner_id = auth.uid();
select pg_temp.act_as('00000000-0000-0000-0000-00000000000a');
select is((select owner_id from public.groups), '00000000-0000-0000-0000-00000000000a'::uuid, 'owner tidak bisa diubah langsung');

-- Pemilik keluar: kepemilikan pindah ke anggota berikutnya
select public.leave_group((select group_id from invite));
select pg_temp.act_as('00000000-0000-0000-0000-00000000000b');
select is((select owner_id from public.groups), '00000000-0000-0000-0000-00000000000b'::uuid, 'owner pindah saat pemilik keluar');

select * from finish();
rollback;
