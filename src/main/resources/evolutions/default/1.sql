# --- !Ups
create table "company" ("companies_house_identifier" VARCHAR(255) NOT NULL PRIMARY KEY,"name" VARCHAR(255) NOT NULL);
create table "report" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"company_id" VARCHAR(36) NOT NULL,"filing_date" date NOT NULL,"start_date" date NOT NULL,"end_date" date NOT NULL,"payment_terms" VARCHAR(35000) NOT NULL,"payment_period" INTEGER NOT NULL,"maximum_contract_period" INTEGER NOT NULL,"maximum_contract_period_comment" VARCHAR(3500),"payment_terms_changed_comment" VARCHAR(3500),"payment_terms_changed_notified_comment" VARCHAR(3500),"payment_terms_comment" VARCHAR(14000),"dispute_resolution" VARCHAR(35000) NOT NULL,"offer_einvoicing" BOOLEAN NOT NULL,"offer_supply_chain_finance" BOOLEAN NOT NULL,"retention_charges_in_policy" BOOLEAN NOT NULL,"retention_charges_in_past" BOOLEAN NOT NULL,"payment_codes" VARCHAR(255));
create index "report_company_idx" on "report" ("company_id");
create table "payment_history" ("id" BIGINT NOT NULL PRIMARY KEY,"report_id" BIGINT NOT NULL,"average_days_to_pay" INTEGER NOT NULL,"percent_paid_later_than_agreed_terms" INTEGER NOT NULL,"percent_invoices_within30days" INTEGER NOT NULL,"percent_invoices_within60days" INTEGER NOT NULL,"percent_invoices_beyond60days" INTEGER NOT NULL);
create unique index "one_payment_history_row_per_report" on "payment_history" ("report_id");
create index "paymenthistory_report_idx" on "payment_history" ("report_id");
alter table "report" add constraint "report_company_fk" foreign key("company_id") references "company"("companies_house_identifier") on update NO ACTION on delete CASCADE;
alter table "payment_history" add constraint "paymenthistory_report_fk" foreign key("report_id") references "report"("id") on update NO ACTION on delete CASCADE;

# --- !Downs
alter table "payment_history" drop constraint "paymenthistory_report_fk";
alter table "report" drop constraint "report_company_fk";
drop table "payment_history";
drop table "report";
drop table "company";