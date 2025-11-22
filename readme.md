# GiftAndGo File Processor

A Spring Boot Kotlin service that:
- Accepts a text file of people via `POST /files/process`
- Parses and validates each line into a Person
- Optionally applies Validation to each person via feature flag
- Returns a JSON file
- Checks the caller’s IP against `ip-api.com` and blocks certain countries and ISPs
- Logs every request to a database

## Tech stack

- Kotlin + Spring Boot
- Spring Web, Jakarta Validation, Spring Data JPA, Spring RestClient
- H2
- WireMock for mocking ip-api.com in HTTP tests

## Configuration

```yml
features:
  person-validation: true
ipcheck:
  base-url: http://ip-api.com/json
  blocked-countries: [ CN, ES, US ]
  blocked-isps: [ amazon, aws, google, gcp, microsoft, azure ]
  timeout-ms: 1500
  enabled: true
```

`features.person-validation=true` uses `ValidatingFileProcessorService`

`features.person-validation=false` uses `RelaxedFileProcessorService`

`ipcheck.enabled=true` enables the `IpGateAndAuditFilter`

## Running locally

`./gradlew clean bootRun`

## Tests

- Unit tests for parsing, validation and ipCheck logic
- HTTP tests using TestRestTemplate
- WireMock tests that mock `ip-api.com/json/{ip}`

### Run all tests

`./gradlew clean test`
