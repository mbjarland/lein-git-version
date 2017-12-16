(ns lein-git-version.plugin
  ""
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.git-version :refer [default-config git-status]])
  (:import [java.io File]
           [java.util.regex Pattern]))

(defn middleware
  ""
  [{:keys           [git-version name root]
    project-version :version
    :as             project}]
  (let [{:keys [path root-ns set-version status-to-version] :as config} (merge default-config git-version)        
        {:keys [tag version ahead ahead? ref ref-short] :as status}     (git-status config)
        status                                                          (dissoc status :version)
        
        status-to-version (when status-to-version
                            (try (eval status-to-version)
                                 (catch Exception e
                                   (printf "While trying to evaluate status-to-version function:\n%s\n\nEncountered exception:\n%s\n"
                                           status-to-version e)
                                   (System/exit 1))))]
    (cond-> (merge project status)
      (or (= set-version :git-ref-short)
          (= project-version :project/git-ref-short))
      (assoc :version ref-short)

      (or (= set-version :git-ref)
          (= project-version :project/git-ref))
      (assoc :version ref)

      status-to-version
      (assoc :version (status-to-version status)))))
