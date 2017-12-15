(defproject me.arrdem/lein-git-version "0.1.0-SNAPSHOT"
  :description "Use git for project versions."
  :url "https://github.com/arrdem/lein-git-version"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :eval-in-leiningen true
  :deploy-repositories [["releases" :clojars]]

  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :plugins      [[lein-midje "3.2"]]}})
