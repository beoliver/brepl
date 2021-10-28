;;; CODE to RUN
(remove-ns 'brepl.tasks) ;; if reloading just nuke anything we already have
(in-ns 'brepl.tasks)

(clojure.core/use 'clojure.core)

(defmulti handle-task "Multimethod that dispatches on `:task` keywords" :task)

(defn handle [task-data]
  (let [result (handle-task task-data)]
    {:task (:task task-data)
     :result result}))

(defmethod handle-task :metadata-for-symbol
  [{:keys [namespace name]}]
  (let [v (ns-resolve (symbol namespace) (symbol name))]
    (-> v
        meta
        (select-keys [:arglists :doc :line :column :file :name :macro :ns])
        (update :ns ns-name))))

(defmethod handle-task :metadata-for-ns-publics
  [{:keys [name]}]
  (->> name
       symbol
       ns-publics
       vals
       (map meta)
       (map #(-> %
                 (select-keys [:arglists :doc :line :column :file :name :macro :ns])
                 (update :ns ns-name)))))

(defmethod handle-task :list-all-ns-names [_] (mapv ns-name (all-ns)))

(defmethod handle-task :apropos [{:keys [pattern]}]
  (clojure.repl/apropos (re-pattern pattern)))

;;; this comes at the end of the file as we use it to indicate that we have injected the code
{:task :inject-clojure :result (ns-name *ns*)}
