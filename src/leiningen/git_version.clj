(ns leiningen.git-version
  ""
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(def git-describe-pattern
  #"(?<tag>.*)-(?<ahead>\d+)-g(?<ref>[0-9a-f]*)(?<dirty>(-dirty)?)")

(def default-config
  {:git              "git"
   :describe-pattern git-describe-pattern
   :tag-to-version   nil})

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

(defmacro let-groups
  "Let for binding groups out of a j.u.Pattern j.u.r.Matcher."
  {:style/indent [2]}
  [m bindings & body]
  (let [s (with-meta (gensym "matcher") {:tag java.util.regex.Matcher})]
    `(let [~s ~m
           ~@(mapcat identity
                     (for [b bindings]
                       `[~b (.group ~s ~(name b))]))]
       ~@body)))

(defn ensure-pattern [x]
  (if (string? x)
    (re-pattern x)
    x))

(defn parse-git-describe
  [{:keys [describe-pattern] :as config} out]
  (let [pattern (ensure-pattern describe-pattern)
        matcher (re-matcher pattern out)]
    (if-not (.matches matcher)
      (do (binding [*out* *err*]
            (printf "Warning: lein-git-version didn't match the current repo status:\n%s\n\nAgainst pattern:\n%s\n"
                    (pr-str out) pattern)
            (.flush *out*))
          {})
      (let-groups matcher [tag ahead ref dirty]
                  {:tag       tag
                   :ahead     (Integer/parseInt ahead)
                   :ahead?    (not= ahead "0")
                   :ref       (get-git-ref config)
                   :ref-short ref
                   :dirty?    (not= "" dirty)}))))

(defn git-describe
  ""
  [{:keys [git] :as config}]
  (let [{:keys [exit out] :as child} (apply sh [git "describe" "--tags" "--dirty" "--long"])]
    (if-not (= exit 0)
      (binding [*out* *err*]
        (printf "Warning: lein-git-version git exited %d\n%s\n"
                exit child)
        (.flush *out*)
        {})
      (parse-git-describe config (str/trim out)))))

(defn git-status
  "Fetch the current git status."
  [config]
  (if-let [{:keys [dirty?] :as status} (git-describe config)]
    (cond->  status
      (not dirty?) (assoc :message (git-ref-message config "HEAD"))
      (not dirty?) (assoc :timestamp (git-ref-ts config "HEAD")))))
