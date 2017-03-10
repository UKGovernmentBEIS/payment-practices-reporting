## Database Row classes

The case classes in this package model the database rows and are used
by the slick table implementations to map data in and out of the database.

### Modelling the report

I have broken up the data for the reports into sub-sections, with
logically-related data grouped into different tables. I've done this
for two main reasons:

1. The report has more than 22 fields on it, which meant that I
could not model it as a single case class
2. It is possible that at some point in the future we will break the
report form up into a multi-page flow and the most likely sequence
of pages will be the same as the groupings I've made in these rows.

### Modelling conditional questions

A lot of the questions on the report are presented as a Yes/No choice
and an optional text field, where the text need only be supplied if
the user chooses Yes. I have modelled these in the row classes with
a type of `Option[String]`, mapping to nullable varchar columns in
the database tables. The inference is that if the field is `None` in
a row then the user selected `No` on the form, and if it is `Some(text)`
then the user answered `Yes` and supplied the text.

### Modelling Confirmations

There are three tables to represent the state that confirmations can be
in. Initially a confirmation is created in a `pending` state and the process that
attempts to deliver it will either leave it `pending` (in the case
of a temporary failure), move it to `sent` (on successfully handing it
off to the GOV Notify service) or move it to `failed`, the Notify service
indicates a reason for permanent failure, such as a badly formed email address.

