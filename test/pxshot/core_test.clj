(ns pxshot.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [pxshot.core :as pxshot]))

(deftest client-test
  (testing "creates client with defaults"
    (let [c (pxshot/client "px_test_key")]
      (is (= "px_test_key" (:api-key c)))
      (is (= "https://api.pxshot.com" (:base-url c)))
      (is (= 30000 (:timeout c)))))

  (testing "creates client with custom options"
    (let [c (pxshot/client "px_test_key" {:base-url "https://custom.api.com"
                                          :timeout  60000})]
      (is (= "px_test_key" (:api-key c)))
      (is (= "https://custom.api.com" (:base-url c)))
      (is (= 60000 (:timeout c))))))

(deftest screenshot-validation-test
  (testing "throws when URL is missing"
    (let [c (pxshot/client "px_test_key")]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"URL is required"
                            (pxshot/screenshot c {}))))))

(deftest key-transformation-test
  (testing "internal key transformation functions"
    ;; Access private functions for testing
    (let [snake->kebab #'pxshot.core/snake->kebab
          kebab->snake #'pxshot.core/kebab->snake
          transform-keys #'pxshot.core/transform-keys]
      (is (= :full-page (snake->kebab :full_page)))
      (is (= :wait-for-selector (snake->kebab :wait_for_selector)))
      (is (= :full_page (kebab->snake :full-page)))
      (is (= :wait_for_selector (kebab->snake :wait-for-selector)))
      (is (= {:full-page true :url "test"}
             (transform-keys snake->kebab {:full_page true :url "test"}))))))

;; Integration tests (require actual API key)
;; Set PXSHOT_API_KEY environment variable to run these

(deftest ^:integration screenshot-integration-test
  (when-let [api-key (System/getenv "PXSHOT_API_KEY")]
    (let [c (pxshot/client api-key)]
      (testing "takes screenshot as bytes"
        (let [image (pxshot/screenshot c {:url "https://example.com"
                                          :width 800
                                          :height 600})]
          (is (bytes? image))
          (is (pos? (alength image)))))

      (testing "takes screenshot as stored URL"
        (let [result (pxshot/screenshot c {:url "https://example.com"
                                           :store true})]
          (is (string? (:url result)))
          (is (string? (:expires-at result)))
          (is (pos? (:width result)))
          (is (pos? (:height result)))
          (is (pos? (:size-bytes result))))))))

(deftest ^:integration usage-integration-test
  (when-let [api-key (System/getenv "PXSHOT_API_KEY")]
    (let [c (pxshot/client api-key)]
      (testing "gets usage stats"
        (let [usage (pxshot/usage c)]
          (is (map? usage)))))))
