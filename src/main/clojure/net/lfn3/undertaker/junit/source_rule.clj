(ns net.lfn3.undertaker.junit.source-rule
  (:gen-class
    :name net.lfn3.undertaker.junit.SourceRule
    :state state
    :implements [net.lfn3.undertaker.junit.Source]
    :init init
    :constructors {[]                            []
                   [java.util.Map]               []
                   [java.util.Map java.util.Map] []})
  (:import (org.junit.runners.model Statement)
           (org.junit.runner Description JUnitCore Request)
           (java.util List Map Collection Set)
           (java.util.function Function BiFunction)
           (java.lang.reflect Modifier Method Parameter ParameterizedType Constructor Executable)
           (net.lfn3.undertaker.junit Seed Trials)
           (net.lfn3.undertaker.junit Generator Debug Source SourceRule GenericGenerator)
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

(def default-class->generic-generators-map
  {List (reify GenericGenerator                             ;Assumes there's only a single element in generic-classes
          (apply [_ source generic-classes]
            (.nextList ^Source source (reify Generator
                                        (apply [_ source] (.reflectively source ^Class (first generic-classes)))))))
   Map  (reify GenericGenerator
          (apply [_ source generic-classes]
            (.nextMap ^Source source
                      (reify Generator
                        (apply [_ source] (.reflectively source ^Class (first generic-classes))))
                      (reify Generator
                        (apply [_ source] (.reflectively source ^Class (nth generic-classes 1)))))))
   Set  (reify GenericGenerator                             ;Assumes there's only a single element in generic-classes
          (apply [_ source generic-classes]
            (.nextSet ^Source source (reify Generator
                                       (apply [_ source] (.reflectively source ^Class (first generic-classes)))))))})

(defn -init
  ([] (-init {}))
  ([class->generator-map] (-init class->generator-map {}))
  ([class->generator-map generic-class->generator-map]
   [[] {:class->generator         (merge default-class->generator-map class->generator-map)
        :generic-class->generator (merge default-class->generic-generators-map generic-class->generator-map)
        :generic-params-map       (atom {})}]))

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

(defn make-run-test-fn [^JUnitCore junit ^Request test-request]
  (fn [] (with-bindings {#'*nested* true}
           (let [failures (.getFailures (.run junit test-request))]
             (assert (<= (count failures) 1))               ;only one since we're only running a single method
             (when-let [failure (first failures)]
               (throw (.getException failure)))))))

(defn process-result [result test-name debug?]
  (when (false? (get-in result [::undertaker/initial-results ::undertaker/result]))
    (let [message (undertaker/format-results test-name result java-seed-message debug?)
          cause (or (get-in result [::undertaker/shrunk-results ::undertaker/cause])
                    (get-in result [::undertaker/initial-results ::undertaker/cause]))]
      (throw (override-delegate
               Throwable
               cause
               (getMessage [] message))))))

(defn ^Statement -apply [_ ^Statement base ^Description test-description]
  (proxy [Statement] []
    (evaluate []
      (if (not *nested*)                                    ;Check we're not already inside this rule
        (let [seed (get-annotation-value Seed test-description (undertaker/next-seed (System/nanoTime)))
              trials (get-annotation-value Trials test-description 1000)
              debug? (get-annotation-value Debug test-description false)
              junit (JUnitCore.)
              class (Class/forName (.getClassName test-description))
              test-request (Request/method class (.getMethodName test-description))
              result (undertaker/run-prop {:seed       seed
                                           :iterations trials
                                           :debug      debug?}
                                          (make-run-test-fn junit test-request))]
          (process-result result (first (str/split (.getDisplayName test-description) #"\(")) debug?))
        (.evaluate base)))))

(defn -pushInterval [_]
  ;TODO (source/push-interval undertaker/*source* {})
  )

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
   (undertaker/with-leaf-interval
     (core/unchecked-char (.applyAsShort code-point-gen this)))))

(defn ^String -nextString
  ([this] (-nextString this CodePoints/ANY 0 undertaker/default-string-max-size))
  ([this ^ShortGenerator intGen] (-nextString this intGen 0 undertaker/default-string-max-size))
  ([this ^ShortGenerator intGen size] (-nextString this intGen size size))
  ([this ^ShortGenerator intGen min max]
   (undertaker/with-compound-interval
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
  ([this ^Function generator min max] (undertaker/vec-of #(.apply generator this) min max)))

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
  ([this ^Generator g] (undertaker/with-compound-interval   ;TODO: Not sure about this
                         (.apply g this))))

(defn -generate-Class
  ([this ^Class c]
   (let [{:keys [class->generator]} (.state this)]
     (if-let [^Generator g (get class->generator c)]
       (undertaker/with-compound-interval                   ;TODO: or this
         (.apply g this))
       (throw (ex-info (str "Could not find generator for " (.getName c) " in Source's class->generator map") {}))))))

(defn -nullable
  ([this ^Generator g] (undertaker/frequency [[20 #(.apply g this)
                                               1 (constantly nil)]])))

(defn is-interface-we-cannot-generate [this ^Class c]
  (let [{:keys [generic-class->generator class->generator]} (.state this)]
    (and (.isInterface c)
         (not (get generic-class->generator c))
         (not (get class->generator c)))))

(defn has-params-we-can-generate [this ^Class c ^Executable exec]
  (->> exec
       (.getParameters)
       (map #(.getType %1))
       (not-any? #(or (is-interface-we-cannot-generate this %1)
                      (= c %1)))))

(defn get-static-constructors [this ^Class c]
  (->> c
       (.getMethods)
       (filter #(-> (.getModifiers %1)
                    (Modifier/isStatic)))
       (filter #(-> (.getReturnType %1)
                    (= c)))
       (filter (partial has-params-we-can-generate this c))))

(defn get-constructors-we-can-use [this ^Class c]
  (->> c
       (.getConstructors)
       (filter (partial has-params-we-can-generate this c))))

(defn get-type-params [^Parameter param] (.getTypeParameters (.getType param)))

(defn get-generic-types [^Parameter param]
  (let [^ParameterizedType maybe-parameterized (.getParameterizedType param)]
    (when (instance? ParameterizedType maybe-parameterized)
      (.getActualTypeArguments maybe-parameterized))))

(defn map-types-to-generics [^Parameter param]
  (zipmap (get-type-params param) (get-generic-types param)))

(defn build-generic-types-map [already-known ^Executable c]
  (->> c
       (.getGenericParameterTypes)
       (filter (partial instance? ParameterizedType))
       (map (juxt #(-> %1 .getRawType .getTypeParameters)
                  #(-> %1 .getActualTypeArguments)))
       (map (partial apply zipmap))
       (apply merge already-known))
  #_(->> c
       (.getParameters)
       (mapcat map-types-to-generics)
       (merge already-known)))

(defn generate-array-reflectively [this array-class-string]
  (let [class (Class/forName array-class-string)]
    (-nextArray this class #(.reflectively this class %1))))

(defn recursively-get [m k]
  (if (contains? m k)
    (loop [v (get m k)]
      (let [n (get m v ::no-val)]
        (if (= ::no-val n)
          v
          (recur n))))
    nil))

(defn resolve-type-params [^SourceRule this type-params]
  (if (empty? type-params)
    {}
    (let [{:keys [generic-params-map]} (.state this)]
      (->> type-params
           (map (partial recursively-get @generic-params-map))
           (filter (complement nil?))))))

(defn generate-from-class [this class]
  (let [{:keys [class->generator generic-class->generator]} (.state this)
        ^Function generator (get class->generator class)
        ^BiFunction generic-generator (get generic-class->generator class)
        type-params (.getTypeParameters class)
        ;TODO: write test where constructor doesn't have the same number of types as the class?
        resolved-type-params (resolve-type-params this type-params)
        enough-resolved-type-params? (= (count type-params) (count resolved-type-params))]
    (cond
      generator (.apply generator this)
      (and generic-generator
           enough-resolved-type-params?) (.apply generic-generator this resolved-type-params)
      (str/starts-with? (.getName class) "[L") (->> class
                                                    (.getName)
                                                    (drop 2)
                                                    (apply str)
                                                    (generate-array-reflectively this))
      (.isEnum class) (-nextEnum this class)

      :default ::not-genned)))

(def -reflectively-Class)

(defn match-to-parameters [parameters generic-parameters]
  (loop [generic-parameters generic-parameters
         parameters parameters
         result []]
    (if (first parameters)
      (let [candidate (first generic-parameters)
            param-type (.getType (first parameters))
            matched? (and (instance? ParameterizedType candidate)
                          (= (.getRawType candidate) param-type))]
        (if matched?
          (recur (rest generic-parameters)
                 (rest parameters)
                 (conj result candidate))
          (recur generic-parameters
                 (rest parameters)
                 (conj result nil))))
      result)))

(defn pick-constructor-or-throw [this ^Class c]
  (let [constructors (concat (get-constructors-we-can-use this c) (get-static-constructors this c))
        chosen (undertaker/elements constructors)]     ;Might be a static constructor
    (when (nil? chosen)
      (throw (IllegalArgumentException.
               (str "Class " c " did not have any accessible constructors with parameters we could reflectively "
                    "generate that were not " c "." \newline
                    "If " c " is not a concrete class, consider passing a concrete class, or providing a "
                    "default generator to use for " c " in the generator map when constructing this Source."))))
    chosen))

(defn build-parameters-with-generics [this ^Executable x type-param-to-type]
  )

(defn map-to-type-vars [^ParameterizedType t]
  (let [tps (map #(.getName %1) (.getTypeParameters ^Class (.getRawType t)))
        args (.getActualTypeArguments t)]
    (zipmap tps args)))

(defn -reflectively-ParameterizedType [this ^ParameterizedType t]
  (let [chosen (pick-constructor-or-throw this (.getRawType t))
        type-var->type-arg (map-to-type-vars t)]
    (println type-var->type-arg)

    ;(println (map identity (.getTypeParameters ^Class (.getRawType t))))
    ;TODO: if this is just a class, kick it onto the normal class generator
    (println (map (partial map identity) (map #(.getActualTypeArguments %1) (.getGenericParameterTypes chosen))))
    (println (map identity (map #(.getParameterizedType %1) (.getParameters chosen))))
    ;(println (map identity (.getActualTypeArguments t)))
    ))

(defn build-parameters [this ^Executable x]
  (let [params (.getParameters x)
        matched-params (match-to-parameters params (.getGenericParameterTypes x))]
    (prn (map identity params))
    (prn matched-params)
    (println "============")
    (->> params
         (map #(.getType %1))
         (map-indexed #(if-let [^ParameterizedType parameterized (get matched-params %1)]
                         (-reflectively-ParameterizedType this parameterized)
                         (-reflectively-Class this %2)))
         (into-array Object))))

(defn -reflectively-Constructor
  ([this ^Constructor c]
   (let [{:keys [generic-params-map]} (.state this)]
     (swap! generic-params-map build-generic-types-map c))
   (.newInstance c (build-parameters this c))))

(defn -reflectively-Method-Object
  ([this ^Method m instance]
   (let [{:keys [generic-params-map]} (.state this)]
     (swap! generic-params-map build-generic-types-map m))
   (.invoke m instance (build-parameters this m))))

(defn -reflectively-Method
  ([this ^Method m]
   (let [{:keys [generic-params-map]} (.state this)]
     (swap! generic-params-map build-generic-types-map m))
   (-reflectively-Method-Object this m (-reflectively-Class this (.getDeclaringClass m)))))

(defn -reflectively-Class
  ([this ^Class c]
   (let [generated (generate-from-class this c)]
     (if-not (= ::not-genned generated)
       generated
       (let [chosen (pick-constructor-or-throw this c)]     ;Might be a static constructor
         (if (instance? Constructor chosen)
           (-reflectively-Constructor this chosen)
           (-reflectively-Method-Object this chosen nil)))))))
