(defproject me.arrdem/lein-git-version "_"
  :description "Use git for project versions."
  :url "https://github.com/arrdem/lein-git-version"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :eval-in-leiningen true
  :deploy-repositories [["releases" :clojars]]

  :plugins [[me.arrdem/lein-git-version "LATEST"]]
  :git-version
  ,,{:status-to-version
     ,,(fn [{:keys [tag version branch ahead ahead? dirty?] :as git}]
         (assert (re-find #"\d+\.\d+\.\d+" tag)
                 "Tag is assumed to be a raw SemVer version")
         (if (and tag (not ahead?) (not dirty?))
           tag
           (let [[_ prefix patch] (re-find #"(\d+\.\d+)\.(\d+)" tag)
                 patch            (Long/parseLong patch)
                 patch+           (inc patch)]
             (format "%s.%d-%s-SNAPSHOT" prefix patch+ branch))))
     }
  )
