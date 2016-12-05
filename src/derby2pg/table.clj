(ns derby2pg.table
  (:require [jdbc.core :as jdbc]
            [clojure.string :as str]
            [derby2pg.connect :as connect])
  (:import [org.apache.derby.catalog.types DefaultInfoImpl TypeDescriptorImpl])
)

(defn column-default[tuple]
  (if-let [^DefaultInfoImpl dd (:columndefault tuple)]
    (if-not (.isDefaultValueAutoinc dd)
      (-> tuple
          (update :columndefault str)
          (dissoc :autoincrementinc :autoincrementstart))
      (dissoc tuple :columndefault))
    (dissoc tuple :columndefault :autoincrementinc :autoincrementstart)))

(defn column-datatype [tuple]
  (let [^TypeDescriptorImpl td (:columndatatype tuple)]
    (assoc tuple 
           :columndatatype (-> (.getTypeName td) .toLowerCase (.replaceAll " " "-") keyword)
           :text (.isStringType td)
           :max-width (.getMaximumWidth td)
           :precision (.getPrecision td)
           :scale (.getScale td)
           :nullable (.isNullable td))))           

(defn get-table-data
  "Return seq of table data for the given schema"
  [derby-spec schema & exclude-tables]
  (let [query "select tab.tablename, col.columnname, col.columndatatype, 
                      col.columndefault, col.autoincrementinc, col.autoincrementstart
               from sys.sysschemas sch
               join sys.systables tab using(schemaid)
               join sys.syscolumns col on tab.tableid = col.referenceid
               where sch.schemaname = ? and
                     tab.tabletype = 'T'
                     order by tab.tablename, col.columnnumber"
        exclude-tables (if exclude-tables (apply hash-set exclude-tables) #{})]
    (with-open [conn (connect/create derby-spec)]
      (group-by :tablename
                (map (comp column-datatype column-default)
                     (remove #(exclude-tables (:tablename %))
                             (jdbc/fetch conn [query schema])))))))

(defn not-null-clause [column]
  (if-not (:nullable column)
    " not null"))

(defn default-clause [column]
  (if (:columndefault column)
    (str " default "
         (if (:text column) "'" "")
         (:columndefault column)
         (if (:text column) "'" ""))))

(defn format-numeric-column[column]
  (str "    " (:columnname column) " numeric ("
       (if (not= 0 (:scale column))
         (str (:precision column) ", " (:scale column))
         (:precision column)) ")" 
       (not-null-clause column) (default-clause column)))

(defn format-generic-column[column]
  (let [datatype (:columndatatype column)]
    (str "    " (:columnname column) " " 
         (cond 
           (= (:columndatatype column) :timestamp) "timestamp with time zone"
           (= (:columndatatype column) :double) "double precision"
           (or (:text column) (= (:columndatatype column) :xml)) "text"           
           :else (name datatype))
         (not-null-clause column) 
         (default-clause column))))

(defn format-column [column]
  (if (#{:numeric :decimal} (:columndatatype column))
    (format-numeric-column column)
    (format-generic-column column)))

(defn create-table-sql [schema table-columns]
  (let [table-name-clause 
        (str "create table " schema "." (:tablename (first table-columns)) " (\n")
        columns-clause (str (str/join ",\n" (map format-column table-columns)) "\n);\n")]
    (str table-name-clause columns-clause)))
