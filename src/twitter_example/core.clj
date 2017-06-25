(ns twitter-example.core
 (:require [clojure.set]
           [clojure.string :as s]
           [clj-http.client :as client]
           [overtone.at-at :as overtone]
           [twitter.api.restful :as twitter]
           [twitter.oauth :as twitter-oauth]
           [environ.core :refer [env]]))

; We are generating tweets based on templates, similar to the game Apples to
; Apples: https://en.wikipedia.org/wiki/Apples_to_Apples
; We start with two lists of strings: One list contains string with blank
; spaces, the other list is used to fill in these spaces.



; We define the "templates" - these strings are the skeleton of the tweet
; We will later replace every occurence of ___ with a string that we chose
; randomly from the list "blanks".
;(def templates ["Seems city hall is serving ___ these days"
;                "The police arrested ___ in connection with the robbery."
;                "I don't know if ___ will visit us next Sunday."
;                "She sent a card to ___."
;                "Doing that sort of thing makes you look like ___."


(def templates ["Seems the __p were right, ___ will eat us alive"
                "Thankfully the __p predicted ___ many years ago."
                "I don't know if the __p of 2017 would have agreed with ___."
                "__p confirmed ___."
                "Doing that sort of thing makes you look like a __p member."])

; Next we define the "blanks"
(def blanks ["mass emigration"
             "more government intervention"
             "greybow parade"
             "unsocial welfare"
             "self driving bicycles"])

; Next we define the "blanks"
(def parties ["socialists"
              "conservatives"
              "liberals"
              "green party"
              "right-wingnuts"])

; generate-sentence returns a random sentence, built by choosing one template
; string at random and filling in the blank space (___) with a randomly chosen
; string from the blanks list.
(defn generate-sentence []
  (let [template (rand-nth templates)
        party (rand-nth parties)
        blank (rand-nth blanks)
        template-with-party (s/replace template "__p" party)]
      (s/replace template-with-party "___" blank)))


; Tweets are limited to 140 characters. We might randomly generate a sentence
; with more than 140 characters, which would be rejected by Twitter.
; So we check if our generated sentence is longer than 140 characters, an if
; it is we try again.
(defn tweet-text []
  (let [tweet (generate-sentence)]
    (if (<= (count tweet) 140)
     tweet
     (recur))))

; We retrieve the twitter credentials from the profiles.clj file here.
; In profiles.clj we defined the "env(ironment)" which we use here
; to get the secret passwords we need.
; Make sure you add your credentials in profiles.clj and not here!
(def twitter-credentials (twitter-oauth/make-oauth-creds (env :app-consumer-key)
                                                         (env :app-consumer-secret)
                                                         (env :user-access-token)
                                                         (env :user-access-secret)))

; Sends the tweet.
(defn status-update []
 (let [text (tweet-text)]
   (when (not-empty text)
     (try (twitter/statuses-update :oauth-creds twitter-credentials
                                   :params {:status text})
          (catch Exception e (println "Something went wrong: " (.getMessage e)))))))

(def my-pool (overtone/mk-pool))

(defn -main [& args]
  ;; every 2 hours
  (println "Started up")
  (println (tweet-text))
  (overtone/every (* 1000 60 60 2) #(println (status-update)) my-pool))
