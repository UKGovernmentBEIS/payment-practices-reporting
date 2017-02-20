# --- !Ups
create table "report_header" ("id" BIGINT NOT NULL PRIMARY KEY,"company_name" VARCHAR(255) NOT NULL,"company_id" VARCHAR(255) NOT NULL,"created_at" date NOT NULL,"updated_at" date NOT NULL);
create table "report_period" ("report_id" BIGINT NOT NULL,"start_date" date NOT NULL,"end_date" date NOT NULL);
create unique index "reportperiod_report_idx" on "report_period" ("report_id");
create table "payment_terms" ("report_id" BIGINT NOT NULL,"payment_terms" VARCHAR(255) NOT NULL,"payment_period" INTEGER NOT NULL,"maximum_contract_period" INTEGER NOT NULL,"maximum_contract_period_comment" VARCHAR(255),"payment_terms_changed_comment" VARCHAR(255),"payment_terms_changed_notified_comment" VARCHAR(255),"payment_terms_comment" VARCHAR(255),"dispute_resolution" VARCHAR(255) NOT NULL);
create unique index "paymentterms_report_idx" on "payment_terms" ("report_id");
create table "payment_history" ("report_id" BIGINT NOT NULL,"average_days_to_pay" INTEGER NOT NULL,"percent_paid_later_than_agreed_terms" INTEGER NOT NULL,"percent_invoices_within30days" INTEGER NOT NULL,"percent_invoices_within60days" INTEGER NOT NULL,"percent_invoices_beyond60days" INTEGER NOT NULL);
create unique index "paymenthistory_report_idx" on "payment_history" ("report_id");
create table "other_info" ("report_id" BIGINT NOT NULL,"offer_einvoicing" BOOLEAN NOT NULL,"offer_supply_chain_finance" BOOLEAN NOT NULL,"retention_charges_in_policy" BOOLEAN NOT NULL,"retention_charges_in_past" BOOLEAN NOT NULL,"payment_codes" VARCHAR(255));
create unique index "otherinfo_report_idx" on "other_info" ("report_id");
create table "filing" ("report_id" BIGINT NOT NULL,"filing_date" date NOT NULL,"approved_by" VARCHAR(255) NOT NULL);
create unique index "filing_report_idx" on "filing" ("report_id");
alter table "report_period" add constraint "reportperiod_report_fk" foreign key("report_id") references "report_header"("id") on update NO ACTION on delete CASCADE;
alter table "payment_terms" add constraint "paymentterms_report_fk" foreign key("report_id") references "report_header"("id") on update NO ACTION on delete CASCADE;
alter table "payment_history" add constraint "paymenthistory_report_fk" foreign key("report_id") references "report_header"("id") on update NO ACTION on delete CASCADE;
alter table "other_info" add constraint "otherinfo_report_fk" foreign key("report_id") references "report_header"("id") on update NO ACTION on delete CASCADE;
alter table "filing" add constraint "filing_report_fk" foreign key("report_id") references "report_header"("id") on update NO ACTION on delete CASCADE;

# --- !Downs
alter table "filing" drop constraint "filing_report_fk";
alter table "other_info" drop constraint "otherinfo_report_fk";
alter table "payment_history" drop constraint "paymenthistory_report_fk";
alter table "payment_terms" drop constraint "paymentterms_report_fk";
alter table "report_period" drop constraint "reportperiod_report_fk";
drop table "filing";
drop table "other_info";
drop table "payment_history";
drop table "payment_terms";
drop table "report_period";
drop table "report_header";