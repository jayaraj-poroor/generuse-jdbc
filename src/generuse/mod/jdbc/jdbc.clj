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
   	(:use [generuse.lib.exec :only (deref-eval)])	
   	)
)

;TODO
(def pick_ {:name "pick" :target-type :db_sql})
(defn ^{:axon pick_} pick[target-eval param-evals 
											ctx globals & more]
	{:value true :type :boolean}
)

