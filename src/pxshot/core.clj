(ns pxshot.core
  "Official Clojure SDK for the Pxshot screenshot API.

  ## Quick Start

  ```clojure
  (require '[pxshot.core :as pxshot])

  ;; Create a client
  (def client (pxshot/client \"px_your_api_key\"))

  ;; Take a screenshot (returns bytes)
  (def image (pxshot/screenshot client {:url \"https://example.com\"}))

  ;; Take a screenshot and store it (returns map with :url)
  (def result (pxshot/screenshot client {:url \"https://example.com\" :store true}))
  (:url result) ;; => \"https://storage.pxshot.com/...\"

  ;; Check API usage
  (pxshot/usage client)
  ```

  ## Configuration

  The client accepts an optional configuration map:

  ```clojure
  (def client (pxshot/client \"px_your_api_key\"
                             {:base-url \"https://api.pxshot.com\"
                              :timeout 30000}))
  ```"
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

;;; ----------------------------------------------------------------------------
;;; Client
;;; ----------------------------------------------------------------------------

(def ^:private default-base-url "https://api.pxshot.com")
(def ^:private default-timeout 30000)

(defn client
  "Creates a new Pxshot client.

  Arguments:
    api-key - Your Pxshot API key (string starting with 'px_')
    opts    - Optional configuration map:
              :base-url - API base URL (default: https://api.pxshot.com)
              :timeout  - Request timeout in milliseconds (default: 30000)

  Returns:
    A client map to pass to other functions.

  Example:
    (def client (pxshot/client \"px_your_api_key\"))
    (def client (pxshot/client \"px_your_api_key\" {:timeout 60000}))"
  ([api-key]
   (client api-key {}))
  ([api-key opts]
   {:api-key  api-key
    :base-url (or (:base-url opts) default-base-url)
    :timeout  (or (:timeout opts) default-timeout)}))

;;; ----------------------------------------------------------------------------
;;; Internal helpers
;;; ----------------------------------------------------------------------------

(defn- build-headers
  "Builds HTTP headers with authentication."
  [client]
  {"Authorization" (str "Bearer " (:api-key client))
   "Content-Type"  "application/json"
   "Accept"        "application/json"})

(defn- request-opts
  "Builds common request options."
  [client]
  {:headers          (build-headers client)
   :socket-timeout   (:timeout client)
   :connection-timeout (:timeout client)
   :throw-exceptions false})

(defn- handle-error
  "Handles error responses from the API."
  [response]
  (let [status (:status response)
        body   (try
                 (json/parse-string (:body response) true)
                 (catch Exception _
                   {:error {:message (:body response)}}))]
    (throw (ex-info (or (get-in body [:error :message])
                        (str "API error: " status))
                    {:type   :pxshot/api-error
                     :status status
                     :body   body}))))

(defn- snake->kebab
  "Converts snake_case keyword to kebab-case."
  [k]
  (keyword (clojure.string/replace (name k) "_" "-")))

(defn- kebab->snake
  "Converts kebab-case keyword to snake_case."
  [k]
  (keyword (clojure.string/replace (name k) "-" "_")))

(defn- transform-keys
  "Transforms all keys in a map using the given function."
  [f m]
  (into {} (map (fn [[k v]] [(f k) v]) m)))

;;; ----------------------------------------------------------------------------
;;; Screenshot API
;;; ----------------------------------------------------------------------------

(def ^:private screenshot-params
  "Valid screenshot parameters."
  #{:url :format :quality :width :height :full-page :wait-until
    :wait-for-selector :wait-for-timeout :device-scale-factor :store :block-ads})

(defn screenshot
  "Takes a screenshot of a URL.

  Arguments:
    client - A Pxshot client created with `client`
    opts   - Screenshot options map:
             :url                - URL to screenshot (required)
             :format             - Image format: \"png\", \"jpeg\", \"webp\" (default: \"png\")
             :quality            - JPEG/WebP quality 1-100 (default: 80)
             :width              - Viewport width in pixels (default: 1920)
             :height             - Viewport height in pixels (default: 1080)
             :full-page          - Capture full scrollable page (default: false)
             :wait-until         - Wait condition: \"load\", \"domcontentloaded\", \"networkidle\"
             :wait-for-selector  - CSS selector to wait for before capture
             :wait-for-timeout   - Additional wait time in ms after page load
             :device-scale-factor - Device scale factor for retina (default: 1)
             :store              - Store image and return URL (default: false)
             :block-ads          - Block ads and trackers (default: false)

  Returns:
    - When :store is false/nil: byte array of the image
    - When :store is true: map with :url, :expires-at, :width, :height, :size-bytes

  Throws:
    ExceptionInfo on API errors with :type :pxshot/api-error

  Examples:
    ;; Get screenshot as bytes
    (def image (screenshot client {:url \"https://example.com\"}))
    (io/copy (io/input-stream image) (io/file \"screenshot.png\"))

    ;; Get screenshot as stored URL
    (def result (screenshot client {:url \"https://example.com\"
                                    :store true
                                    :full-page true}))
    (:url result) ;; => \"https://storage.pxshot.com/...\""
  [client opts]
  (when-not (:url opts)
    (throw (ex-info "URL is required" {:type :pxshot/validation-error})))
  (let [url      (str (:base-url client) "/v1/screenshot")
        ;; Convert kebab-case to snake_case for API
        body     (->> (select-keys opts screenshot-params)
                      (transform-keys kebab->snake))
        store?   (:store opts)
        response (http/post url
                            (merge (request-opts client)
                                   {:body (json/generate-string body)
                                    :as   (if store? :json :byte-array)}))]
    (if (<= 200 (:status response) 299)
      (if store?
        ;; Convert response keys from snake_case to kebab-case
        (transform-keys snake->kebab (:body response))
        (:body response))
      (handle-error response))))

(defn screenshot!
  "Takes a screenshot, returning bytes or throwing on error.

  Same as `screenshot` but always returns bytes (ignores :store option).
  Useful when you need raw image data and want to be explicit about it.

  See `screenshot` for full documentation."
  [client opts]
  (screenshot client (dissoc opts :store)))

;;; ----------------------------------------------------------------------------
;;; Usage API
;;; ----------------------------------------------------------------------------

(defn usage
  "Gets API usage statistics.

  Arguments:
    client - A Pxshot client created with `client`

  Returns:
    A map with usage statistics (keys converted to kebab-case).

  Throws:
    ExceptionInfo on API errors with :type :pxshot/api-error

  Example:
    (usage client)
    ;; => {:screenshots-today 42
    ;;     :screenshots-month 1337
    ;;     :plan \"pro\"
    ;;     ...}"
  [client]
  (let [url      (str (:base-url client) "/v1/usage")
        response (http/get url
                           (merge (request-opts client)
                                  {:as :json}))]
    (if (<= 200 (:status response) 299)
      (transform-keys snake->kebab (:body response))
      (handle-error response))))

;;; ----------------------------------------------------------------------------
;;; Convenience functions
;;; ----------------------------------------------------------------------------

(defn screenshot-url
  "Takes a screenshot and returns a stored URL.

  Convenience function that always sets :store to true.

  Arguments:
    client - A Pxshot client
    opts   - Screenshot options (see `screenshot`)

  Returns:
    A map with :url, :expires-at, :width, :height, :size-bytes

  Example:
    (def result (screenshot-url client {:url \"https://example.com\"}))
    (:url result) ;; => \"https://storage.pxshot.com/...\""
  [client opts]
  (screenshot client (assoc opts :store true)))

(defn screenshot-bytes
  "Takes a screenshot and returns raw bytes.

  Convenience function that always sets :store to false.

  Arguments:
    client - A Pxshot client
    opts   - Screenshot options (see `screenshot`)

  Returns:
    Byte array of the image.

  Example:
    (def image (screenshot-bytes client {:url \"https://example.com\"}))
    (io/copy (io/input-stream image) (io/file \"shot.png\"))"
  [client opts]
  (screenshot client (dissoc opts :store)))

(defn save-screenshot
  "Takes a screenshot and saves it to a file.

  Arguments:
    client - A Pxshot client
    opts   - Screenshot options (see `screenshot`)
    path   - File path to save the image

  Returns:
    The file path.

  Example:
    (save-screenshot client {:url \"https://example.com\"} \"screenshot.png\")"
  [client opts path]
  (let [image (screenshot-bytes client opts)]
    (clojure.java.io/copy (java.io.ByteArrayInputStream. image)
                          (clojure.java.io/file path))
    path))
