(ns leiningen.git-version
  ""
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(def default-config
  {:git            "git"
   :tag-to-version nil})

(def git-dirty-pattern
  #"(?<tag>.*)-(?<ahead>\d+)-g(?<ref>[0-9a-f]*)(?<snapshot>(-dirty)?)")

(defn get-git-ref
  ""
  [{:keys [git] :as config}]
  (let [cmd [git "rev-parse" "--verify" "HEAD"]]
    (:out (apply sh cmd))))

(defn git-ref-message
  ""
  [{:keys [git] :as config} ref-or-sha]
  (let [cmd [git "log" "-1" ref-or-sha]]
    (:out (apply sh cmd))))

(defn git-ref-ts
  ""
  [{:keys [git] :as config} ref-or-sha]
  (let [cmd [git "log" "-1" "--pretty=%ct" ref-or-sha]]
    (str/trim (:out (apply sh cmd)))))

(defn git-status
  "Fetch the current git status."
  [{:keys [git] :as config}]
  (let [buff                       (:out (apply sh [git "describe" "--tags" "--dirty" "--long"]))
        _                          (println "debug]" (pr-str buff))
        [_ tag ahead ref dirty? _] (or (re-find git-dirty-pattern buff)
                                       [nil nil nil true])
        dirty?                     (not= "" dirty?)]
    (cond-> {:tag       tag
             :ahead     (Integer/parseInt ahead)
             :ahead?    (not= ahead "0")
             :ref       (get-git-ref config)
             :ref-short ref}
      dirty?       (assoc :dirty? true)
      (not dirty?) (assoc :message (git-ref-message config "HEAD"))
      (not dirty?) (assoc :timestamp (git-ref-ts config "HEAD")))))
