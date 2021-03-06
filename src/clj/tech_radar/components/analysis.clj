(ns tech-radar.components.analysis
  (:require [tech-radar.services.analysis :refer [load-data
                                                  run]]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan close!]]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [tech-radar.utils.parsers :refer [parse-int]]
            [tech-radar.utils.settings :refer [load-classify-settings]]
            [tech-radar.analytics.model :refer [new-model]]
            [tech-radar.analytics.protocols :refer [init
                                                    trends
                                                    topic]]))

(defrecord Analysis [database metrics preprocessor get-trends-fn get-texts-fn]
  component/Lifecycle
  (start [component]
    (if get-trends-fn
      component
      (do
        (timbre/info "Initializing analysis")
        (let [{:keys [topics]} (load-classify-settings)
              max-hashtags-per-trend (-> env
                                         (:max-hashtags-per-trend)
                                         (parse-int))
              max-texts-per-request  (-> env
                                         (:max-texts-per-request)
                                         (parse-int))
              load-data-hours        (-> env
                                         (:load-data-hours)
                                         (parse-int))
              model                  (new-model {:max-hashtags-per-trend max-hashtags-per-trend
                                                 :max-texts-per-request  max-texts-per-request})
              database*              (:database database)
              topics*                (map first topics)
              initial-data           (load-data database* topics* load-data-hours)]
          (init model initial-data)
          (run {:database        database*
                :topics          topics*
                :load-data-hours load-data-hours
                :model           model
                :analysis-chan   (:analysis-chan preprocessor)
                :metrics         metrics})
          (assoc component :get-trends-fn (fn []
                                            (trends model))
                           :get-texts-fn (fn [topic*]
                                           (topic model topic*)))))))
  (stop [component]
    (when get-trends-fn
      (timbre/info "Stopping analysis")
      (dissoc component :get-trends-fn :get-texts-fn))))

(defn new-analysis []
  (map->Analysis {}))
