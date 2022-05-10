
begin transaction;

create role archief_dml;
create role archief_ddl;
create role archief_ro;

create user archief_dba;
create user archief_readonly;

revoke all on database archief from public;
revoke create on database archief from archief_dml, archief_ro;

grant all on database archief to archief_ddl;
grant connect on database archief to archief_ddl, archief_dml, archief_ro;

grant archief_ro to archief_readonly;
grant archief_dml to archief;
grant archief_ddl to archief;
grant archief_ddl to archief_dba;

alter user archief_ddl nologin;
alter user archief_dml nologin;
alter user archief_ro nologin;

revoke all on schema public from public, archief_ddl, archief_dml, archief_ro;
grant all on schema public to archief_ddl;
grant usage on schema public to archief_dml, archief_ro;

grant select, insert, update, delete on all tables in schema public to archief_dml;
grant temporary on database archief to archief_dml, archief_ro;

commit;
