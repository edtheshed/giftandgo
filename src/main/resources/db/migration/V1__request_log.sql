create table request_log (
                             id uuid primary key,
                             request_uri varchar(2048) not null,
                             request_timestamp timestamp with time zone not null,
                             response_code int not null,
                             ip_address varchar(64) not null,
                             country_code varchar(8),
                             ip_provider varchar(256),
                             time_lapsed_ms bigint not null
);
