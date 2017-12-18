# lein-git-version

[![Clojars Project](https://img.shields.io/clojars/v/me.arrdem/lein-git-version.svg)](https://clojars.org/me.arrdem/lein-git-version)

This repo is a fork and to my taste a massive cleanup of
[cvillecsteele's
lein-git-version](https://github.com/cvillecsteele/lein-git-version)
which is itself an un-maintained alternative to
[michalmarczyk's](https://github.com/michalmarczyk/lein-git-version)
original project.

## Motivation

Leiningen projects, in their heritage from Maven, list an explicit
version as the 3rd element of a `project.clj` file. For instance

```clojure
(defproject foo "some-version"
  ...)
```

There are a couple problems with this.  First of all, until the
arrival of `leiningen.release/bump-version` for the `lein release`
task, there was really no sane way to update the version of a property
short of a sed script which just rewrote the `project.clj`.

While `bump-version` is a mostly acceptable solution, it still relies
on the filesystem (or to be more specific a code repository)
reflecting in a file under version control the logical identifier
attached to some point in the history of the repository.

The problem with sticking a version identifier in the filesystem is
that it becomes a source of merge conflicts when multiple people are
collaborating on an artifact, and it may be difficult to automate
vesion management using merge hooks in workflows with automated
commits that can be difficult to implement.

Moreover, in monorepo patterns ala
[lein-modules](https://github.com/jcrossley3/lein-modules), versions
for shared libraries which are distributed only as a component of
artifacts in the repository are no longer particularly a meaningful
construct. The commit ID or the version control label is the most
meaningful identifier.

## Usage

Add:

```clojure
;; Add in the git-version plugin
:plugins [[me.arrdem/lein-git-version "2.0.3"]
          ...]
```

By default, my incarnation of lein-git-version doesn't do anything to
your project, except make some additional keys visible in the project
map.

```clojure
{:tag       ;; Name of the last git tag if any
 :ahead     ;; Number of commits ahead of the last tag, or 0
 :ahead?    ;; Is the head ahead by more than 0 commits
 :ref       ;; The full current ref
 :ref-short ;; The "short" current ref
 :dirty?    ;; Optional. Boolean. Are there un-committed changes.
 :message   ;; Optional. The last commit message when clean.
 :timestamp ;; Optional. The last commit date when clean.
}
```
If the keyword `:project/ref` or `:project/ref-short` is used in place
of a version string, lein-git-version will update the version string
of the project to instead reflect the ref or short ref.

For instance,

```clojure
(defproject bar :project/ref
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]
  ...)
```
or

```clojure
(defproject baz :project/ref-short
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]
  ...)
```
lein-git-version can also be used to compute a versions string, as an
arbitrary function of the current git status by specifying a
`status-to-version` function of the above status structure in the
`:git-version` map of your `project.clj`.

For instance, lein-git-version an earlier version of itself uses
itself to compute its own version.

```clojure
(defproject me.arrdem/lein-git-version "_"
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]

  :git-version {:status-to-version
    (fn [{:keys [tag version ahead ahead? dirty?] :as git}]
      (if (and tag (not ahead?) (not dirty?))
        tag
        (str tag
             (when ahead? (str "." ahead))
             (when dirty? "-SNAPSHOT"))))
  }
  ...)
```

will compute a version string containing the last tag, the number of
commits ahead and the "-SNAPSHOT" suffix if the repo is dirty or it's
ahead.

This enables your release workflow to consist simply of creating a tag
and doing a deploy. No source changes are required.

Like its predecessors, lein-git-version can be used to generate a
version file. Previous incarnations tried to lay down a version
namespace. This incarnation can be configured to lay down an EDN file
containing git information at a specified location relative to the
root of the project.

For instance,

```clojure
(defproject com.my-app/cares-what-version-it-is :project/ref-short
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]
  ...
  :git-version {:version-file "resources/com/my_app/version.edn"
                :version-file-keys [:ref :version :timestamp]}))
```

will cause lein-git-version to make the specified directory, and lay
down an EDN file containing all the selected project status
information from the project map.

It's suggested that rather than target a directory under source
control, a source control ignored eg `"gen-resources/..."` directory
be used to isolate generated build artifacts from user
sources. Remember to add the target to your project's `:resource-paths`!

## License

Copyright © 2017 Reid McKenzie

Derived from lein-git-version © 2016 Colin Steele

Distributed under the Eclipse Public License, the same as Clojure.
