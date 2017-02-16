# --- !Ups

INSERT INTO company(name, companies_house_identifier) VALUES ('EIGENCODE LTD', '10203299');
INSERT INTO company(name, companies_house_identifier) VALUES ('BRIGNULL LIMITED', '08456262');

INSERT INTO report(company_id, filing_date, average_days_to_pay, percent_invoices_paid_beyond_agreed_terms, percent_invoices_within_30_days, percent_invoices_within_60_days,
                   percent_invoices_beyond_60_days, start_date, end_date, payment_terms, payment_period, maximum_contract_period, payment_terms_changed_comment,
                   payment_terms_changed_notified_comment, payment_terms_comment, dispute_resolution, offer_einvoicing, offer_supply_chain_finance,
                   retention_charges_in_policy, retention_charges_in_past, payment_codes)
VALUES ('10203299', '2015-12-27', 78, 21, 9, 70,
                    21, '2015-05-28', '2015-11-27', 'We don’t have a single standard payment period.',0, 'Our maximum payment period is 90 days', NULL,
        NULL, 'All the information you need is on our website', 'All of our contracts contain an industry-standard dispute resolution process. This can be found at http://www.eigencode.io/suppliers/dispute_resolution', FALSE, TRUE,
        FALSE, FALSE, NULL);

INSERT INTO report(company_id, filing_date, average_days_to_pay, percent_invoices_paid_beyond_agreed_terms, percent_invoices_within_30_days, percent_invoices_within_60_days,
                   percent_invoices_beyond_60_days, start_date, end_date, payment_terms, payment_period, maximum_contract_period, payment_terms_changed_comment,
                   payment_terms_changed_notified_comment, payment_terms_comment, dispute_resolution, offer_einvoicing, offer_supply_chain_finance,
                   retention_charges_in_policy, retention_charges_in_past, payment_codes)
VALUES ('10203299', '2016-06-27', 84, 19, 9, 60, 31, '2015-11-28', '2016-05-27', 'We don’t have a single standard payment period.',0, 'Our maximum payment period is 90 days', 'All the information you need is on our website',
        NULL, NULL, 'All of our contracts contain an industry-standard dispute resolution process. This can be found at http://www.eigencode.io/suppliers/dispute_resolution', FALSE, FALSE,
        FALSE, TRUE, NULL);

INSERT INTO report(company_id, filing_date, average_days_to_pay, percent_invoices_paid_beyond_agreed_terms, percent_invoices_within_30_days, percent_invoices_within_60_days,
                   percent_invoices_beyond_60_days, start_date, end_date, payment_terms, payment_period, maximum_contract_period, payment_terms_changed_comment,
                   payment_terms_changed_notified_comment, payment_terms_comment, dispute_resolution, offer_einvoicing, offer_supply_chain_finance,
                   retention_charges_in_policy, retention_charges_in_past, payment_codes)
VALUES ('08456262', '2016-03-21', 38, 16, 84, 9,
                    7, '2015-09-21', '2016-03-20', 'Our standard payment period is 30 days from receipt of invoice, subject to our weekly payment run.',30, 'Our maximum payment period is 60 days. However, this relates only to contracts with a value above £100,000 per month.', NULL,
        NULL, 'All suppliers must submit invoices and all other documentation through our online system. Our supplier FAQ can be found at www.brignullltd.com/suppliers/suppliersetup.asp', E'We have a dispute resolution process. You can find details at www.brignullltd.com/suppliers/contract-disputes.asp\n\nWe support Alternative Dispute Resolution (ADR) processes such as arbitration', TRUE, TRUE,
        FALSE, TRUE, 'We have been signatories to the Prompt Payment Code since 2014.');

INSERT INTO report(company_id, filing_date, average_days_to_pay, percent_invoices_paid_beyond_agreed_terms, percent_invoices_within_30_days, percent_invoices_within_60_days,
                   percent_invoices_beyond_60_days, start_date, end_date, payment_terms, payment_period, maximum_contract_period, payment_terms_changed_comment,
                   payment_terms_changed_notified_comment, payment_terms_comment, dispute_resolution, offer_einvoicing, offer_supply_chain_finance,
                   retention_charges_in_policy, retention_charges_in_past, payment_codes)
VALUES ('08456262', '2016-09-21', 36, 11, 89, 4,
                    7, '2016-03-21', '2016-09-20', 'Our standard payment period is 30 days from receipt of invoice, subject to our weekly payment run.',30, 'Our maximum payment period is 60 days. However, this relates only to contracts with a value above £100,000 per month.', NULL,
        NULL, 'All suppliers must submit invoices and all other documentation through our online system. Our supplier FAQ can be found at www.brignullltd.com/suppliers/suppliersetup.asp', E'We have a dispute resolution process. You can find details at www.brignullltd.com/suppliers/contract-disputes.asp\n\nWe support Alternative Dispute Resolution (ADR) processes such as arbitration', TRUE, FALSE,
        FALSE, TRUE, 'We have been signatories to the Prompt Payment Code since 2014.');

# --- !Downs

DELETE FROM report;
DELETE FROM company;

