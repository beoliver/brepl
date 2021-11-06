(ns brepl.bencode
  (:require [cljs.pprint :refer [cl-format]]
            [clojure.string :as str]))

(defn encode [data]
  (cond
    (string? data) (cl-format nil "~a:~a" (count data) data)
    (symbol? data) (let [s (str data)]
                     (cl-format nil "~a:~a" (count s) s))
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
      "i"  (parse-int bencode)
      "d"  (parse-dict bencode)
      "l"  (parse-list bencode)
      (parse-str bencode))))

(defn- parse-int [bencode]
  (let [end-of-int (.indexOf bencode "e")
        int-val (int (.slice bencode 1 end-of-int))
        remaining (.slice bencode (inc end-of-int))]
    [int-val remaining]))

(defn- parse-str [bencode]
  ;; [0-9]+ : .*
  (let [colon-index (.indexOf bencode ":")
        str-len (int (.slice bencode 0 colon-index))
        string-start-index (inc colon-index)
        string-end-index   (+ str-len string-start-index)
        string (.slice bencode string-start-index string-end-index)
        remaining (.slice bencode string-end-index)]
    [string remaining]))

(defn- parse-dict [bencode]
  (loop [bencode (.slice bencode 1) ; strip the 'd' prefix
         value   (transient {})]    ;
    (if (= "e" (.charAt bencode 0)) [(persistent! value) (.slice bencode 1)]
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
