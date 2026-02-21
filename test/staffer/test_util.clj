(ns staffer.test-util
  "Shared test utilities.")

(defn write-temp-file!
  "Writes `content` to a temp file and returns its absolute path.
   Optional `prefix` controls the temp file name prefix."
  ([content] (write-temp-file! "test" content))
  ([prefix content]
   (let [f (java.io.File/createTempFile prefix ".txt")]
     (.deleteOnExit f)
     (spit f content)
     (.getAbsolutePath f))))
