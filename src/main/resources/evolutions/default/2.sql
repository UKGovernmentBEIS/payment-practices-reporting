# --- !Ups

create table "comment" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"report_id" BIGINT NOT NULL,"comment" VARCHAR NOT NULL,"timestamp" TIMESTAMP NOT NULL);
alter table "comment" add constraint "comment_report_fk" foreign key("report_id") references "report"("report_id") on update NO ACTION on delete CASCADE;

alter table "report" add COLUMN "archived_on" TIMESTAMP NULL;

# --- !Downs

alter table "report" drop COLUMN "archived_on";
drop table "comment";
