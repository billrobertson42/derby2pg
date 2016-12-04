(ns derby2pg.sequence)

(defn create-auto-inc-sql [schema column]
  (let [seq-name (str schema \. (:tablename column) \_ (:columnname column) "_SEQ")
        table-name (str schema \. (:tablename column))]
    (str "create sequence " seq-name ";\n"
         "alter table " table-name " alter column " (:columnname column) 
         " set default nextval('" seq-name "');\n"
         "alter sequence " seq-name " owned by " table-name \. (:columnname column) ";\n"
         "select setval('" seq-name "', " (:autoincrementstart column) ");\n")))
