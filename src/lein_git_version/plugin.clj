(ns lein-git-version.plugin
  "The lein-git-version plugin as loaded by lein itself."
  (:require [leiningen.git-version :refer [default-config git-status]]))

(defn middleware
  "Leiningen middleware.

  Function of a project to a project which inserts git status and ref information.

  Tries to unconditionally create the `:ref`, `:ref-short`, `:tag`,
  `:ahead`, `:ahead?` and `:dirty?` keys in the leiningen project
  map. May create the `:message` and `:timestamp` keys as well.

  If the project's `:version` is `:project/ref` or
  `:project/ref-short`, the git ref or short ref will be used as the
  version of the project.

  If the user has specified a `:status-to-version` lambda under the
  `:git-version` configuration map, that function will be used to
  compute a project version based on the current git state."

  [{:keys           [git-version name root]
    project-version :version
    :as             project}]
  (let [{:keys [path root-ns set-version status-to-version] :as config}
        ,,(merge default-config git-version)

        {:keys [tag version ahead ahead? ref ref-short] :as status}
        ,,(git-status config)

        status
        ,,(dissoc status :version)

        status-to-version
        ,,(when status-to-version
            (try (eval status-to-version)
                 (catch Exception e
                   (binding [*out* *err*]
                     (printf (str "While trying to evaluate status-to-version function:\n%s\n\n"
                                  "Encountered exception:\n%s\n")
                             status-to-version e))
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
