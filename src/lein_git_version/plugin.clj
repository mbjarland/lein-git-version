(ns lein-git-version.plugin
  ""
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.git-version :refer :all])
  (:import [java.io File]
           [java.util.regex Pattern]))

(defn middleware
  ""
  [{:keys           [git-version name root]
    project-version :version
    :as             project}]
  (let [{:keys [path root-ns set-version] :as config} (merge default-config git-version)
        
        fs            File/separator
        fsp           (Pattern/quote fs)
        version       (get-git-version config) 
        git-ref       (get-git-ref config)
        git-ref-short (.substring git-ref 0 7)]
    (cond-> (-> project
                (assoc :git-ref git-ref
                       :git-ref-short git-ref-short))
      (or (= set-version :git-ref-short)
          (= project-version :project/git-ref-short))
      (assoc :version git-ref-short)

      (or (= set-version :git-ref)
          (= project-version :project/git-ref))
      (assoc :version git-ref))))
