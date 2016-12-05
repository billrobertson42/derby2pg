(ns derby2pg.key
  (:require [jdbc.core :as jdbc]
            [clojure.string :as str]
            [derby2pg.connect :as connect]
            [derby2pg.index :refer [format-referenced-columns]]))

(defn get-non-fkey-data[derby-spec schema]
  (let [query "select cs.type, tab.tablename, cg.descriptor
               from   sys.sysconstraints cs
               join   sys.syskeys k on cs.constraintid = k.constraintid
               join   sys.sysconglomerates cg on cg.conglomerateid = k.conglomerateid
               join   sys.systables tab on cs.tableid = tab.tableid
               join   sys.sysschemas sch on cs.schemaid = sch.schemaid
               where  cs.state = 'E' and
                      sch.schemaname=? and 
                      cg.isconstraint = true
               order by tab.tablename"]
    (with-open [conn (connect/create derby-spec)]
      (jdbc/fetch conn [query schema]))))

(defn get-fkey-data[derby-spec schema]
  (let [query "select cs.type, tab.tablename, cg.descriptor,
                      k.deleterule, k.updaterule,
                      SCG.DESCRIPTOR REF_DESCRIPTOR,
                      STAB.TABLENAME REF_TABLENAME
               from sys.sysconstraints cs
               join sys.sysforeignkeys k on cs.constraintid = k.constraintid
               join sys.sysconglomerates cg on k.conglomerateid = cg.conglomerateid
               join sys.systables tab on cs.tableid = tab.tableid
               join sys.sysschemas sch on cs.schemaid = sch.schemaid
               JOIN SYS.SYSKEYS SK ON SK.CONSTRAINTID = K.KEYCONSTRAINTID
               JOIN SYS.SYSCONGLOMERATES SCG on SK.CONGLOMERATEID = SCG.CONGLOMERATEID
               JOIN SYS.SYSTABLES STAB on SCG.TABLEID = STAB.TABLEID               
               where cs.state = 'E' and 
                     sch.schemaname = ? and
                     cg.isconstraint = true
               order by tab.tablename"]
    (with-open [conn (connect/create derby-spec)]
      (jdbc/fetch conn [query schema]))))

(defn key-descriptor[key-tuple]
  (let [^org.apache.derby.catalog.IndexDescriptor descriptor (:descriptor key-tuple)
        ^org.apache.derby.catalog.IndexDescriptor ref-descriptor (:ref_descriptor key-tuple)
        key-tuple (-> key-tuple
                      (dissoc :descriptor)
                      (dissoc :ref_descriptor)
                      (assoc :column-indexes 
                             (mapv dec (seq (.baseColumnPositions descriptor)))))]
    (if ref-descriptor
        (assoc key-tuple :ref-column-indexes 
               (mapv dec (seq (.baseColumnPositions ref-descriptor))))
        key-tuple)))

(defn get-key-data [derby-spec schema & exclude-tables]
  (let [exclude-tables  (if exclude-tables (apply hash-set exclude-tables) #{})]
    (map key-descriptor
         (remove #(exclude-tables (:tablename %))
                 (concat (get-non-fkey-data derby-spec schema)
                         (get-fkey-data derby-spec schema))))))

(defn format-constraint-type [key-data]
  (condp = (:type key-data)
    "P" "primary key"
    "U" "unique"
    "F" "foreign key"))

(defn format-rule [operation rule]
  (condp = rule
    "S" (str " on " operation " restrict")
    "R" ""
    "U" (str " on " operation " set null")
    "C" (str " on " operation " cascade")))

(defn format-fk-reference [schema referenced-columns key-datum]
  (if (= "F" (:type key-datum))
    (let [fk-ref {:column-indexes (:ref-column-indexes key-datum)
                  :tablename (:ref_tablename key-datum)}]
      (str " references " schema "." (:ref_tablename key-datum) 
           (format-referenced-columns referenced-columns fk-ref)
           (format-rule "update" (:updaterule key-datum))
           (format-rule "delete" (:deleterule key-datum))))))

(defn constraint-name [table-columns key-datum]
  (let [columns (mapv :columnname table-columns)
        ref-columns (mapv :columnname table-columns)]
    (condp = (:type key-datum)
      "U" (str/lower-case 
           (str (:tablename key-datum) "_key_"
                (str/join "_" (map columns (:column-indexes key-datum)))))

      "P" (str/lower-case
           (str (:tablename key-datum) "_key"))
      
      "F" (str/lower-case
           (str (:tablename key-datum) "_" (:ref_tablename key-datum) "_"
                (str/join "_" (map columns (:column-indexes key-datum))) "_fk"))
      )))

(defn create-key-sql [schema table-data key-datum]
  (let [table-columns (table-data (:tablename key-datum))
        referenced-columns (if (= "F" (:type key-datum)) 
                             (table-data (:ref_tablename key-datum))
                             [])]

    (str "alter table " schema "." (:tablename key-datum) " add constraint "
         (constraint-name table-columns key-datum) " "
         (format-constraint-type key-datum) 
         (format-referenced-columns table-columns key-datum)
         (format-fk-reference schema referenced-columns key-datum) ";"
         )))
       
