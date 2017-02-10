# payment-practices-reporting
[![CircleCI](https://circleci.com/gh/UKGovernmentBEIS/payment-practices-reporting.svg?style=svg)](https://circleci.com/gh/UKGovernmentBEIS/payment-practices-reporting)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6640412ef5a54d149d5a7ed87e583521)](https://www.codacy.com/app/UKGovernmentBEIS/payment-practices-reporting?utm_source=github.com&utm_medium=referral&utm_content=UKGovernmentBEIS/payment-practices-reporting&utm_campaign=badger)


## Configuration

### Companies House API
You'll need to obtain an API key for making calls to the Companies House API. Go to the 
Companies House [developer hub](https://developer.companieshouse.gov.uk/api/docs/),
register an account and create a new application. One of the pieces of information
provided is an API key. You can provide this key to the application by setting the
`COMPANIES_HOUSE_API_KEY` environment variable prior to starting `sbt`.

Similarly, for production, inject the api key value into the environment with that
env variable.
