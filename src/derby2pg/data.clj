(ns derby2pg.data
  (:require [jdbc.core :as jdbc]
            [clojure.string :as str]
            [derby2pg.connect :as connect])
  (:import [org.apache.derby.catalog.types DefaultInfoImpl TypeDescriptorImpl])
)

(defn escape [value]
  (if value
    (-> value str
        (.replace "\\" "\\\\") (.replace "\b" "\\b") (.replace "\f" "\\f") 
        (.replace "\n" "\\n") (.replace "\r" "\\r") (.replace "\t" "\\t") 
        (.replace "\u000b" "\\v"))
    "\\N"))

(defn row-data [columns data-formatters tuple]
  (let [raw-data (map tuple columns)
        fmt-data (map-indexed (fn [i val] ((data-formatters i) val)) raw-data)]
    (str/join "\t" fmt-data)))

(defn copy-data-sql[schema table-name columns data-formatters rows output-writer]
  (when (seq rows)
    (.write output-writer 
            (str "copy " schema "." table-name " ("
                 (str/join ", " (map name columns))
                 ") from stdin;\n"))
    (doseq [row (take 1000 rows)]
      (.write output-writer (str (row-data columns data-formatters row) "\n")))
    (.write output-writer "\\.\n\n")
    (recur schema table-name columns data-formatters (drop 1000 rows) output-writer)))

(defn data-formatter [table-column]
  (cond
    (= :timestamp (:columndatatype table-column))
    (fn [value] (escape (if value (str value " " (System/getProperty "user.timezone")))))
    :else
    escape))

(defn create-copy-sql [derby-spec schema table-columns output-writer]
  (let [table-name (:tablename (first table-columns))
        columns (map (comp keyword str/lower-case) (map :columnname table-columns))
        data-formatters (mapv data-formatter table-columns)
        query (str "select " (str/join ", " (map name columns)) "\n"
                   "from " schema "." table-name)]
    (with-open [conn (connect/create derby-spec)
                cursor (jdbc/fetch-lazy conn query)]
      (copy-data-sql schema table-name columns data-formatters 
                     (jdbc/cursor->lazyseq cursor) output-writer))))
    
                                      
