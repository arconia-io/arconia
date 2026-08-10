create table tenant_details
(
    id         serial primary key,
    identifier text not null unique default pg_catalog.gen_random_uuid(),
    enabled    boolean              default true,
    created_at timestamp            default now()
);

create table tenant_details_attributes
(
    tenant_id       integer not null references tenant_details (id),
    attribute_name  text    not null,
    attribute_value text    not null,
    primary key (tenant_id, attribute_name)
);
