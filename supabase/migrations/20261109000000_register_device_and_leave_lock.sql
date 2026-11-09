-- jOURnal: pendaftaran token push lewat RPC, dan keluar grup yang aman dari balapan.

-- Setelah sesi dicabut, HP yang sama masuk sebagai akun anonim baru dengan token FCM yang sama. Baris token itu
-- masih milik akun lama, sehingga upsert langsung ditolak RLS dan HP tidak lagi menerima push. Token mewakili
-- satu HP, jadi pemiliknya selalu akun yang terakhir mendaftarkannya.
create function public.register_device(p_token text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := public.require_user();
begin
  insert into public.devices (fcm_token, user_id)
  values (p_token, v_user)
  on conflict (fcm_token) do update set user_id = excluded.user_id, updated_at = now();
end;
$$;

revoke execute on function public.register_device(text) from public, anon;
grant execute on function public.register_device(text) to authenticated;

-- Baris grup dikunci lebih dulu (sama seperti join_group): tanpa itu dua anggota terakhir yang keluar bersamaan
-- saling melihat yang lain masih ada, sehingga grup kosong tidak diarsipkan dan kodenya tetap bisa dipakai.
create or replace function public.leave_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := public.require_user();
  v_next_owner uuid;
begin
  perform 1 from public.groups where id = p_group_id for update;

  delete from public.group_members where group_id = p_group_id and user_id = v_user;
  if not found then
    raise exception 'NOT_MEMBER';
  end if;

  select user_id into v_next_owner
  from public.group_members
  where group_id = p_group_id
  order by joined_at
  limit 1;

  if v_next_owner is null then
    update public.groups
    set archived_at = now(), invite_code = null, invite_expires_at = null
    where id = p_group_id;
  else
    update public.groups
    set owner_id = v_next_owner
    where id = p_group_id and owner_id = v_user;
  end if;
end;
$$;
