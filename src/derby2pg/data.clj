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

(defn row-data [columns tuple]
  (let [row-data (map tuple columns)]
    (str/join "\t" (map escape row-data))))

(defn copy-data-sql[schema table-name columns rows output-writer]
  (when (seq rows)
    (.write output-writer 
            (str "copy " schema "." table-name " ("
                 (str/join ", " (map name columns))
                 ") from stdin;\n"))
    (doseq [row (take 1000 rows)]
      (.write output-writer (str (row-data columns row) "\n")))
    (.write output-writer "\\.\n\n")
    (recur schema table-name columns (drop 1000 rows) output-writer)))

(defn create-copy-sql [derby-spec schema table-columns output-writer]
  (let [table-name (:tablename (first table-columns))
        columns (map (comp keyword str/lower-case) (map :columnname table-columns))
        query (str "select " (str/join ", " (map name columns)) "\n"
                   "from " schema "." table-name)]
    (with-open [conn (connect/create derby-spec)
                cursor (jdbc/fetch-lazy conn query)]
      (copy-data-sql schema table-name columns (jdbc/cursor->lazyseq cursor) output-writer))))
    
                                      
