-- jOURnal: skema awal (tabel, RLS, RPC, job terjadwal).
-- Rujukan: docs/PRD.md §8.

create extension if not exists pg_cron with schema pg_catalog;

-- ---------------------------------------------------------------------------
-- Tabel
-- ---------------------------------------------------------------------------

create table public.profiles (
  id           uuid primary key references auth.users on delete cascade,
  display_name text not null check (char_length(display_name) between 1 and 40),
  email        text not null check (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
  avatar_url   text,
  updated_at   timestamptz not null default now()
);

create table public.devices (
  fcm_token  text primary key,
  user_id    uuid not null references auth.users on delete cascade,
  updated_at timestamptz not null default now()
);
create index devices_user_id_idx on public.devices (user_id);

create table public.groups (
  id                uuid primary key default gen_random_uuid(),
  name              text not null check (char_length(name) between 1 and 50),
  owner_id          uuid not null references auth.users,
  invite_code       text unique,
  invite_expires_at timestamptz,
  archived_at       timestamptz,
  created_at        timestamptz not null default now()
);

create table public.group_members (
  group_id  uuid not null references public.groups (id) on delete cascade,
  user_id   uuid not null references auth.users on delete cascade,
  notify    boolean not null default true,
  joined_at timestamptz not null default now(),
  primary key (group_id, user_id)
);
create index group_members_user_id_idx on public.group_members (user_id);

create table public.notes (
  id         uuid primary key default gen_random_uuid(),
  group_id   uuid not null references public.groups (id),
  author_id  uuid not null references auth.users,
  content    text not null check (char_length(content) between 1 and 500),
  color      text check (color in ('butter', 'mint', 'sky', 'peach', 'lilac', 'plain')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);
create index notes_group_id_updated_at_idx on public.notes (group_id, updated_at desc);

-- ---------------------------------------------------------------------------
-- Trigger
-- ---------------------------------------------------------------------------

create function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create trigger profiles_touch_updated_at
  before update on public.profiles
  for each row execute function public.touch_updated_at();

create trigger notes_touch_updated_at
  before update on public.notes
  for each row execute function public.touch_updated_at();

-- Catatan tidak boleh dipindah grup atau berganti penulis setelah dibuat.
create function public.notes_guard_immutable_columns()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.group_id <> old.group_id or new.author_id <> old.author_id then
    raise exception 'NOTE_IMMUTABLE_COLUMN';
  end if;
  return new;
end;
$$;

create trigger notes_guard_immutable_columns
  before update on public.notes
  for each row execute function public.notes_guard_immutable_columns();

-- ---------------------------------------------------------------------------
-- Helper untuk RLS (security definer agar tidak rekursif terhadap RLS group_members)
-- ---------------------------------------------------------------------------

create function public.is_member(p_group_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.group_members
    where group_id = p_group_id and user_id = auth.uid()
  );
$$;

create function public.shares_group_with(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.group_members mine
    join public.group_members theirs on theirs.group_id = mine.group_id
    where mine.user_id = auth.uid() and theirs.user_id = p_user_id
  );
$$;

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------

alter table public.profiles      enable row level security;
alter table public.devices       enable row level security;
alter table public.groups        enable row level security;
alter table public.group_members enable row level security;
alter table public.notes         enable row level security;

create policy profiles_select on public.profiles
  for select to authenticated
  using (id = auth.uid() or public.shares_group_with(id));
create policy profiles_insert on public.profiles
  for insert to authenticated
  with check (id = auth.uid());
create policy profiles_update on public.profiles
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

create policy devices_all on public.devices
  for all to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

create policy groups_select on public.groups
  for select to authenticated
  using (public.is_member(id));

create policy group_members_select on public.group_members
  for select to authenticated
  using (public.is_member(group_id));
-- Hanya kolom notify milik sendiri yang boleh diubah langsung.
create policy group_members_update_own on public.group_members
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());
revoke update on public.group_members from authenticated;
grant update (notify) on public.group_members to authenticated;

create policy notes_select on public.notes
  for select to authenticated
  using (public.is_member(group_id));
create policy notes_insert on public.notes
  for insert to authenticated
  with check (author_id = auth.uid() and public.is_member(group_id));
-- Hapus = soft delete lewat update deleted_at; tidak ada policy delete.
create policy notes_update_own on public.notes
  for update to authenticated
  using (author_id = auth.uid())
  with check (author_id = auth.uid() and public.is_member(group_id));

-- Tidak ada akses untuk role anon (pengguna selalu sign-in, minimal anonim).
revoke all on public.profiles, public.devices, public.groups, public.group_members, public.notes from anon;

-- ---------------------------------------------------------------------------
-- RPC grup
-- Kode error (dipetakan di app): NOT_AUTHENTICATED, PROFILE_REQUIRED, INVITE_NOT_FOUND,
-- INVITE_EXPIRED, GROUP_FULL, ALREADY_MEMBER, NOT_MEMBER, NOT_OWNER, CANNOT_REMOVE_SELF
-- ---------------------------------------------------------------------------

create function public.generate_invite_code()
returns text
language plpgsql
volatile
set search_path = ''
as $$
declare
  -- Tanpa karakter ambigu: 0 O 1 I L
  alphabet constant text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  code_length constant int := 6;
  code text;
begin
  loop
    code := '';
    for i in 1..code_length loop
      code := code || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1);
    end loop;
    exit when not exists (select 1 from public.groups where invite_code = code);
  end loop;
  return code;
