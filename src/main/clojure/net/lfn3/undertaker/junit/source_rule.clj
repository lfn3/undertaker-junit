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
           (java.lang.reflect Modifier Method)
           (net.lfn3.undertaker.junit Seed Trials)
           (net.lfn3.undertaker.junit Generator Debug)
           (net.lfn3.undertaker.junit.generators IntGenerator CodePoints ShortGenerator)
           (net.lfn3.undertaker.junit.primitive.functions ToBooleanFunction ToByteFunction ToCharFunction ToFloatFunction ToShortFunction))
  (:require [net.lfn3.undertaker.core :as undertaker]
            [net.lfn3.undertaker.source :as source]
            [clojure.string :as str]
            [clojure.core :as core]))

(defmacro get-array-fn [type-hint type-str specialize-from]
  (let [camelcased-type-str (str (str/upper-case (first type-str)) (apply str (rest type-str)))
        fn-name (symbol (str "-next" camelcased-type-str "Array"))
        array-fn-name (symbol (str type-str "-array"))
        generator-name (symbol "undertaker" type-str)
        apply-fn (symbol (str "applyAs" camelcased-type-str))
        function-type-hint (if (= specialize-from :java)
                             (symbol (str "java.util.function.To" camelcased-type-str "Function"))
                             (symbol (str "net.lfn3.undertaker.junit.primitive.functions.To" camelcased-type-str "Function")))]
    `(defn ^{:tag type-hint} ~fn-name
       ([_#] (~array-fn-name (undertaker/vec-of ~generator-name)))
       ([this# ^{:tag ~function-type-hint} generator#]
         (~array-fn-name (undertaker/vec-of #(. generator# ~apply-fn this#))))
       ([this# ^{:tag ~function-type-hint} generator# size#]
         (~array-fn-name (undertaker/vec-of #(. generator# ~apply-fn this#) size# size#)))
       ([this# ^{:tag ~function-type-hint} generator# min# max#]
         (~array-fn-name (undertaker/vec-of #(. generator# ~apply-fn this#) min# max#))))))

(get-array-fn "[J" "long" :java)
(get-array-fn "[B" "byte" :undertaker)
(get-array-fn "[C" "char" :undertaker)
(get-array-fn "[D" "double" :java)
(get-array-fn "[F" "float" :undertaker)
(get-array-fn "[I" "int" :java)
(get-array-fn "[S" "short" :undertaker)
(get-array-fn "[Z" "boolean" :undertaker)

(defn wrap-fn-to-java-fn [f]
  (reify
    Function
    (apply [_ _] (f))))

(def primitive-generators
  {Long/TYPE      undertaker/long
   Long           undertaker/long
   Integer/TYPE   undertaker/int
   Integer        undertaker/int
   Short/TYPE     undertaker/short
   Short          undertaker/short
   Byte/TYPE      undertaker/byte
   Byte           undertaker/byte
   Float/TYPE     undertaker/float
   Float          undertaker/float
   Double/TYPE    undertaker/double
   Double         undertaker/double
   Character/TYPE undertaker/char
   Character      undertaker/char
   Boolean/TYPE   undertaker/boolean
   Boolean        undertaker/boolean})

(def array-generators
  {(Class/forName "[J") (partial -nextLongArray nil)
   (Class/forName "[B") (partial -nextByteArray nil)
   (Class/forName "[C") (partial -nextCharArray nil)
   (Class/forName "[D") (partial -nextDoubleArray nil)
   (Class/forName "[F") (partial -nextFloatArray nil)
   (Class/forName "[I") (partial -nextIntArray nil)
   (Class/forName "[S") (partial -nextShortArray nil)
   (Class/forName "[Z") (partial -nextBooleanArray nil)})

(def java-types-generators
  {String undertaker/string})

(def default-class->generator-map (->> (merge primitive-generators java-types-generators array-generators)
                                       (map (fn [[class f]] [class (wrap-fn-to-java-fn f)]))
                                       (into {})))

(defn -init
  ([] (-init {}))
  ([class->generator-map] [[] {:class->generator (merge default-class->generator-map class->generator-map)}]))

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
                  (list f (vec args)
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

(defn java-seed-message [name {:keys [:net.lfn3.undertaker.core/seed]}]
  (format "To rerun this particular failing case you can add an annotation to the test:
@Test
@net.lfn3.undertaker.junit.Seed(%s)
public void %s() { ... }"
          seed name))

(defn ^Statement -apply [_ ^Statement base ^Description description]
  (proxy [Statement] []
    (evaluate []
      (if (not *nested*)                                    ;Check we're not already inside this rule
        (let [seed (get-annotation-value Seed description (undertaker/next-seed (System/nanoTime)))
              trials (get-annotation-value Trials description 1000)
              debug? (get-annotation-value Debug description false)
              junit (JUnitCore.)
              class (resolve (symbol (.getClassName description)))
              test-request (Request/method class (.getMethodName description))
              result (undertaker/run-prop {:seed       seed
                                           :iterations trials
                                           :debug debug?}
                                          #(with-bindings {#'*nested* true}
                                             (->> (.run junit test-request)
                                                  (.getFailures)
                                                  (map (fn [f] (-> (.getException f) ;TODO: process failures in a function
                                                                   (throw))))
                                                  (dorun))))]
          (when (false? (get-in result [::undertaker/initial-results ::undertaker/result]))
            (let [test-name (first (str/split (.getDisplayName description) #"\("))
                  message (undertaker/format-results test-name result java-seed-message debug?)
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

(defn ^byte -nextByte
  ([this] (-nextByte this Byte/MIN_VALUE Byte/MAX_VALUE))
  ([this max] (-nextByte this Byte/MIN_VALUE max))
  ([_ min max] (undertaker/byte min max)))

(defn ^short -nextShort
  ([this] (-nextShort this Short/MIN_VALUE Short/MAX_VALUE))
  ([this max] (-nextShort this Integer/MIN_VALUE max))
  ([_ min max] (undertaker/short min max))
  ([_ min max & more-ranges] (apply undertaker/short min max more-ranges)))

(defn ^int -nextInt
  ([this] (-nextInt this Integer/MIN_VALUE Integer/MAX_VALUE))
  ([this max] (-nextInt this Integer/MIN_VALUE max))
  ([_ min max] (undertaker/int min max))
  ([_ min max & more-ranges] (apply undertaker/int min max more-ranges)))

(defn ^long -nextLong
  ([this] (-nextLong this Long/MIN_VALUE Long/MAX_VALUE))
  ([this max] (-nextLong this Long/MIN_VALUE max))
  ([_ min max] (undertaker/long min max)))

(defn ^boolean -nextBool
  ([_] (undertaker/boolean)))

(defn ^char -nextChar
  ([_] (undertaker/char))
  ([this code-point-gen]
   (undertaker/with-interval
     (core/unchecked-char (.applyAsShort code-point-gen this)))))

(defn ^String -nextString
  ([this] (-nextString this CodePoints/ANY 0 undertaker/default-string-max-size))
  ([this ^ShortGenerator intGen] (-nextString this intGen 0 undertaker/default-string-max-size))
  ([this ^ShortGenerator intGen size] (-nextString this intGen size size))
  ([this ^ShortGenerator intGen min max]
   (undertaker/with-interval
     (->> (undertaker/vec-of #(.applyAsShort intGen this) min max)
          (map unchecked-char)
          (char-array)
          (String.)))))

(defn ^float -nextFloat
  ([this] (-nextFloat this (- Float/MAX_VALUE) Float/MAX_VALUE))
  ([this max] (-nextFloat this (- Float/MAX_VALUE) max))
  ([_ min max] (undertaker/float min max)))

(defn ^double -nextDouble
  ([this] (-nextDouble this (- Double/MAX_VALUE) Double/MAX_VALUE))
  ([this max] (-nextDouble this (- Double/MAX_VALUE) max))
  ([_ min max] (undertaker/double min max)))

(defn ^double -nextRealDouble
  ([this] (-nextRealDouble this (- Double/MAX_VALUE) Double/MAX_VALUE))
  ([this max] (-nextRealDouble this (- Double/MAX_VALUE) max))
  ([_ min max] (undertaker/real-double min max)))

(defn ^List -nextList
  ([this ^Function generator] (-nextList this generator 0 64))
  ([this ^Function generator size] (-nextList this generator size size))
  ([this ^Function generator min max] (ArrayList. (undertaker/vec-of #(.apply generator this) min max))))

(defn ^Map -nextMap
  ([this ^Function keyGen valGen] (-nextMap this keyGen valGen 0 undertaker/default-collection-max-size))
  ([this ^Function keyGen valGen size] (-nextMap this keyGen valGen size size))
  ([this ^Function keyGen valGen minSize maxSize]
   (if (instance? BiFunction valGen)
     (undertaker/map-of #(.apply keyGen this) #(.apply valGen this %1) minSize maxSize {:value-gen-takes-key-as-arg true})
     (undertaker/map-of #(.apply keyGen this) #(.apply valGen this) minSize maxSize))))

(defn ^Set -nextSet
  ([this ^Function generator] (-nextSet this generator 0 undertaker/default-collection-max-size))
  ([this ^Function generator size] (-nextSet this generator size size))
  ([this ^Function generator minSize maxSize] (undertaker/set-of #(.apply generator this) minSize maxSize)))

(defn -nextArray
  ([this ^Class c ^Function generator] (-nextArray this c generator 0 64))
  ([this ^Class c ^Function generator size] (-nextArray this c generator size size))
  ([this ^Class c ^Function generator min max] (into-array c (undertaker/vec-of #(.apply generator this) min max))))

(defn -nextEnum
  ([_ ^Class c] (undertaker/elements (.getEnumConstants c))))

(defn -from
  ([_ ^Collection c] (undertaker/elements c)))

(defn -generate
  ([this ^Generator g] (undertaker/with-interval
                         (.apply g this))))

(defn -generate-Class
  ([this ^Class c]
   (let [{:keys [class->generator]} (.state this)]
     (if-let [g (get class->generator c)]
       (undertaker/with-interval
         (.apply g this))
       (throw (ex-info (str "Could not find generator for " (.getName c) " in Source's class->generator map") {}))))))

(defn -nullable
  ([this ^Generator g] (undertaker/frequency [[20 #(.apply g this)
                                               1 (constantly nil)]])))

(def generate-from-class)
(def -reflectively-Method)

(defn -reflectively
  ([this c]
   (let [is-class? (class? c)
         generated (and is-class? (generate-from-class this c))
         method? (instance? Method c)]
     (cond
       method? (-reflectively-Method this c)
       (and is-class? (not= ::not-genned generated)) generated
       :default
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
                (.newInstance constructor)))))))
  ([this ^Method m instance]
    (-reflectively-Method this m instance)))

(defn -reflectively-Method
  ([this ^Method m]
   (-reflectively-Method this m (-reflectively this (.getDeclaringClass m))))
  ([this ^Method m instance]
   (let [generated-parameters (->> (.getParameters m)
                                   (map #(.getType %1))
                                   (map #(-reflectively this %1))
                                   (to-array))]
     (.invoke m instance generated-parameters))))

(defn generate-array-reflectively [this array-class-string]
  (let [class (Class/forName array-class-string)]
    (-nextArray this class (partial -reflectively this class))))

(defn generate-from-class [this class]
  (let [{:keys [class->generator]} (.state this)
        generator (get class->generator class)]
    (cond
      generator (.apply generator this)

      (str/starts-with? (.getName class) "[L") (->> class
                                                    (.getName)
                                                    (drop 2)
                                                    (apply str)
                                                    (generate-array-reflectively this))

      (.isEnum class) (-nextEnum this class)

      :default ::not-genned)))
