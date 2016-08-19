(ns tm-news.core
  (:gen-class)
  (:use [hiccup.core])
  (:require [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [clojure.string :as string]))


(def iltalehti-url "http://www.iltalehti.fi")
(def iltasanomat-url "http://www.iltasanomat.fi")

(defn fetch-url [url encoding]
  (-> url java.net.URL.
      .getContent (java.io.InputStreamReader. encoding)
      html/html-resource))

(defn parse-href [a]
  (:href (:attrs a)))

(defn parse-content [node]
  (first (:content node)))

(defn get-is-links []
  (take 10 
    (map #(str iltasanomat-url (parse-href %))
      (html/select (fetch-url iltasanomat-url "UTF-8") [:div.most-read :> :div.tabs-content :> :ol.is-most-read-articles-list :> :li.list-item :a]))))

(defn get-is-titles []
  (take 10 
    (map parse-content
      (html/select (fetch-url iltasanomat-url "UTF-8") [:div.most-read :> :div.tabs-content :> :ol.is-most-read-articles-list :li.list-item :> :div.content :> :p]))))

(defn get-il-links []
  (map parse-href 
    (html/select (fetch-url iltalehti-url "ISO-8859-1") [:div#iltab_luetuimmat-kaikki1 :> :p :> :a])))

(defn get-il-titles []
  (map parse-content
    (html/select (fetch-url iltalehti-url "ISO-8859-1") [:div#iltab_luetuimmat-kaikki1 :> :p :> :a :span.list-title])))

(defn get-most-read []
  (let [actions [get-il-titles get-il-links get-is-titles get-is-links]
        futures (doall (map #(future (apply % [])) actions))
        results (map deref futures)
        [il-titles il-links is-titles is-links] results
        pairs (partition 2 (interleave il-titles il-links is-titles is-links))]
    pairs))

;(println (json/write-str {:ilta (get-most-read)}))