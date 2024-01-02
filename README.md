# derby2pg

A Clojure toolkit for migrating Apache Derby databases to PostgreSQL

# Version 0.21.0

This version updates jar files to the latest versions, and also adds the
network client to the uberjar, so it should also work through the network
driver as well as local.

# Usage

## Prerequistites

First, you will need to have Java 21 installed. If you're runing Apache
Derby you probably already have that covered.

Java 21 is required for the latest version because the latest version
of Derby requires it.

## Download

Will attempt to make a release available for download on Github. Check the releases tab.

If wish to build the code yourself, please see the instructions in the Clone
and Build section below.

## Clone and Build

If you have already downlaod the jar file, you can skip this step. If
not, then follow the instructions below.

First, you must clone this repository from Git.

Second, you must install [Leiningen](http://leiningen.org/), and build
the jar file.  To do this, open the terminal or cmd prompt, cd to the 
project base directory (it contains `project.clj`).  Then issue the 
following command:

    lein uberjar

Which will create `derby2pg.jar` in the `target` sub-directory. For example:

    $ lein uberjar
    Compiling derby2pg.core
    Compiling derby2pg.core
    Created /Users/bill/dev/gt/code/derby2pg/target/derby2pg-0.1.0-SNAPSHOT.jar
    Created /Users/bill/dev/gt/code/derby2pg/target/derby2pg.jar

## Run the program

The program is run from the terminal or command prompt. 

    java -jar derby2pg.jar outfile-name jdbc-url schema-name include-data [tables to exclude]

Arguments

* `outfile-name` The name of the output file to generate. e.g. "output.sql"
* `jdbc-url` The jdbc url to use to connect to the Derby database to export
* `schema-name` The name of the schema in the Derby database to export. Must exactly match the case of the schema in the database, which is usually upper case.
* `include-data` Must be either `true` or `false` If `true`, the program will generate copy statements for the data in exported tables.
* [tables to exclude] *optional* list of tables to ignore by the exported. Must match the case name of the table in the database, which is usually upper case.

Example

    $ java -jar derby2pg.jar output.sql "jdbc:derby:/path/to/db;user=your_username;password=your_password" DOT true
    Generate create schema statement
    Generating tables
    Generating copy statements for SALE
    Generating copy statements for LINE_ITEM
    ...
    Generating copy statements for CUSTOMER
    Generating indexes
    Generating keys
    Generating auto increment sequences
    done

This generates a script that you can inspect, and possibly modify if
you wish. You should be able to run this script in psql or pgadmin.

Example

    $ psql

    bill=# \i output.sql
    CREATE SCHEMA
    CREATE TABLE
    CREATE TABLE
    ...
    CREATE TABLE
    COPY 11
    COPY 10000
    ...
    COPY 10000
    COPY 243
    CREATE INDEX
    CREATE INDEX
    ...
    CREATE INDEX
    ALTER TABLE
    ALTER TABLE
    ...
    ALTER TABLE
    
    CREATE SEQUENCE
    ALTER TABLE
    ALTER SEQUENCE
     setval 
    --------
         17
    (1 row)
    ...
    bill=#

## Notes

### Data Types

* Derby string types become `text` columns in the output script
* Derby numeric types are identical in the output script, e.g. a `bigint` column in the Derby database will be a `bigint` column in the output script
* Derby `time` and `date` columns are identical in the output script
* Derby `timestamp` columns are translated to PostgreSQL `timestamp with time zone` columns

### Timezone handling

The Derby `timestamp` data type does not have a timezone. To
accomodate this when translating to a timestamp column with a time
zone, the program adds the JVM's default timezone when creating the
copy statements. The JVM's default timezone usually defaults to your
computer's timezone.

To change this behavior, you simply need to configure the JVM's
default timezone with a system property when running the program.  You
should use full timezone name in the TZ column
(here)[https://en.wikipedia.org/wiki/List_of_tz_database_time_zones]. e.g. `America/New_York`
or `GMT`. To do this, you will need to pass a `-Duser.timezone=XXX`
argument to the JVM. For example:

    java -Duser.timezone=GMT -jar derby2pg.jar output.sql "jdbc:derby:/path/to/db;user=your_username;password=your_password" DOT true

If you only ever used Derby on machines that were all on the same time
zone, and you code has no special accomodations for dealing with time
zones, you will probably be fine with the default timestamp behavior.

### Partially supported data types

These Derby data types are known not to work. The program should
be able to create tables for these columns but the export does not work. 
This might be addressable by adding an appropriate function to
derby2pg.data/data-formatter function.

* XML -> Works for table generation, fails data export.
* CLOB -> Works for table generation, fails data export

### Derby data types not tested

* BLOB
* CHAR FOR BIT DATA
* VARCHAR FOR BIT DATA
* LONG VARCHAR FOR BIT DATA
* User defined types

### Derby Features not supported

The program will not export the following entities.

* aliases/synonyms
* triggers/functions/procedures
* views
* check constraints
* deferred constraints

## License

Copyright © 2016-2023 Bill Robertson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


