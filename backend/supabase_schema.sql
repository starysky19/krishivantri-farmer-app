-- Supabase PostgreSQL Schema for KRASHIVANTRI Mapping & Drone Marketplace
-- Run in Supabase SQL Editor (https://leawuzhjkuzwgzdduwzq.supabase.co)
-- Enables users, farmer_profiles, pilot_profiles, orders, farm_boundaries, operator_locations, and Realtime updates.

create extension if not exists "uuid-ossp";

create table if not exists public.users (
    id text primary key,
    phone text unique not null,
    name text not null,
    role text not null check (role in ('admin', 'pilot', 'farmer')),
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create table if not exists public.farmer_profiles (
    user_id text primary key references public.users(id) on delete cascade,
    farmer_id_number text not null,
    coins integer not null default 0
);

create table if not exists public.pilot_profiles (
    user_id text primary key references public.users(id) on delete cascade,
    drone_type text not null default 'quadcopter',
    tank_capacity double precision not null default 10,
    license_document_uri text not null default '',
    is_license_verified boolean not null default false,
    bank_upi_id text not null default '',
    wallet_balance double precision not null default 0,
    is_online boolean not null default false,
    rating_average double precision not null default 5.0,
    rating_count integer not null default 0,
    current_lat double precision,
    current_lng double precision,
    location_updated_at bigint not null default 0
);

create table if not exists public.orders (
    id text primary key,
    farmer_id text not null references public.users(id) on delete cascade,
    assigned_pilot_id text references public.users(id),
    field_polygon_coordinates jsonb not null,
    acres_to_spray double precision not null,
    location_address text not null,
    total_amount double precision not null,
    admin_commission double precision not null,
    pilot_earnings double precision not null,
    payment_method text not null check (payment_method in ('online', 'cash')),
    payment_status text not null check (payment_status in ('pending', 'paid')),
    order_status text not null check (order_status in ('pending', 'accepted', 'in_progress', 'awaiting_confirmation', 'completed', 'cancelled')),
    pilot_eta_minutes integer not null default 15,
    initial_eta_minutes integer not null default 15,
    distance_km double precision,
    field_center_lat double precision not null default 0,
    field_center_lng double precision not null default 0,
    accepted_at bigint,
    pilot_rating integer,
    razorpay_payment_id text,
    pilot_wallet_credited boolean not null default false,
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create index if not exists idx_orders_farmer on public.orders(farmer_id);
create index if not exists idx_orders_status on public.orders(order_status);

create table if not exists public.farm_boundaries (
    id uuid primary key default uuid_generate_v4(),
    farmer_id text not null,
    farm_name text not null,
    polygon_coordinates jsonb not null,
    area_acres numeric(10, 2) not null,
    area_hectares numeric(10, 2) not null,
    center_lat double precision not null,
    center_lng double precision not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_farm_boundaries_farmer on public.farm_boundaries(farmer_id);

create table if not exists public.operator_locations (
    id text primary key,
    name text not null,
    role_type text not null check (role_type in ('drone_operator', 'drone_mitra')),
    lat double precision not null,
    lng double precision not null,
    drone_type text not null default 'quadcopter',
    is_available boolean not null default true,
    heading_degrees real not null default 0,
    updated_at timestamptz not null default now()
);

-- Enable Row Level Security
alter table public.users enable row level security;
alter table public.farmer_profiles enable row level security;
alter table public.pilot_profiles enable row level security;
alter table public.orders enable row level security;
alter table public.farm_boundaries enable row level security;
alter table public.operator_locations enable row level security;

create policy "Allow app user read/write" on public.users for all using (true) with check (true);
create policy "Allow app farmer profile read/write" on public.farmer_profiles for all using (true) with check (true);
create policy "Allow app pilot profile read/write" on public.pilot_profiles for all using (true) with check (true);
create policy "Allow app order read/write" on public.orders for all using (true) with check (true);
create policy "Allow authenticated or anon farm boundary read/write"
    on public.farm_boundaries for all
    using (true)
    with check (true);
create policy "Allow realtime operator location read/write"
    on public.operator_locations for all
    using (true)
    with check (true);

-- Enable Supabase Realtime broadcasts for live updates
alter publication supabase_realtime add table public.orders;
alter publication supabase_realtime add table public.operator_locations;
alter publication supabase_realtime add table public.farm_boundaries;
