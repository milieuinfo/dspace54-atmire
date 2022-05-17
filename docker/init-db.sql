
begin transaction;

create schema public;
create role dspace_dml;
create role dspace_ddl;
create role dspace_ro;

create user dspace_dba;
create user dspace_readonly;

revoke all on database dspace from public;
revoke create on database dspace from dspace_dml, dspace_ro;

grant all on database dspace to dspace_ddl;
grant connect on database dspace to dspace_ddl, dspace_dml, dspace_ro;

grant dspace_ro to dspace_readonly;
grant dspace_dml to dspace;
grant dspace_ddl to dspace;
grant dspace_ddl to dspace_dba;

alter user dspace_ddl nologin;
alter user dspace_dml nologin;
alter user dspace_ro nologin;

revoke all on schema public from public, dspace_ddl, dspace_dml, dspace_ro;
grant all on schema public to dspace_ddl;
grant usage on schema public to dspace_dml, dspace_ro;

grant select, insert, update, delete on all tables in schema public to dspace_dml;
grant temporary on database dspace to dspace_dml, dspace_ro;

commit;
