(ns derby2pg.sequence
  (:require [derby2pg.connect :as connect]
            [jdbc.core :as jdbc])
  )

(defn get-column-max-value [dbspec schema column]
  (with-open [conn (connect/create dbspec)]
    (let [query (str "select max(" (:columnname column) ") max_val from " schema "."
                     (:tablename column))]
      (or (:max_val (first (jdbc/fetch conn query))) 1))))

(defn create-auto-inc-sql [dbspec schema column]
  (let [seq-name (str schema \. (:tablename column) \_ (:columnname column) "_SEQ")
        table-name (str schema \. (:tablename column))]
    (str "create sequence " seq-name ";\n"
         "alter table " table-name " alter column " (:columnname column) 
         " set default nextval('" seq-name "');\n"
         "alter sequence " seq-name " owned by " table-name \. (:columnname column) ";\n"
         "select setval('" seq-name "', " (get-column-max-value dbspec schema column) ");\n")))
