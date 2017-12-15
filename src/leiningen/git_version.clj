(ns leiningen.git-version
  ""
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(def default-config
  {:version-cmd    "git describe --match v*.* --abbrev=4 --dirty=**DIRTY**"
   :ref-cmd        "git rev-parse --verify HEAD"
   :msg-cmd        "git log -1 HEAD"
   :ts-cmd         "git log -1 --pretty=%ct"
   :root-ns        nil
   :assoc-in-keys  [[:version]]
   :filename       "version.clj"
   :tag-to-version #(apply str (rest %))})

(defn get-git-version
  ""
  [{:keys [version-cmd tag-to-version] :as config}]
  (let [cmd (str/split version-cmd #" ")]
    (tag-to-version (str/trim (:out (apply sh cmd))))))

(defn get-git-ref
  ""
  [{:keys [ref-cmd] :as config}]
  (let [cmd (str/split ref-cmd #" ")]
    (apply str (str/trim (:out (apply sh cmd))))))

(comment
  (get-git-ref {:git-version-cmd "git rev-parse"})
  (get-git-ref {}))

(defn get-git-last-message
  ""
  [{:keys [msg-cmd] :as config}]
  (let [cmd (str/split msg-cmd #" ")]
    (str/replace (apply str (str/trim
                             (:out (apply sh cmd))))
                 #"\"" "'")))

(comment
  (get-git-last-message {}))

(defn get-git-ts
  ""
  [{:keys [ts-cmd] :as config}]
  (let [cmd (str/split ts-cmd #" ")]
    (apply str (str/trim (:out (apply sh cmd))))))

(comment
  (get-git-ts {}))

(defn git-version
  "Show project version, as tagged in git."
  [project & args]
  (let [config (merge default-config (:git-version project))]
    (println "Version:" (:version project) "\n" (get-git-last-message config))))

(comment
  (get-git-version {:git-version {:version-cmd "git describe --abbrev=0"}})
  (get-git-version {}))
