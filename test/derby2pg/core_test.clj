(ns derby2pg.core-test
  (:require [jdbc.core :as jdbc]
            [derby2pg.connect :as connect]
            [derby2pg.core :as core]
            [derby2pg.table :as table]
            [derby2pg.data :as data]
            [derby2pg.index :as index]
            [derby2pg.key :as key]
            [derby2pg.sequence :as sequence]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  )

;; integration test that covers most of the code

(def dbspec "jdbc:derby:memory:derby2pg")

(def setup_sql
  ["create schema foo"
   "create table foo.bar (
     integer_col int primary key not null generated by default as identity,
     smallint_col smallint not null,
     bigint_col bigint unique,     
     double_col double precision,
     real_col real,
     decimal_1_col numeric(10),
     decimal_2_col decimal(10,2),    
     date_col date default current_date,
     time_col time,
     timestamp_col timestamp,     
     varchar_col varchar(10),
     long_varchar_col long varchar,
     char_col char(10))"
   "insert into foo.bar (smallint_col, bigint_col, double_col,
                         real_col, decimal_1_col, decimal_2_col, 
                         date_col, time_col, timestamp_col, 
                         varchar_col, long_varchar_col, char_col)
                 values (1, 2, 3, 
                         4, 5, 6, 
                         '2016-12-01', '4:00', '1960-01-01 23:03:20', 
                         'tc', 'lvc', 'char\tx')"
   "create index an_index on foo.bar(timestamp_col)"
   "create table foo.bar2 (z integer)"
   "alter table foo.bar2 add constraint moogie foreign key(z) references foo.bar(integer_col)"

])

(defn test-with-database [f]
  (try
    (with-open [conn (connect/create (str dbspec ";create=true"))]
      (doseq [statement setup_sql]
        (jdbc/execute conn statement)))
    (f)
    (finally
      (try
        (with-open [conn (connect/create (str dbspec ";drop=true"))])
        (catch Exception x)))))

(use-fixtures :once test-with-database)

