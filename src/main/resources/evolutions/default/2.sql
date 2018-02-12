# --- !Ups

alter table "report" add COLUMN "archived_on" TIMESTAMP NULL;
alter table "report" add COLUMN "archive_comment" TEXT NULL;

# --- !Downs

alter table "report" drop COLUMN "archived_on";
alter table "report" drop COLUMN "archive_comment";
