insert into tenant_details (id, identifier, enabled)
values (1, 'acme', true),
       (2, 'beans', true),
       (3, 'pixie', false);

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
values (1, 'plan', 'premium'),
       (1, 'region', 'eu-north-1'),
       (3, 'onboarding', 'true');
