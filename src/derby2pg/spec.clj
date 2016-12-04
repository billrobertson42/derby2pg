(ns derby2pg.spec)

(defn derby-spec[subname user password]
  {:subprotocol "derby"
   :subname subname
   :user user
   :password password})

(def dbs (derby-spec "/Users/bill/invpos/data/db" "dot" "C^1@?mg*LmCt"))

