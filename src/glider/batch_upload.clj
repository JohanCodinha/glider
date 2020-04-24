(ns glider.batch-upload
  (:require [clojure.test :refer [deftest is run-tests]]
            [glider.event-store.core :refer [append-to-stream uuid #_select]]
            [dk.ative.docjure.spreadsheet :as spreadsheet]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defn row->keyword [sheet row-number]
  (let [row (nth (spreadsheet/row-seq sheet) (dec row-number))]
    (->> (spreadsheet/cell-seq row)
         (map spreadsheet/read-cell))))

(defn spreadsheet-collumn->record-map [spreadsheetPath sheetName keys-row data-start-row]
  (let [workbook (spreadsheet/load-workbook "./resources/LathamSnipeBLAdata.xls")
        sheet (spreadsheet/select-sheet "Occurrence records" workbook)
        ks (row->keyword sheet keys-row)
        rows (spreadsheet/row-seq sheet)]
    (map #(into {} (keep (fn [[k v :as m]] (when v m))
                         (map vector ks (->> %
                                             spreadsheet/cell-seq
                                             (map spreadsheet/read-cell)))))
         (drop data-start-row rows))))

(comment
  (->> (spreadsheet-collumn->record-map 
         "./resources/LathamSnipeBLAdata.xls"
         "Occurrence records"
         3
         4)
       (map #(append-to-stream ::batch-upload %)))
  )
