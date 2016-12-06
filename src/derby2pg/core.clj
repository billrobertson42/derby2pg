(ns derby2pg.core
  (:require [clojure.java.io :as io]
            [derby2pg.table :refer [create-table-sql get-table-data]]
            [derby2pg.data :refer [create-copy-sql]]
            [derby2pg.index :refer [create-index-sql get-index-data]]
            [derby2pg.key :refer [create-key-sql get-key-data]]
            [derby2pg.sequence :refer [create-auto-inc-sql]])
  (:import [org.apache.derby.catalog.types DefaultInfoImpl TypeDescriptorImpl])
  (:gen-class))

(defn generate-script [file-name derby-spec schema-name include-data & exclude-tables]
  (let [table-data (apply get-table-data derby-spec schema-name exclude-tables)
        table-sql (map #(create-table-sql schema-name %) (vals table-data))]

    (spit file-name (str "-- generated at " (java.util.Date.) 
                         "\n\n--tables"))

    (println "Generate create schema statement")
    (spit file-name (str "create schema " schema-name ";\n\n"))

    (println "Generating tables")
    (doseq [create table-sql]
      (spit file-name (str "\n" create) :append true))

    (if include-data
      (with-open [f (io/writer file-name :append true)]
        (.write f "\n--copy statements \n")
        (doseq [t (vals table-data)]
          (println "Generating copy statements for" (:tablename (first t)))
          (create-copy-sql derby-spec schema-name t f))))

    (println "Generating indexes")
    (spit file-name "\n-- indexes\n\n" :append true)
    (let [index-data (apply get-index-data derby-spec schema-name exclude-tables)]
      (doseq [sql (map #(create-index-sql schema-name table-data %) index-data)]
        (spit file-name (str sql "\n\n") :append true)))

    (println "Generating keys")
    (spit file-name "\n-- keys\n\n" :append true)
    (let[key-data (apply get-key-data derby-spec schema-name exclude-tables)]
      (doseq [sql (map #(create-key-sql schema-name table-data %) key-data)]
        (spit file-name (str sql "\n\n") :append true)))

    (println "Generating auto increment sequences")
    (spit file-name "\n-- auto increment sequences\n\n" :append true)
    (doseq [sql (map #(create-auto-inc-sql derby-spec schema-name %) 
                     (filter :autoincrementinc (apply concat (vals table-data))))]
      (spit file-name (str sql "\n\n") :append true))

    (println "done")
    (spit file-name (str "-- completed at " (java.util.Date.)) :append true)

    ))

(defn -main [& args]
  (let [args (vec args)
        file-name (args 0)
        derby-spec (args 1)
        schema-name (args 2)
        include-data (Boolean/parseBoolean (args 3))
        exclude-tables (drop 4 args)]
    (apply generate-script file-name derby-spec schema-name include-data exclude-tables)))
