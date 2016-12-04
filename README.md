# derby2pg

A Clojure toolkit for migrating Apache Derby databases to PostgreSQL

## Usage

FIXME

## TODO

* DONE Generate schema
** DONE create table sql
*** DONE table name
*** DONE column name
*** DONE column datatype
*** DONE nullability
*** DONE column default values
** DONE generate indexes
*** DONE single/multi column
*** DONE ascending/descending
*** DONE use postgres index names rather than derby names
** DONE generate constraints
*** DONE generate foreign key constraints
*** DONE generate unique primary key constraints
** DONE serial/bigserial generation
* DONE Enumerate tables
** DONE generate scheme for tables
** DONE generate copy statement for tables
** DONE generate indexes for tables
** DONE generate primary and keys for tables
** DONE generate sequences
* DONE Copy data
** DONE copy statement
** DONE all tuples
** DONE escape values in tuples
** DONE end of input indicator
** DONE FIX: exclude tables mechanism
* TODO document: unhandled aliases/synonyms
* TODO document: unhandled triggers/functions/procedures
* TODO document: unhandled views
* TODO document: unahndled check constraints
* TODO document: unhandled deferred constraints
* DONE document: unhandled xml data type
* TODO create a main and executable jar

## Notes

Derby data types known not to work, fully or partially

* XML -> Works for table generation, fails data export
* CLOB -> Works for table generation, fails data export

Derby data types not tested:

* BLOB
* CHAR FOR BIT DATA
* VARCHAR FOR BIT DATA
* LONG VARCHAR FOR BIT DATA
* User defined types

## License

Copyright © 2016 Bill Robertson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


