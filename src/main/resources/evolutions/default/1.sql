# --- !Ups
create table "report" ("report_id" BIGSERIAL NOT NULL PRIMARY KEY,"company_name" VARCHAR(255) NOT NULL,"company_id" VARCHAR(255) NOT NULL,"filing_date" TIMESTAMP NOT NULL,"approved_by" VARCHAR(255) NOT NULL,"confirmation_email_address" VARCHAR(255) NOT NULL,"start_date" TIMESTAMP NOT NULL,"end_date" TIMESTAMP NOT NULL,"payment_codes" VARCHAR(245));
create table "contract_details" ("report_id" BIGINT NOT NULL UNIQUE,"payment_terms" VARCHAR(35000) NOT NULL,"shortest_payment_period" INTEGER NOT NULL,"longest_payment_period" INTEGER,"maximum_contract_period" INTEGER NOT NULL,"maximum_contract_period_comment" VARCHAR(3500),"payment_terms_changed_comment" VARCHAR(3500),"payment_terms_changed_notified_comment" VARCHAR(3500),"payment_terms_comment" VARCHAR(3500),"dispute_resolution" VARCHAR(14000) NOT NULL,"offer_einvoicing" BOOLEAN NOT NULL,"offer_supply_chain_finance" BOOLEAN NOT NULL,"retention_charges_in_policy" BOOLEAN NOT NULL,"retention_charges_in_past" BOOLEAN NOT NULL,"average_days_to_pay" INTEGER NOT NULL,"percent_paid_later_than_agreed_terms" INTEGER NOT NULL,"percent_invoices_within30days" INTEGER NOT NULL,"percent_invoices_within60days" INTEGER NOT NULL,"percent_invoices_beyond60days" INTEGER NOT NULL);
create unique index "long_form_report_idx" on "contract_details" ("report_id");
create table "confirmation_pending" ("report_id" BIGINT NOT NULL,"email_address" VARCHAR(255) NOT NULL,"url" VARCHAR(255) NOT NULL,"retry_count" INTEGER NOT NULL,"last_error_status" INTEGER,"last_error_text" VARCHAR(2048),"locked_at" TIMESTAMP);
create unique index "confirmationpending_report_idx" on "confirmation_pending" ("report_id");
create table "confirmation_sent" ("report_id" BIGINT NOT NULL,"email_address" VARCHAR(255) NOT NULL,"email_body" VARCHAR(4096) NOT NULL,"notification_id" VARCHAR(36) NOT NULL,"sent_at" TIMESTAMP NOT NULL);
create unique index "confirmationsent_report_idx" on "confirmation_sent" ("report_id");
create table "confirmation_failed" ("report_id" BIGINT NOT NULL,"email_address" VARCHAR(255) NOT NULL,"error_status" INTEGER NOT NULL,"error_text" VARCHAR(2048) NOT NULL,"failed_at" TIMESTAMP NOT NULL);
create unique index "confirmationfailed_report_idx" on "confirmation_failed" ("report_id");
create table "session" ("id" VARCHAR(36) NOT NULL PRIMARY KEY,"expires_at" TIMESTAMP NOT NULL,"session_data" VARCHAR NOT NULL);
alter table "contract_details" add constraint "long_form_report_fk" foreign key("report_id") references "report"("report_id") on update NO ACTION on delete CASCADE;
alter table "confirmation_pending" add constraint "confirmationpending_report_fk" foreign key("report_id") references "report"("report_id") on update NO ACTION on delete CASCADE;
alter table "confirmation_sent" add constraint "confirmationsent_report_fk" foreign key("report_id") references "report"("report_id") on update NO ACTION on delete CASCADE;
alter table "confirmation_failed" add constraint "confirmationfailed_report_fk" foreign key("report_id") references "report"("report_id") on update NO ACTION on delete CASCADE;

# --- !Downs
alter table "confirmation_failed" drop constraint "confirmationfailed_report_fk";
alter table "confirmation_sent" drop constraint "confirmationsent_report_fk";
alter table "confirmation_pending" drop constraint "confirmationpending_report_fk";
alter table "contract_details" drop constraint "long_form_report_fk";
drop table "session";
drop table "confirmation_failed";
drop table "confirmation_sent";
drop table "confirmation_pending";
drop table "contract_details";
drop table "report";