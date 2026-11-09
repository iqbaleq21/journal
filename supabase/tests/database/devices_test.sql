-- Tes pendaftaran token push. Jalankan: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(3);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000d1', 'lama@test.dev'),
  ('00000000-0000-0000-0000-0000000000d2', 'baru@test.dev');

create function pg_temp.act_as(p_user uuid) returns void language sql as $$
  select set_config('role', 'authenticated', true),
         set_config('request.jwt.claims', json_build_object('sub', p_user, 'role', 'authenticated')::text, true);
$$;

select pg_temp.act_as('00000000-0000-0000-0000-0000000000d1');
select lives_ok($$ select public.register_device('token-hp') $$, 'token didaftarkan');

-- Sesi lama dicabut, akun anonim baru di HP yang sama (token sama) mengambil alih token.
select pg_temp.act_as('00000000-0000-0000-0000-0000000000d2');
select lives_ok($$ select public.register_device('token-hp') $$, 'akun baru di HP yang sama bisa mendaftarkan token');
reset role;
select is(
  (select user_id from public.devices where fcm_token = 'token-hp'),
  '00000000-0000-0000-0000-0000000000d2'::uuid,
  'token kini milik akun baru');

select * from finish();
rollback;
