(ns net.lfn3.undertaker.junit.source-rule
  (:gen-class
    :name net.lfn3.undertaker.junit.SourceRule
    :state state
    :implements [net.lfn3.undertaker.junit.Source]
    :init init
    :constructors {[] []
                   [java.util.Map] []})
  (:import (org.junit.runners.model Statement)
           (org.junit.runner Description JUnitCore Computer Request)
           (java.util List ArrayList Map Collection Set)
           (java.util.function Function BiFunction)
           (java.lang.reflect Modifier)
           (net.lfn3.undertaker.junit Seed Trials)
           (net.lfn3.undertaker.junit Generator)
           (net.lfn3.undertaker.junit.generators IntGenerator CodePoints))
  (:require [net.lfn3.undertaker.core :as undertaker]
            [net.lfn3.undertaker.source :as source]
            [clojure.string :as str]))

(defn -init
  ([] (-init {}))
  ([class->generator-map] [[] {:class->generator class->generator-map}]))

(def ^:dynamic *nested* false)

(defn add-tag-meta-if-applicable [symbol ^Class type]
  (if (and (.isPrimitive type)
           (not= "long" (str type))
           (not= "double" (str type)))
    symbol
    (with-meta symbol {:tag (.getName type)})))

(defmacro override-delegate
  "Based on https://stackoverflow.com/a/33463302/5776097"
  [type delegate & body]
  (let [d (gensym)
        overrides (group-by first body)
        methods (for [m (.getMethods (resolve type))
                      :let [f (-> (.getName m)
                                  symbol
                                  (add-tag-meta-if-applicable (.getReturnType m)))]
                      :when (and (not (overrides f))
                                 (not (Modifier/isPrivate (.getModifiers m)))
                                 (not (Modifier/isProtected (.getModifiers m))))
                      :let [args (for [t (.getParameterTypes m)]
                                   (add-tag-meta-if-applicable (gensym) t))]]
                  (list f (vec (conj args 'this))
                        `(. ~d ~f ~@(map #(with-meta % nil) args))))
        grouped-methods (->> methods
                             (group-by first)
                             (map (fn [[f arities]]
                                    (let [by-arity-length (group-by (fn [[_ args-vec]] (count args-vec)) arities)]
                                      (apply list f (->> by-arity-length
                                                         (map (fn [[_ arities]] (first arities)))
                                                         (map (fn [[_ args-vec body]] `(~args-vec (~@body)))))))))
                             (into []))]
    `(let [~d ~delegate]
       (proxy [~type] [] ~@body ~@grouped-methods))))

(defn get-annotation-value [^Class annotation ^Description description default]
  (let [annotation (or (.getAnnotation description annotation)
                       (.getAnnotation (.getTestClass description) annotation))]
    (or (some-> annotation
                (.value))
        default)))

(defn ^Statement -apply [_ ^Statement base ^Description description]
  (proxy [Statement] []
    (evaluate []
      (if (not *nested*)                                    ;Check we're not already inside this rule
        (let [seed (get-annotation-value Seed description (undertaker/next-seed (System/nanoTime)))
              trials (get-annotation-value Trials description 1000)
              junit (JUnitCore.)
              _ (Computer.)
              class (resolve (symbol (.getClassName description)))
              test-request (Request/method class (.getMethodName description))
              result (undertaker/run-prop {:seed       seed
                                           :iterations trials}
                                          #(with-bindings {#'*nested* true}
                                             (->> (.run junit test-request)
                                                  (.getFailures)
                                                  (map (fn [f] (-> (.getException f) ;TODO: process failures in a function
                                                                   (throw))))
                                                  (dorun))))]
          (when (false? (get-in result [::undertaker/initial-results ::undertaker/result]))
            (let [test-name (first (str/split (.getDisplayName description) #"\("))
                  message (undertaker/format-results test-name result)
                  cause (or (get-in result [::undertaker/shrunk-results ::undertaker/cause])
                            (get-in result [::undertaker/initial-results ::undertaker/cause]))]
              (throw (override-delegate
                       Throwable
                       cause
                       (getMessage [] message))))))
        (.evaluate base)))))

(defn -pushInterval [_]
  (source/push-interval undertaker/*source*))

(defn -popInterval [_ generated-value]
  (source/pop-interval undertaker/*source* generated-value))

(defn ^byte -getByte
  ([this] (-getByte this Byte/MIN_VALUE Byte/MAX_VALUE))
  ([this max] (-getByte this Byte/MIN_VALUE max))
  ([_ min max] (undertaker/byte min max)))

(defn ^short -getShort
  ([this] (-getShort this Short/MIN_VALUE Short/MAX_VALUE))
  ([this max] (-getShort this Integer/MIN_VALUE max))
  ([_ min max] (undertaker/short min max)))

(defn ^int -getInt
  ([this] (-getInt this Integer/MIN_VALUE Integer/MAX_VALUE))
  ([this max] (-getInt this Integer/MIN_VALUE max))
  ([_ min max] (undertaker/int min max))
  ([_ min max & more-ranges] (apply undertaker/int min max more-ranges)))

(defn ^long -getLong
  ([this] (-getLong this Long/MIN_VALUE Long/MAX_VALUE))
  ([this max] (-getLong this Long/MIN_VALUE max))
  ([_ min max] (undertaker/long min max)))

(defn ^boolean -getBool
  ([_] (undertaker/boolean)))

(defn ^char -getChar
  ([_] (undertaker/char)))

(defn ^char -getAsciiChar
  ([_] (undertaker/char-ascii)))

(defn ^char -getAlphanumericChar
  ([_] (undertaker/char-alphanumeric)))

(defn ^char -getAlphaChar
  ([_] (undertaker/char-alpha)))

(defn ^String -getString
  ([this] (-getString this CodePoints/ANY 0 undertaker/default-string-max-size))
  ([this ^IntGenerator intGen] (-getString this intGen 0 undertaker/default-string-max-size))
  ([this ^IntGenerator intGen size] (-getString this intGen size size))
  ([this ^IntGenerator intGen min max]
   (undertaker/with-interval
     (->> (undertaker/vec-of #(.applyAsInt intGen this) min max)
          (map char)
          (char-array)
          (String.)))))

(defn ^float -getFloat
  ([this] (-getFloat this (- Float/MAX_VALUE) Float/MAX_VALUE))
  ([this max] (-getFloat this (- Float/MAX_VALUE) max))
  ([_ min max] (undertaker/float min max)))

(defn ^double -getDouble
  ([this] (-getDouble this (- Double/MAX_VALUE) Double/MAX_VALUE))
  ([this max] (-getDouble this (- Double/MAX_VALUE) max))
  ([_ min max] (undertaker/double min max)))

(defn ^double -getRealDouble
  ([this] (-getRealDouble this (- Double/MAX_VALUE) Double/MAX_VALUE))
  ([this max] (-getRealDouble this (- Double/MAX_VALUE) max))
  ([_ min max] (undertaker/real-double min max)))

(defn ^List -getList
  ([this ^Function generator] (-getList this generator 0 64))
  ([this ^Function generator size] (-getList this generator size size))
  ([this ^Function generator min max] (ArrayList. (undertaker/vec-of #(.apply generator this) min max))))

(defn ^Map -getMap
  ([this ^Function keyGen valGen] (-getMap this keyGen valGen 0 undertaker/default-collection-max-size))
  ([this ^Function keyGen valGen size] (-getMap this keyGen valGen size size))
  ([this ^Function keyGen valGen minSize maxSize]
   (if (instance? BiFunction valGen)
     (undertaker/map-of #(.apply keyGen this) #(.apply valGen this %1) minSize maxSize {:value-gen-takes-key-as-arg true})
     (undertaker/map-of #(.apply keyGen this) #(.apply valGen this) minSize maxSize))))

(defn ^Set -getSet
  ([this ^Function generator] (-getSet this generator 0 undertaker/default-collection-max-size))
  ([this ^Function generator size] (-getSet this generator size size))
  ([this ^Function generator minSize maxSize] (undertaker/set-of #(.apply generator this) minSize maxSize)))

(defn -getArray
  ([this ^Class c ^Function generator] (-getArray this c generator 0 64))
  ([this ^Class c ^Function generator size] (-getArray this c generator size size))
  ([this ^Class c ^Function generator min max] (into-array c (undertaker/vec-of #(.apply generator this) min max))))

(defn -getEnum
  ([_ ^Class c] (undertaker/elements (.getEnumConstants c))))

(defn -from
  ([_ ^Collection c] (undertaker/elements c)))

(defn -generate
  ([this ^Generator g] (.apply g this)))

(defn -generate-Class
  ([this ^Class c]
   (let [{:keys [class->generator]} (.state this)]
     (if-let [g (get class->generator c)]
       (.apply g this)
       (throw (ex-info (str "Could not find generator for " (.getName c) " in Source's class->generator map") {}))))))

(defn -getNullable
  ([this ^Generator g] (undertaker/frequency [[20 #(.apply g this)
                                               1 (constantly nil)]])))

(def generate-from-class)

(defn -reflectively
  ([this c]
   (let [is-class? (class? c)
         generated (and is-class? (generate-from-class this c))]
     (if (and is-class? (not= ::not-genned generated))
       generated
       (do
         (when (and is-class? (.isInterface c))
           (throw (IllegalArgumentException. (str "Can't reflectively generate an interface. "
                                                  "Please pass a concrete class instead of " c))))
         (let [constructor (if is-class?
                             (let [constructors (->> c
                                                     (.getConstructors)
                                                     (filter #(->> %1
                                                                   (.getParameters)
                                                                   (map (fn [p] (.getType p)))
                                                                   (not-any? (fn [parameter-class]
                                                                               (or (.isInterface parameter-class)
                                                                                   (= c parameter-class)))))))]
                               (when (empty? constructors)
                                 (throw (IllegalArgumentException.
                                          (str "Class " c " did not have any public constructors "
                                               "with only concrete parameters that were not " c "."))))
                               (undertaker/elements constructors))
                             c)]                            ;Assume c is a constructor
           (->> constructor
                (.getParameters)
                (map #(.getType %1))
                (map #(-reflectively this %1))
                (into-array Object)
                (.newInstance constructor))))))))

(defmacro get-array-fn [type-hint type-str & [array-fn-name]]
  (let [fn-name (symbol (str "-get" (str/upper-case (first type-str))
                             (apply str (rest type-str))
                             "Array"))
        array-fn-name (if array-fn-name
                        (symbol array-fn-name)
                        (symbol (str type-str "-array")))
        generator-name (symbol "undertaker" type-str)]
    `(defn ^{:tag type-hint} ~fn-name
       ([_#] (~array-fn-name (undertaker/vec-of ~generator-name)))
       ([this# ^Function generator#]
         (~array-fn-name (undertaker/vec-of #(.apply generator# this#))))
       ([this# ^Function generator# size#]
         (~array-fn-name (undertaker/vec-of #(.apply generator# this#) size# size#)))
       ([this# ^Function generator# min# max#]
         (~array-fn-name (undertaker/vec-of #(.apply generator# this#) min# max#))))))

(get-array-fn "[J" "long")
(get-array-fn "[B" "byte")
(get-array-fn "[C" "char")
(get-array-fn "[D" "double")
(get-array-fn "[F" "float")
(get-array-fn "[I" "int")
(get-array-fn "[S" "short")
(get-array-fn "[Z" "boolean" "boolean-array")

(defn generate-array-reflectively [this array-class-string]
  (let [class (Class/forName array-class-string)]
    (-getArray this class (partial -reflectively this class))))

(defn generate-from-class [this class]
  (let [{:keys [class->generator]} (.state this)
        generator (get class->generator class)]
    (cond
      generator (.apply generator this)

      (or (= class Long) (= class Long/TYPE)) (undertaker/long)
      (or (= class Integer) (= class Integer/TYPE)) (undertaker/int)
      (or (= class Short) (= class Short/TYPE)) (undertaker/short)
      (or (= class Byte) (= class Byte/TYPE)) (undertaker/byte)
      (or (= class Float) (= class Float/TYPE)) (undertaker/float)
      (or (= class Double) (= class Double/TYPE)) (undertaker/double)
      (or (= class Character) (= class Character/TYPE)) (undertaker/char)
      (or (= class Boolean) (= class Boolean/TYPE)) (undertaker/boolean)
      (= class String) (undertaker/string)

      (= class (Class/forName "[J")) (-getLongArray this)
      (= class (Class/forName "[B")) (-getByteArray this)
      (= class (Class/forName "[C")) (-getCharArray this)
      (= class (Class/forName "[D")) (-getDoubleArray this)
      (= class (Class/forName "[F")) (-getFloatArray this)
      (= class (Class/forName "[I")) (-getIntArray this)
      (= class (Class/forName "[S")) (-getShortArray this)
      (= class (Class/forName "[Z")) (-getBooleanArray this)

      (str/starts-with? (.getName class) "[L") (->> class
                                                    (.getName)
                                                    (drop 2)
                                                    (apply str)
                                                    (generate-array-reflectively this))

      (.isEnum class) (-getEnum this class)

      :default ::not-genned)))
