(ns todomvc.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [secretary.core :as secretary]
            [todomvc.events]
            [todomvc.subs]
            [todomvc.views]
            [devtools.core :as devtools]
            [todomvc.rules :refer [app-session]]
            [todomvc.facts :refer [visibility-filter]]
            [libx.core :refer [router changes-ch registry]]
            [libx.util :refer [insert-fire]]
            [todomvc.add-me :as add-me])
  (:import [goog History]
           [goog.history EventType]))


;; -- Debugging aids ----------------------------------------------------------
(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)   ;; so that println writes to `console.log`

;; -- Routes and History ------------------------------------------------------
;; Although we use the secretary library below, that's mostly a historical
;; accident. You might also consider using:
;;   - https://github.com/DomKM/silk
;;   - https://github.com/juxt/bidi
;; We don't have a strong opinion.
;;
(defroute "/" [] (dispatch [:set-showing :all]))
(defroute "/:filter" [filter] (dispatch [:set-showing (keyword filter)]))

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


;; -- Entry Point -------------------------------------------------------------
;; Within ../../resources/public/index.html you'll see this code
;;    window.onload = function () {
;;      libx.core.main();
;;    }
;; So this is the entry function that kicks off the app once the HTML is loaded.
;;
(defn init-router! []
  (let [r (router changes-ch registry)]
    (println "Router initialized" r)
    r))

(init-router!)

(add-me/send! changes-ch {:added [[-1 :done-count 0]]})
(defn ^:export main []
  (let [initial-state (-> app-session
                        (add-me/replace-listener)
                        (insert-fire (visibility-filter (random-uuid) :all)))
        changes (add-me/ops initial-state)
        _ (println "Initial state" changes)]
    (dispatch-sync [:initialise-db initial-state])
    (reagent/render [todomvc.views/todo-app] (.getElementById js/document "app"))))
