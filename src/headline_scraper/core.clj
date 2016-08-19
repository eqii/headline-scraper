(ns headline-scraper.core
  (:gen-class)
  (:use [hiccup.core])
  (:require [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [clojure.core.memoize :as memoize]))


(def iltalehti-url "http://www.iltalehti.fi")
(def iltasanomat-url "http://www.iltasanomat.fi")

(def iltalehti-selectors { :titles [:div#iltab_luetuimmat-kaikki1 :> :p :> :a :span.list-title]
                           :links [:div#iltab_luetuimmat-kaikki1 :> :p :> :a] })

(def iltasanomat-selectors { :titles [:div.most-read :> :div.tabs-content :> :ol.is-most-read-articles-list
                                      :li.list-item :> :div.content :> :p]
                             :links [:div.most-read :> :div.tabs-content :>
                                     :ol.is-most-read-articles-list :> :li.list-item :a] })

(defn fetch-url [url encoding]
  (-> url java.net.URL.
      .getContent (java.io.InputStreamReader. encoding)
      html/html-resource))

(def fetch-iltasanomat
  (memoize/ttl #(fetch-url iltasanomat-url "UTF-8") {} :ttl/threshold 1000))

(def fetch-iltalehti
  (memoize/ttl #(fetch-url iltalehti-url "ISO-8859-1") {} :ttl/threshold 1000))

(defn parse-href [a]
  (:href (:attrs a)))

(defn parse-content [node]
  (first (:content node)))

(defn get-iltasanomat-links []
  (take 10
    (map #(str iltasanomat-url (parse-href %))
      (html/select (fetch-iltasanomat) (:links iltasanomat-selectors)))))

(defn get-iltasanomat-titles []
  (take 10
    (map parse-content
      (html/select (fetch-iltasanomat) (:titles iltasanomat-selectors )))))

(defn get-iltalehti-links []
  (map parse-href
    (html/select (fetch-iltalehti) (:links iltalehti-selectors))))

(defn get-iltalehti-titles []
  (map parse-content
    (html/select (fetch-iltalehti) (:titles iltalehti-selectors))))

(defn get-most-read []
  (let [actions [get-iltalehti-titles get-iltalehti-links get-iltasanomat-titles get-iltasanomat-links]
        futures (doall (map #(future (apply % [])) actions))
        results (map deref futures)
        [il-titles il-links is-titles is-links] results
        pairs (partition 2 (interleave il-titles il-links is-titles is-links))]
    pairs))

;(println (json/write-str {:ilta (get-most-read)}))
