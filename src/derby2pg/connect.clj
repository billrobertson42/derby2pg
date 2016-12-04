(ns derby2pg.connect
  (:require [jdbc.core :as jdbc])
  (:import [java.sql DriverManager]))

(defn create[db-url]
  (jdbc/connection (DriverManager/getConnection db-url)))
