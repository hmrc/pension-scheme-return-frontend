# App name

Pension Scheme Return Frontend is the frontend interface for the pension scheme return service.
The pension scheme return service sits under the manage pension scheme service and is a yearly
return that must be completed for each scheme by the PSA or PSP for the scheme.

***

## Technical documentation

### Before running the app

Run the following command to start all of the related services for this project:
```bash
sm2 -start PSR_ALL
```
Included in the above command is `PENSION_SCHEME_RETURN_FRONTEND`, which is this repository's most recent release.
If you want to run your local version of this code instead, run:
```bash
sm2 -stop PENSION_SCHEME_RETURN_FRONTEND
```

then:

```bash
sbt 'run'
```

Note: this service runs on port 10701 by default, but a different port (e.g. 17000) may be specified, as shown in the example below:

```bash
sbt 'run 17000'
```

***

### Running the test suite

```bash
sbt clean coverage test it:test coverageReport
```

or

You can execute the [runtests.sh](runtests.sh) file to run the tests and generate coverage report easily.
```bash
/bin/bash ./runtests.sh
```

***

#### Auto-completion (and aliases)

In order to improve the user experience when entering a country we use a Javascript powered country auto-completion facility
(see https://github.com/alphagov/govuk-country-and-territory-autocomplete). This uses another configuration file which
defines aliases for countries (see https://github.com/alphagov/govuk-country-and-territory-autocomplete/blob/master/dist/location-autocomplete-graph.json)

In order for this to work correctly and only offer valid country/territory suggestions for etmp which actually exist in the associated country
"select" element, the configuration file must be consistent with the list of countries/territories which are supported.

We have created a shell script (which uses Scala Ammonite under the covers) to perform preparation of a suitable
alias config file based on the contents of the country lookup config file (see above).

- Download these 2 files from https://github.com/alphagov/govuk-country-and-territory-autocomplete

* location-autocomplete-canonical-list.json and locate in under `/conf`
* location-autocomplete-graph.json and rename it as full-location-autocomplete-graph.json and locate it under `/utils`

- Create your own etmp valid country/territory codes, named it validEtmpCountryTerritoryNames.json and locate it under `/utils`
- Adjust the countries/territories in canonical list
- Exceptional territory codes should be unified in location-autocomplete-graph
- Execute the following query from the `/utils` subdirectory to arrange location-autocomplete-graph.json . Please note that
this command will fail if the target file (the 3rd parameter) already exists.

```bash
./filter-country-territory-lookup-config.sh validEtmpCountryTerritoryNames.json full-location-autocomplete-graph.json ../app/assets/location-autocomplete-graph.json
```

You will need Scala development tools installed incl. Ammonite Scala scripting in order for this to run. See
https://docs.scala-lang.org/getting-started/index.html

***

## Licence

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
