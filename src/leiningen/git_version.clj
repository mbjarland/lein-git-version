(ns leiningen.git-version
  "A quick and dirty wrapper around git for fetching ref and status information."
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(def git-describe-pattern
  #"(?<tag>.*)-(?<ahead>\d+)-g(?<ref>[0-9a-f]*)(?<dirty>(-dirty)?)")

(def default-config
  {:git              "git"
   :describe-pattern git-describe-pattern
   :tag-to-version   nil})

(defn git-ref
  "Fetches the git ref of `ref`, being a tag or ref name using the configured `git`."
  [{:keys [git] :as config} ref]
  (let [cmd [git "rev-parse" "--verify" ref]]
    (:out (apply sh cmd))))

(defn git-ref-message
  "Fetches the message of the `ref-or-sha` from git-log, using the configured `git`."
  [{:keys [git] :as config} ref-or-sha]
  (let [cmd [git "log" "-1" ref-or-sha]]
    (:out (apply sh cmd))))

(defn git-ref-ts
  "Fetches the timestamp of the `ref-or-sha` from git-log, using the configured `git`."
  [{:keys [git] :as config} ref-or-sha]
  (let [cmd [git "log" "-1" "--pretty=%ct" ref-or-sha]]
    (str/trim (:out (apply sh cmd)))))

(defmacro let-groups
  "Let for binding groups out of a j.u.r.Pattern j.u.r.Matcher."
  {:style/indent [1]}
  [[bindings m] & body]
  (let [s (with-meta (gensym "matcher") {:tag java.util.regex.Matcher})]
    `(let [~s ~m
           ~@(mapcat identity
                     (for [b bindings]
                       `[~b (.group ~s ~(name b))]))]
       ~@body)))

(defn ensure-pattern
  "Given a string, compiles it to a j.u.Pattern."
  [x]
  (cond (string? x)
        (re-pattern x)

        (instance? java.util.regex.Pattern x)
        x

        :else
        (throw (IllegalArgumentException. "ensure-pattern requires a string or a j.u.r.Pattern!"))))

(defn- parse-git-describe
  "Implementation detail.

  Used to parse the output of git-describe, using the configured `describe-pattern`.

  Returns a map `{:tag, :ahead, :ahead?, :ref, :ref-short, :dirty?}`
  if the pattern matches, otherwise returns the empty map."
  [{:keys [describe-pattern] :as config} out]
  (let [pattern (ensure-pattern describe-pattern)
        matcher (re-matcher pattern out)]
    (if-not (.matches matcher)
      (do (binding [*out* *err*]
            (printf (str "Warning: lein-git-version didn't match the current repo status:\n%s\n\n"
                         "Against pattern:\n%s\n\n")
                    (pr-str out) pattern)
            (.flush *out*))
          {})
      (let-groups [[tag ahead ref dirty] matcher]
        {:tag       tag
         :ahead     (Integer/parseInt ahead)
         :ahead?    (not= ahead "0")
         :ref       (git-ref config "HEAD")
         :ref-short ref
         :dirty?    (not= "" dirty)}))))

(defn git-describe
  "Uses git-describe to parse the status of the repository.

  Using the configured `git` and `describe-pattern` to parse the output.

  Returns a map `{:tag, :ahead, :ahead?, :ref, :ref-short, :dirty?}`
  if the pattern matches, otherwise returns the empty map."
  [{:keys [git] :as config}]
  (let [{:keys [exit out] :as child} (apply sh [git "describe" "--tags" "--dirty" "--long"])]
    (if-not (= exit 0)
      (binding [*out* *err*]
        (printf "Warning: lein-git-version git exited %d\n%s\n\n"
                exit child)
        (.flush *out*)
        {})
      (parse-git-describe config (str/trim out)))))

(defn git-status
  "Fetch the current git status, augmenting `#'git-describe`'s output
  with the message and timestamp of the last commit, if the repository
  isn't dirty."
  [config]
  (if-let [{:keys [dirty?] :as status} (git-describe config)]
    (cond->  status
      (not dirty?) (assoc :message (git-ref-message config "HEAD"))
      (not dirty?) (assoc :timestamp (git-ref-ts config "HEAD")))))
