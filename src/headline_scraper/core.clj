(ns headline-scraper.core
  (:gen-class)
  (:use [hiccup.core])
  (:require [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [clojure.core.memoize :as memoize]
            [org.httpkit.server :as httpkit]))

(def iltalehti { :url "http://www.iltalehti.fi"
                 :encoding "ISO-8859-1"
                 :titles [:div#iltab_luetuimmat-kaikki1 :> :p :> :a :span.list-title]
                 :links [:div#iltab_luetuimmat-kaikki1 :> :p :> :a] })

(def iltasanomat { :url "http://www.iltasanomat.fi"
                   :encoding "UTF-8"
                   :titles [:div.most-read :> :div.tabs-content :> :ol.is-most-read-articles-list
                            :li.list-item :> :div.content :> :p]
                   :links [:div.most-read :> :div.tabs-content :>
                           :ol.is-most-read-articles-list :> :li.list-item :a] })

(defn fetch-url [url encoding]
  (try
    (-> url java.net.URL.
        .getContent (java.io.InputStreamReader. encoding)
        html/html-resource)
    (catch Exception e)))

(def fetch-site
  (memoize/ttl #(fetch-url (:url %) (:encoding %)) {} :ttl/threshold 1000))

(defn parse-href [a]
  (:href (:attrs a)))

(defn parse-content [node]
  (first (:content node)))

(defn get-iltasanomat-links []
  (take 10
    (map #(str (:url iltasanomat) (parse-href %))
      (html/select (fetch-site iltasanomat) (:links iltasanomat)))))

(defn get-iltasanomat-titles []
  (take 10
    (map parse-content
      (html/select (fetch-site iltasanomat) (:titles iltasanomat)))))

(defn get-iltalehti-links []
  (map parse-href
    (html/select (fetch-site iltalehti) (:links iltalehti))))

(defn get-iltalehti-titles []
  (map parse-content
    (html/select (fetch-site iltalehti) (:titles iltalehti))))

(defn get-most-read []
  (let [actions [get-iltalehti-titles get-iltalehti-links get-iltasanomat-titles get-iltasanomat-links]
        futures (doall (map #(future (apply % [])) actions))
        results (map deref futures)
        [il-titles il-links is-titles is-links] results
        pairs (partition 2 (interleave il-titles il-links is-titles is-links))]
    pairs))

(defonce server (atom nil))

(defn app [req]
  (let [news (get-most-read)]
    (if (or (nil? news)
            (zero? (count news)))
      { :status  500
        :headers {"Content-Type" "text/plain"}
        :body    "Internal Server Error" }
      { :status  200
        :headers {"Content-Type" "application/json; charset=UTF-8"}
        :body    (json/write-str news :escape-unicode false :escape-slash false) })))

(defn -main [& args]
  (reset! server (httpkit/run-server #'app {:port 8080})))
