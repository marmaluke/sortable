(ns sortable.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"
                      :colors ["Red" "Green" "Blue" "Yellow" "Black" "White" "Orange"]}))

(def placeholder
  (doto (. js/document (createElement "li"))
    (set! -className "placeholder")))

(defn split-after [pred coll]
  (let [[l r] (split-with pred coll)]
    [(concat l [(first r)]) (rest r)]))

(defn move-before [coll placement item target]
  (let [split (partial (if (= placement :before)
                         split-with
                         split-after)
                       #(not= target %))
        join #(apply conj (first %) item (last %))]
    (->> coll
         (remove #(= item %))
         split
         (map vec)
         join)))

(defn list-component []
  (let [colors (atom ["Red" "Green" "Blue" "Yellow" "Black" "White" "Orange"])
        dragged (atom nil)
        over (atom nil)
        node-placement (atom nil)

        start-drag (fn [e]
                     (println "start dragging")
                     (reset! dragged (.-currentTarget e))
                     (set! (.. e -dataTransfer -effectAllowed) "move")
                     (. (.-dataTransfer e) (setData "text/html" (.-currentTarget e))))

        end-drag (fn [e]
                   (println "stop dragging")
                   (set! (.. @dragged -style -display) "block")
                   (. (.. @dragged -parentNode) (removeChild placeholder))
                   (swap! colors
                          move-before @node-placement (.. @dragged -dataset -id) (.. @over -dataset -id)))

        drag-over (fn [e]
                    (println "dragging over")
                    (.preventDefault e)
                    (set! (.. @dragged -style -display) "none")
                    (when-not (= (.. e -target -className) "placeholder")
                      (reset! over (.-target e))
                      (let [rel-y (- (.-clientY e) (.-offsetTop @over))
                            height (/ (.-offsetHeight @over) 2)
                            parent (.. e -target -parentNode)]
                        (if (> rel-y height)
                          (do
                            (reset! node-placement :after)
                            (.insertBefore parent placeholder (.. e -target -nextElementSibling)))
                          (do
                            (reset! node-placement :before)
                            (.insertBefore parent placeholder (.. e -target)))))))]

    (fn []
      [:ul {:on-drag-over drag-over}
       (for [color @colors]
         ^{:key color} [:li {:data-id color
                             :draggable true
                             :on-drag-start start-drag
                             :on-drag-end end-drag}
                        color])])))

(defn hello []
  [:h1 (:text @app-state)])

(reagent/render-component
 [list-component]
 (. js/document (getElementById "app")))
