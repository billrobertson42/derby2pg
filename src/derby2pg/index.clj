(ns derby2pg.index
  (:require [jdbc.core :as jdbc]
            [derby2pg.connect :as connect]
            [clojure.string :as str]))

(defn index-descriptor[index-tuple]
  (let [^org.apache.derby.catalog.IndexDescriptor descriptor (:descriptor index-tuple)]
    (-> index-tuple
        (dissoc :descriptor)
        (assoc :unique (.isUnique descriptor)) ;; double check vs db_index ALL
        (assoc :column-indexes (mapv dec (seq (.baseColumnPositions descriptor))))
        (assoc :column-sort-ascending (vec (seq (.isAscending descriptor)))))))
        
(defn get-index-data[derby-spec schema & exclude-tables]
  (let [query "select tablename, conglomeratename indexname, descriptor
               from sys.sysconglomerates 
               join sys.systables tab using(tableid)
               join sys.sysschemas sch on sys.sysconglomerates.schemaid = sch.schemaid
               where schemaname = ? and 
                     isindex = true and
                     isconstraint = false
               order by tableid"
        exclude-tables (if exclude-tables (apply hash-set exclude-tables) #{})]
    (with-open [conn (connect/create derby-spec)]
      (map index-descriptor
           (remove #(exclude-tables (:tablename %))
                   (jdbc/fetch conn [query schema]))))))

(defn format-index-column[table-data index-datum ordinal]
  (let [column-index ((:column-indexes index-datum) ordinal)
        ascending (and (:column-sort-ascending index-datum) 
                       ((:column-sort-ascending index-datum) ordinal))]
  (str (:columnname (table-data column-index))
       (if (or ascending (nil? ascending)) "" " desc"))))       

(defn format-referenced-columns [table-data index-datum]
  (str "(" (str/join ", " (map #(format-index-column table-data index-datum %) 
                               (range 0 (count (:column-indexes index-datum))))) ")" ))

(defn create-index-sql [schema-name table-data index-datum]
  (let [tablename (:tablename index-datum)
        table-data (table-data tablename)]
    (str "create " (if (:unique index-datum) "unique " "") "index on " 
         schema-name "." tablename
         (format-referenced-columns table-data index-datum)
         "; -- " (:indexname index-datum)
         )))
  
