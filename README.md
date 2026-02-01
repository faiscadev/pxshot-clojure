# Pxshot Clojure SDK

Official Clojure SDK for the [Pxshot](https://pxshot.com) screenshot API.

[![Clojars Project](https://img.shields.io/clojars/v/com.pxshot/pxshot.svg)](https://clojars.org/com.pxshot/pxshot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

## Installation

### deps.edn

```clojure
com.pxshot/pxshot {:mvn/version "0.1.0"}
```

### Leiningen/Boot

```clojure
[com.pxshot/pxshot "0.1.0"]
```

## Quick Start

```clojure
(require '[pxshot.core :as pxshot])

;; Create a client with your API key
(def client (pxshot/client "px_your_api_key"))

;; Take a screenshot (returns bytes)
(def image (pxshot/screenshot client {:url "https://example.com"}))

;; Save to file
(pxshot/save-screenshot client {:url "https://example.com"} "screenshot.png")

;; Store screenshot and get URL
(def result (pxshot/screenshot client {:url "https://example.com" :store true}))
(:url result)
;; => "https://storage.pxshot.com/..."

;; Check usage
(pxshot/usage client)
```

## API Reference

### Client

```clojure
(pxshot/client api-key)
(pxshot/client api-key {:base-url "https://api.pxshot.com"
                        :timeout 30000})
```

Creates a Pxshot client. Options:
- `:base-url` - API base URL (default: `https://api.pxshot.com`)
- `:timeout` - Request timeout in milliseconds (default: `30000`)

### Screenshot

```clojure
(pxshot/screenshot client {:url "https://example.com"
                           :format "png"
                           :quality 80
                           :width 1920
                           :height 1080
                           :full-page false
                           :wait-until "load"
                           :wait-for-selector ".content"
                           :wait-for-timeout 1000
                           :device-scale-factor 2
                           :block-ads true
                           :store false})
```

Takes a screenshot of a URL. Options:

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `:url` | string | required | URL to screenshot |
| `:format` | string | `"png"` | Image format: `"png"`, `"jpeg"`, `"webp"` |
| `:quality` | integer | `80` | JPEG/WebP quality (1-100) |
| `:width` | integer | `1920` | Viewport width in pixels |
| `:height` | integer | `1080` | Viewport height in pixels |
| `:full-page` | boolean | `false` | Capture full scrollable page |
| `:wait-until` | string | `"load"` | Wait condition: `"load"`, `"domcontentloaded"`, `"networkidle"` |
| `:wait-for-selector` | string | | CSS selector to wait for |
| `:wait-for-timeout` | integer | | Additional wait time in ms |
| `:device-scale-factor` | number | `1` | Device scale factor (for retina) |
| `:block-ads` | boolean | `false` | Block ads and trackers |
| `:store` | boolean | `false` | Store image and return URL |

**Returns:**
- When `:store` is `false`: byte array of the image
- When `:store` is `true`: map with `:url`, `:expires-at`, `:width`, `:height`, `:size-bytes`

### Convenience Functions

```clojure
;; Always returns bytes (ignores :store)
(pxshot/screenshot-bytes client {:url "https://example.com"})

;; Always returns stored URL info
(pxshot/screenshot-url client {:url "https://example.com"})

;; Take screenshot and save to file
(pxshot/save-screenshot client {:url "https://example.com"} "output.png")
```

### Usage

```clojure
(pxshot/usage client)
;; => {:screenshots-today 42
;;     :screenshots-month 1337
;;     :plan "pro"
;;     ...}
```

Returns your API usage statistics.

## Examples

### Full Page Screenshot

```clojure
(pxshot/save-screenshot client
                        {:url "https://example.com"
                         :full-page true
                         :format "jpeg"
                         :quality 90}
                        "full-page.jpg")
```

### Wait for Dynamic Content

```clojure
(pxshot/screenshot client {:url "https://spa-example.com"
                           :wait-until "networkidle"
                           :wait-for-selector ".loaded-content"
                           :wait-for-timeout 2000})
```

### High DPI / Retina Screenshot

```clojure
(pxshot/screenshot client {:url "https://example.com"
                           :device-scale-factor 2
                           :width 1920
                           :height 1080})
```

### Store and Get URL

```clojure
(let [result (pxshot/screenshot-url client {:url "https://example.com"})]
  (println "Screenshot URL:" (:url result))
  (println "Expires at:" (:expires-at result))
  (println "Size:" (:size-bytes result) "bytes"))
```

## Error Handling

The SDK throws `ExceptionInfo` on errors:

```clojure
(try
  (pxshot/screenshot client {:url "https://example.com"})
  (catch clojure.lang.ExceptionInfo e
    (let [data (ex-data e)]
      (case (:type data)
        :pxshot/validation-error
        (println "Validation error:" (ex-message e))

        :pxshot/api-error
        (println "API error:" (ex-message e)
                 "Status:" (:status data))

        (throw e)))))
```

## Development

### Run Tests

```bash
# Unit tests
clj -M:test

# With Leiningen
lein test

# Integration tests (requires API key)
PXSHOT_API_KEY=px_your_key clj -M:test
```

### Build JAR

```bash
clj -T:build jar
```

### Deploy to Clojars

```bash
export CLOJARS_USERNAME=your_username
export CLOJARS_PASSWORD=your_deploy_token
clj -T:build deploy
```

## Requirements

- Clojure 1.11+
- Java 11+

## License

MIT License - see [LICENSE](LICENSE) for details.
