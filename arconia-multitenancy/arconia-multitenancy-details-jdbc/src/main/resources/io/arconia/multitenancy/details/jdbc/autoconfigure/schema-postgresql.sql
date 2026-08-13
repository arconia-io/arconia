create table tenant_details
(
    id         serial primary key,
    identifier text default pg_catalog.gen_random_uuid() not null unique,
    enabled    boolean default true not null,
    created_at timestamp default now() not null
);

create table tenant_details_attributes
(
    tenant_id       integer not null references tenant_details (id),
    attribute_name  text not null,
    attribute_value text not null,
    primary key (tenant_id, attribute_name)
);