(deftest tests
  (let [td (table/get-table-data dbspec "FOO")]
    (is (= 2 (count td)))
    (let [table-sql (table/create-table-sql "FOO" (td "BAR"))]
      (is (= [{:autoincrementstart 1, :columndatatype :integer, :scale 0, :precision 10, 
               :max-width 4, :tablename "BAR", :columnname "INTEGER_COL", 
               :autoincrementinc 1, :nullable false, :text false} 
              {:tablename "BAR", :columnname "SMALLINT_COL", :columndatatype :smallint, 
               :text false, :max-width 2, :precision 5, :scale 0, :nullable false} 
              {:tablename "BAR", :columnname "BIGINT_COL", :columndatatype :bigint, 
               :text false, :max-width 8, :precision 19, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "DOUBLE_COL", :columndatatype :double, 
               :text false, :max-width 8, :precision 52, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "REAL_COL", :columndatatype :real, 
               :text false, :max-width 4, :precision 23, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "DECIMAL_1_COL", :columndatatype :numeric, 
               :text false, :max-width 11, :precision 10, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "DECIMAL_2_COL", :columndatatype :decimal, 
               :text false, :max-width 12, :precision 10, :scale 2, :nullable true} 
              {:tablename "BAR", :columnname "DATE_COL", :columndatatype :date, 
               :columndefault "current_date" :text false, :max-width 10, 
               :precision 10, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "TIME_COL", :columndatatype :time, 
               :text false, :max-width 8, :precision 8, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "TIMESTAMP_COL", :columndatatype :timestamp, 
               :text false, :max-width 29, :precision 29, :scale 9, :nullable true} 
              {:tablename "BAR", :columnname "VARCHAR_COL", :columndatatype :varchar, 
               :text true, :max-width 10, :precision 0, :scale 0, :nullable true} 
              {:tablename "BAR", :columnname "LONG_VARCHAR_COL", :columndatatype 
               :long-varchar, :text true, :max-width 32700, :precision 0, :scale 0, 
               :nullable true} 
              {:tablename "BAR", :columnname "CHAR_COL", :columndatatype :char, 
               :text true, :max-width 10, :precision 0, :scale 0, :nullable true}]
             (td "BAR")))
      (is (= [{:tablename "BAR2", :columnname "Z", :columndatatype :integer, :text false, :max-width 4, :precision 10, :scale 0, :nullable true}]
             (td "BAR2")))
      (is (= "create table FOO.BAR (\n    INTEGER_COL integer not null,\n    SMALLINT_COL smallint not null,\n    BIGINT_COL bigint,\n    DOUBLE_COL double precision,\n    REAL_COL real,\n    DECIMAL_1_COL numeric (10),\n    DECIMAL_2_COL numeric (10, 2),\n    DATE_COL date default current_date,\n    TIME_COL time,\n    TIMESTAMP_COL timestamp with time zone,\n    VARCHAR_COL text,\n    LONG_VARCHAR_COL text,\n    CHAR_COL text\n);\n" table-sql)))

    (let [buffer (java.io.StringWriter.)
          _ (data/create-copy-sql dbspec "FOO" (td "BAR") buffer)
          copy-sql (str buffer)
          tz (System/getProperty "user.timezone")]
      (is (= (str "copy FOO.BAR (integer_col, smallint_col, bigint_col, double_col, real_col, decimal_1_col, decimal_2_col, date_col, time_col, timestamp_col, varchar_col, long_varchar_col, char_col) from stdin;\n1\t1\t2\t3.0\t4.0\t5\t6.00\t2016-12-01\t04:00:00\t1960-01-01 23:03:20.0 " tz "\ttc\tlvc\tchar\\tx    \n\\.\n\n") copy-sql)))
    
    (let [id (index/get-index-data dbspec "FOO")
          id-sql (index/create-index-sql "FOO" td (first id))]
      (is (= [{:tablename "BAR", :indexname "AN_INDEX", :unique false, :column-indexes [9], :column-sort-ascending [true]}] id))
      (is (= "create index on FOO.BAR(TIMESTAMP_COL); -- AN_INDEX" id-sql)))

    (let [kd (key/get-key-data dbspec "FOO")
          kd-sql (key/create-key-sql "FOO" td (first kd))
          kd1 {:type "U", :tablename "BAR", :column-indexes [2]} 
          kd2 {:type "P", :tablename "BAR", :column-indexes [0]} 
          kd3 {:type "F", :tablename "BAR2", :deleterule "R", :updaterule "R", :ref_tablename "BAR", :column-indexes [0], :ref-column-indexes [0]}
          ]
      (is (= kd1 (first (filter #(= (:type %) "U") kd))))
      (is (= kd2 (first (filter #(= (:type %) "P") kd))))
      (is (= kd3 (first (filter #(= (:type %) "F") kd))))

      (is (= "alter table FOO.BAR add constraint bar_key_bigint_col unique(BIGINT_COL);"
             (key/create-key-sql "FOO" td kd1)))
      (is (= "alter table FOO.BAR add constraint bar_key primary key(INTEGER_COL);"
             (key/create-key-sql "FOO" td kd2)))
      (is (= "alter table FOO.BAR2 add constraint bar2_bar_z_fk foreign key(Z) references FOO.BAR(INTEGER_COL);"
             (key/create-key-sql "FOO" td kd3))))

    (is (= "create sequence FOO.BAR_INTEGER_COL_SEQ;\nalter table FOO.BAR alter column INTEGER_COL set default nextval('FOO.BAR_INTEGER_COL_SEQ');\nalter sequence FOO.BAR_INTEGER_COL_SEQ owned by FOO.BAR.INTEGER_COL;\nselect setval('FOO.BAR_INTEGER_COL_SEQ', 1);\n"
           (sequence/create-auto-inc-sql dbspec "FOO" (get-in td ["BAR" 0]))))

    (let [test-file  (io/file "test.sql")]
      (when (.exists test-file) (is (.delete test-file)))
      (core/-main "test.sql" dbspec "FOO" "true")
      (is (.exists test-file)))

    ))
    
