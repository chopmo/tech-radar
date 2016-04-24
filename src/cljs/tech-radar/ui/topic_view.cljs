(ns tech-radar.ui.topic-view
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :refer-macros [html]]
            [tech-radar.state :refer [app-state]]
            [tech-radar.utils.text-formatter :refer [format]]
            [tech-radar.ui.loading-view :refer [loading-view]]))

(defn- format-time-number [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(defn- time->str [t]
  (let [hours   (-> (.getHours t)
                    (format-time-number))
        minutes (-> (.getMinutes t)
                    (format-time-number))]
    (str hours ":" minutes)))

(defui TextItem
  Object
  (render [this]
    (let [{:keys [id text]} (om/props this)]
      (html
        [:div {}
         (mapv identity (format text id))]))))

(def text-item (om/factory TextItem))

(defui TopicItem
  Object
  (render [this]
    (let [{:keys [id created-at text]} (om/props this)]
      (html
        [:tr {}
         [:td {} (time->str created-at)]
         [:td {} (text-item {:id   id
                             :text text})]]))))

(def topic-item (om/factory TopicItem {:keyfn :id}))

(defui TableView
  static om/IQuery
  (query [this]
    [:records-per-page :texts])
  Object
  (render [this]
    (let [{:keys [texts records-per-page]} (om/props this)]
      (html
        [:div {:class "table-responsive"}
         [:table {:class "table table-bordered table-hover table-striped"}
          [:thead {}
           [:tr {}
            [:th {} "Time"]
            [:th {} "Text"]]]
          [:tbody
           (->> texts
                (take records-per-page)
                (mapv topic-item))]]]))))

(def table-view (om/factory TableView))

(defn- topic-name [topic-items current-topic]
  (->> topic-items
       (current-topic)
       (:name)))

(defui TopicView
  static om/IQuery
  (query [this]
    [:name :texts :records-per-page])
  Object
  (render [this]
    (let [{:keys [current-topic topics topic-items records-per-page]} (om/props this)
          name  (topic-name topic-items current-topic)
          texts (current-topic topics)]
      (html
        [:div.container-fluid {}
         [:div {:class "row"}
          [:div {:class "col-lg-12"}
           [:h3 {:class "page-header"} name]]]
         [:div {:class "row"}
          [:div {:class "col-lg-12"}
           (if (seq texts)
             (table-view {:texts            texts
                          :records-per-page records-per-page})
             (loading-view {:text "Loading texts, please wait."}))]]]))))

(def topic-view (om/factory TopicView))

