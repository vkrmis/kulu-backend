# kulu-backend

The main backend service for the Kulu app.

## Set up

* Clone the app

* Create the secret config from the sample:
```
cp config/kulu-backend-secrets-sample.edn config/kulu-backend-secrets.edn
```
* Fill in the placeholder vals in the above file. Read on to learn
  more about our configuration management.

* Set up dev and test dbs:
```
$ psql
create database kulu_backend_dev;
\c kulu_backend_dev
create extension "uuid-ossp";

create database kulu_backend_test;
\c kulu_backend_test
create extension "uuid-ossp";
```
* `lein deps` to get all the dependencies
* Migration script expects DATABASE_URL in the enviroment
```
DATABASE_URL=postgres://$USER\:@localhost:5432/kulu_backend_dev lein clj-sql-up migrate
```

### Running

#### All in one

The easiest thing to do is to let foreman manage the processes:

+ Install `ruby` with rbenv/rvm, if you don't have it already
  ([version](.ruby-version)).
+ `gem install bundler` if you have a new ruby install from the above step.
+ `bundle install` to install foreman.
+ `PORT=3001 NREPL_PORT=3002 bundle exec foreman start` to start all the process

#### Run specific processes

You can also run specific processes seperately:

##### server
In dev, run `bin/server-start`. It sets up some needed env vars and
starts the server with lein. Or you can directly run `lein run -m kulu-backend.web 3001
` (takes defaults for the env vars).

Once started, visit [http://localhost:3001/](http://localhost:3001/)

#### In heroku

* Push to heroku with `git push heroku master`
* Interact on
  [kulu-backend.herokuapp.com](https://kulu-backend.herokuapp.com).
* To run migrations, `heroku run lein clj-sql-up migrate`

## Developer info

### Peculiarities in requests and responses

You may have noticed that our HTTP requests come in with
`under_scored` params but everywhere inside the app we use the
`kabab-case`. This is done with
`kulu-backend.utils.api/idiomatize-keys` and it's friends:

+ On incoming HTTP requests (at the handler level) we convert params
  to the `kabab-case` (`dasherize` them).
+ Once the response is ready, we convert the response body back to the
  `under_score` flavor.

See example [here](src/kulu_backend/handler.clj) and read the
[code documentation here](src/kulu_backend/utils/api.clj) for more
details.

Similarly, we apply the inverse transform (`under_scorize` on the way
in, `dasherize` on the way out) while sending data to SQL.

### Configuration

We use [nomad](https://github.com/james-henderson/nomad) to read our
app's configuration from edn files. The config related files are:

#### Database

In prod, we read the `DATABASE_URL` env var. The dev and test
databases are specified in the config. In test, every DB command is
run in a transaction and rolled back after the test, so we always a
clean slate.

The connection string in test comes from Nomad configs

(expand further)

#### AWS

We use seperate IAM accounts for dev and prod (we stub out AWS usage
in test).

See the AWS Console's IAM tab (for bills@nilenso.com) for the various
IAM accounts we use. Similarly we have separate S3 buckets and SQS
queues for dev/prod and IAM account's permissions make sure that the
dev AWS IAM account can't read/write from the prod resources and vice-versa.

#### Mailgun

TODO

#### ElasticSearch

###### OS X
+ `brew install elasticsearch`

+ Install the KOPF UI plugin to handle all ES administration: https://github.com/lmenezes/elasticsearch-kopf in `/usr/local/var/lib/elasticsearch/plugins`

+ `$ lein run -m kulu-backend.tasks setup dev` to migrate indices.

+ Run `elasticsearch`

###### Elsewhere

+ Go to http://www.elasticsearch.com/download/ and download the latest version of elasticsearch (assumingly as zip).

+ unzip it.

+ `chmod +x bin/elasticsearch`

+ `$ lein run -m kulu-backend.tasks setup dev` to migrate indices.

+ `bin/elasticsearch`

+ this starts a process that binds to port 9200.

+ `curl -X GET http://localhost:9200/`

to get something like:

    {
      "status" : 200,
      "name" : "Mop Man",
      "version" : {
        "number" : "1.3.4",
        "build_hash" : "a70f3ccb52200f8f2c87e9c370c6597448eb3e45",
        "build_timestamp" : "2014-09-30T09:07:17Z",
        "build_snapshot" : false,
        "lucene_version" : "4.9"
        },
      "tagline" : "You Know, for Search"
     }

+ Or install the KOPF UI in your plugins directory from: https://github.com/lmenezes/elasticsearch-kopf

### Tests

Run `lein test` to run tests. It will automatically set the `NOMAD_ENV=test` and pick up the right configuration.

Make sure you run `elasticsearch` before running tests, as some of the tests in the suite test end-to-end making real calls to ES on a test index.