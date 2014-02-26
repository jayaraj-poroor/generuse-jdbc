; Copyright (c) Jayaraj Poroor. All rights reserved.
; The use and distribution terms for this software are covered by the
; GNU Lesser General Public License 3.0 
; (http://www.gnu.org/copyleft/lesser.html)
; which can be found in the file lgpl-3.0.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns generuse.mod.jdbc
    (:gen-class)
    (:use       [generuse.lib.exec      :only (deref-eval)]
                [clojure.set            :only (union)])
    (:import    (java.util Random))
    (:require   [clojure.string         :as str] 
                [clj-dbcp.core          :as dbcp]
                [clojure.java.jdbc      :as jdbc]
    )    
)     

(defn create-pool[poolmin poolmax partitioncount connurl]
    (dbcp/make-datasource (union (dbcp/parse-url connurl) {:init-size poolmin :max-active poolmax}))
)

(defn get-random-row [rs] 
      (def rnd (Random. (System/currentTimeMillis)))
    (let [is-allowed? (fn [w] (> (.nextDouble rnd) 0.5))] 
         (def x (filter is-allowed? rs))
         (if (not-empty x) (first x) (first rs))
    )
)

(defn get-initialized-db-obj [dbname globals]
    (let [dbentry (@globals dbname)]
        (when (not (@globals dbname))
            (throw (ex-info 
                        (str "Database object not found in global heap: "
                              dbname
                        )
                    )
            )
        )
        (if (string? (:value @dbentry))
            (let [connurl (:value @dbentry)
                  poolmin 10
                  poolmax 50
                  connpool (create-pool poolmin poolmax 3 connurl)
                  newvalue  {:connurl connurl :connpool connpool}
                 ]
                 ;(dosync (alter dbentry :value newvalue))
                 (dosync (ref-set dbentry {:value newvalue}))
                 newvalue
            )
            (do
                (assert (map? (:value @dbentry)))
                (:value @dbentry)
            )
        )
    )
)

(defn get-connection-from-pool [target-eval globals]
    (let [init-val (:value (deref-eval target-eval))
          fields   (when init-val (str/split init-val #":"))           
          db       (when (= (count fields) 2) (fields 0))
          table    (when (= (count fields) 2) (fields 1))
          objstr   (str/join "'s" (:objref target-eval))
         ]
         (when (or (not db) (not table))
            (throw (ex-info (str "Initial value for " objstr 
                                 " not specified properly. " 
                                 "Must be dbname:tablename")
                    )
            )
         )
         {:db db, :table table  :conn {:datasource (:connpool (get-initialized-db-obj db globals))}}
    )
)

(def pick-any_ {:names ["pick-any"] :target-type :sql_table})
(defn ^{:axon pick-any_} pick-any[target-eval param-evals ctx globals & more]
    (let [dbinfo (get-connection-from-pool target-eval globals)]
         (get-random-row (jdbc/query (:conn dbinfo) [(str "select * from `" (:table dbinfo) "`")]))
    )
)

(def pick_ {:names ["pick"] :target-type :sql_table})
(defn ^{:axon pick_} pick[target-eval param-evals ctx globals & more]
    (let [dbinfo (get-connection-from-pool target-eval globals)
          constraint-maker (fn[param] (str (param 0) " = '" (:value @(:value (param 1))) "' and " ))
         ]
         (jdbc/query (:conn dbinfo) [(str "select * from `" (:table dbinfo) "` where " (apply str (map constraint-maker (deref-eval param-evals))) " '1' = '1'")])
    )
)