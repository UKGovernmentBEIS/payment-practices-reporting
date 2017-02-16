# --- !Ups
create table "company" ("companies_house_identifier" VARCHAR(255) NOT NULL PRIMARY KEY,"name" VARCHAR(255) NOT NULL);

create table "report" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "company_id" VARCHAR(255) NOT NULL,
  "filing_date" date NOT NULL,
  "average_days_to_pay" INTEGER NOT NULL,
  "percent_invoices_paid_beyond_agreed_terms" INTEGER NOT NULL,
  "percent_invoices_within_30_days" INTEGER NOT NULL,
  "percent_invoices_within_60_days" INTEGER NOT NULL,
  "percent_invoices_beyond_60_days" INTEGER NOT NULL,
  "start_date" date NOT NULL,"end_date" date NOT NULL,
  "payment_terms" VARCHAR(255) NOT NULL,
  "payment_period" INTEGER NOT NULL,
  "maximum_contract_period" VARCHAR(255) NOT NULL,
  "payment_terms_changed_comment" VARCHAR(255),
  "payment_terms_changed_notified_comment" VARCHAR(255),
  "payment_terms_comment" VARCHAR(255),
  "dispute_resolution" VARCHAR(255) NOT NULL,
  "offer_einvoicing" BOOLEAN NOT NULL,
  "offer_supply_chain_finance" BOOLEAN NOT NULL,
  "retention_charges_in_policy" BOOLEAN NOT NULL,
  "retention_charges_in_past" BOOLEAN NOT NULL,
  "payment_codes" VARCHAR(255)
);

create index "report_company_idx" on "report" ("company_id");
alter table "report" add constraint "report_company_fk" foreign key("company_id") references "company"("companies_house_identifier") on update NO ACTION on delete CASCADE;

# --- !Downs
alter table "report" drop constraint "report_company_fk";
drop table "report";
drop table "company";