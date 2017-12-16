(defproject me.arrdem/lein-git-version "0.1.0"
  :description "Use git for project versions."
  :url "https://github.com/arrdem/lein-git-version"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :eval-in-leiningen true
  :deploy-repositories [["releases" :clojars]]

  :plugins [[me.arrdem/lein-git-version "LATEST"]]
  :git-version {:status-to-version
                (fn [{:keys [tag version ahead ahead? dirty?]}]
                  (if (and tag (not ahead?))
                    tag
                    (str tag "-" ahead (when dirty? "-SNAPSHOT"))))})
