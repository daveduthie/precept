(ns libx.todomvc.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [devtools.core :as devtools]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [secretary.core :as secretary]
            [libx.todomvc.events]
            [libx.todomvc.subs]
            [libx.todomvc.views]
            [libx.todomvc.rules :refer [app-session]]
            [libx.todomvc.facts :refer [visibility-filter]]
            [libx.core :refer [router changes-ch registry]]
            [libx.util :refer [insert-fire]]
            [libx.todomvc.add-me :as add-me])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

;; Instead of secretary consider:
;;   - https://github.com/DomKM/silk
;;   - https://github.com/juxt/bidi
(defroute "/" [] (dispatch [:set-showing :all]))
(defroute "/:filter" [filter] (dispatch [:set-showing (keyword filter)]))

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn mount-components []
  (reagent/render [libx.todomvc.views/todo-app] (.getElementById js/document "app")))

(defn init-router! []
  (let [r (router changes-ch registry)]
    (println "Router initialized" r)
    r))

(init-router!)

;(add-me/send! changes-ch {:db/id -1 :done-count 0 :op :add})
;(.log js/console "Registry" (libx.core/registry))
(defn ^:export main []
  (let [initial-state (-> app-session
                        (add-me/replace-listener)
                        (insert-fire (visibility-filter (random-uuid) :all)))
        changes (add-me/ops initial-state)
        _ (println "Initial state" changes)]
    (dispatch-sync [:initialise-db initial-state])
    (mount-components)))