end;
$$;

create function public.require_user()
returns uuid
language plpgsql
stable
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'NOT_AUTHENTICATED';
  end if;
  return auth.uid();
end;
$$;

create function public.require_owner(p_group_id uuid)
returns void
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not exists (
    select 1 from public.groups
    where id = p_group_id and owner_id = auth.uid() and archived_at is null
  ) then
    raise exception 'NOT_OWNER';
  end if;
end;
$$;

create function public.create_group(p_name text)
returns table (group_id uuid, invite_code text)
language plpgsql
security definer
set search_path = ''
as $$
declare
  invite_validity constant interval := interval '7 days';
  v_user uuid := public.require_user();
  v_group public.groups;
begin
  if not exists (select 1 from public.profiles where id = v_user) then
    raise exception 'PROFILE_REQUIRED';
  end if;

  insert into public.groups (name, owner_id, invite_code, invite_expires_at)
  values (trim(p_name), v_user, public.generate_invite_code(), now() + invite_validity)
  returning * into v_group;

  insert into public.group_members (group_id, user_id) values (v_group.id, v_user);

  return query select v_group.id, v_group.invite_code;
end;
$$;

create function public.join_group(p_code text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  max_members constant int := 10;
  v_user uuid := public.require_user();
  v_group public.groups;
begin
  if not exists (select 1 from public.profiles where id = v_user) then
    raise exception 'PROFILE_REQUIRED';
  end if;

  select * into v_group
  from public.groups
  where invite_code = upper(trim(p_code)) and archived_at is null
  for update;

  if not found then
    raise exception 'INVITE_NOT_FOUND';
  end if;
  if v_group.invite_expires_at < now() then
    raise exception 'INVITE_EXPIRED';
  end if;
  if exists (select 1 from public.group_members where group_id = v_group.id and user_id = v_user) then
    raise exception 'ALREADY_MEMBER';
  end if;
  if (select count(*) from public.group_members where group_id = v_group.id) >= max_members then
    raise exception 'GROUP_FULL';
  end if;

  insert into public.group_members (group_id, user_id) values (v_group.id, v_user);
  return v_group.id;
end;
$$;

create function public.leave_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := public.require_user();
  v_next_owner uuid;
begin
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

create function public.remove_member(p_group_id uuid, p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform public.require_owner(p_group_id);
  if p_user_id = auth.uid() then
    raise exception 'CANNOT_REMOVE_SELF';
  end if;

  delete from public.group_members where group_id = p_group_id and user_id = p_user_id;
  if not found then
    raise exception 'NOT_MEMBER';
  end if;
end;
$$;

create function public.regenerate_invite(p_group_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  invite_validity constant interval := interval '7 days';
  v_code text;
begin
  perform public.require_owner(p_group_id);

  update public.groups
  set invite_code = public.generate_invite_code(), invite_expires_at = now() + invite_validity
  where id = p_group_id
  returning invite_code into v_code;

  return v_code;
end;
$$;

create function public.rename_group(p_group_id uuid, p_name text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform public.require_owner(p_group_id);
  update public.groups set name = trim(p_name) where id = p_group_id;
end;
$$;

-- Fungsi internal tidak boleh dipanggil langsung oleh client.
revoke execute on function
  public.generate_invite_code(),
  public.require_user(),
  public.require_owner(uuid),
  public.touch_updated_at(),
  public.notes_guard_immutable_columns()
from public, anon, authenticated;

revoke execute on function
  public.create_group(text),
  public.join_group(text),
  public.leave_group(uuid),
  public.remove_member(uuid, uuid),
  public.regenerate_invite(uuid),
  public.rename_group(uuid, text),
  public.is_member(uuid),
  public.shares_group_with(uuid)
from public, anon;

grant execute on function
  public.create_group(text),
  public.join_group(text),
  public.leave_group(uuid),
  public.remove_member(uuid, uuid),
  public.regenerate_invite(uuid),
  public.rename_group(uuid, text),
  public.is_member(uuid),
  public.shares_group_with(uuid)
to authenticated;

-- ---------------------------------------------------------------------------
-- Realtime & job terjadwal
-- ---------------------------------------------------------------------------

alter publication supabase_realtime add table public.notes;

-- Setiap hari 03:00 UTC: hapus permanen catatan yang di-soft-delete lebih dari 30 hari.
select cron.schedule(
  'purge-deleted-notes',
  '0 3 * * *',
  $$ delete from public.notes where deleted_at < now() - interval '30 days' $$
);
