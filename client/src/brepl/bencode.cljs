(ns brepl.bencode
  (:require [cljs.pprint :refer [cl-format]]
            [clojure.string :as str]))

(defn encode [data]
  (cond
    (string? data) (cl-format nil "~a:~a" (count data) data)
    (keyword? data) (let [s (name data)] (cl-format nil "~a:~a" (count s) s))
    (int? data) (cl-format nil "i~ae" data)
    (map? data) (->> data
                     (sort-by first)
                     (apply concat)
                     (map encode)
                     str/join
                     (cl-format nil "d~ae"))
    (sequential? data) (cl-format nil "l~ae" (str/join (map encode data)))))

(declare parse
         parse-int
         parse-str
         parse-list
         parse-dict)

(defn- parse [bencode]
  (let [first-char (.charAt bencode 0)]
    (case first-char
      "i" (parse-int bencode)
      "d" (parse-dict bencode)
      "l" (parse-list bencode)
      (parse-str bencode))))

(def ^:private bencode-int-re #"^i([0-9]+)e(.*)")

(defn- parse-int [bencode]
  (let [[_ val remaining] (re-matches bencode-int-re bencode)]
    [(int val) remaining]))

(def ^:private bencode-str-re #"^([0-9]+):(.*)")

(defn- parse-str [bencode]
  (let [[_ str-len str-and-remaining] (re-matches bencode-str-re bencode)
        i (int str-len)]
    [(.slice str-and-remaining 0 i) (.slice str-and-remaining i)]))

(defn- parse-dict [bencode]
  (loop [bencode (.slice bencode 1) ; strip the 'd' prefix
         value   (transient {})]    ;
    (if (= "e" (.charAt bencode 0)) ; closing char for a dict
      [(persistent! value) (.slice bencode 1)]
      (let [[k remaining-bencode-temp] (parse bencode)
            [v remaining-bencode] (parse remaining-bencode-temp)]
        (recur remaining-bencode (assoc! value (keyword k) v))))))

(defn- parse-list [bencode]
  (loop [bencode (.slice bencode 1)
         value (transient [])]
    (if (= "e" (.charAt bencode 0))
      [(persistent! value) (.slice bencode 1)]
      (let [[x remaining-bencode] (parse bencode)]
        (recur remaining-bencode (conj! value x))))))

(defn decode [bencode] (first (parse bencode)))
