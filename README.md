# pension-scheme-return-frontend

Pension Scheme Return Frontend is the frontend interface for the pension scheme return service.
The pension scheme return service sits under the manage pension scheme service and is a yearly
return that must be completed for each scheme by the PSA or PSP for the scheme.

***

## Technical documentation

### Before running the app

Run the following command to start all the related services for this project:
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

### Common Issues & Fixes

#### Issue:

sbt compilation warning `"node.js detection failed"`

#### Fix:

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.zshrc
nvm install 18
nvm use 18
```

You can then verify which version of node.js you have by running:

```bash
node -v
```

At the time of writing, v18.19.0 is being used.

***

## Licence

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

##
