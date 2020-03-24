(ns glider.batch-upload
  (:require [clojure.test :refer [deftest is run-tests]]
            [dk.ative.docjure.spreadsheet :as spreadsheet]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defn row->keyword [sheet row-number]
  (let [row (nth (spreadsheet/row-seq sheet) (dec row-number))]
    (->> (spreadsheet/cell-seq row)
         (map spreadsheet/read-cell)
         #_(map #(when % (->kebab-case-keyword %))))))

(defn row->record [ks sheet rowr]
  (let [row (->> (nth (spreadsheet/row-seq sheet) (dec row-number))
                 spreadsheet/cell-seq
                 (map spreadsheet/read-cell))]
    (prn ks row)
   (into {} (map vector ks row))))

(let [workbook (spreadsheet/load-workbook "./resources/LathamSnipeBLAdata.xls")
      sheet (spreadsheet/select-sheet "Occurrence records" workbook)
      ks (row->keyword sheet 3)]
  (map row->record ks sheet 4))

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

(deftest test-spreadsheet-collumn->record-map
  (prn (spreadsheet-collumn->record-map 
         "./resources/LathamSnipeBLAdata.xls"
         "Occurrence records"
         2
         3)))

(defn alphabet-seq-gen
  ([s] (concat s [(mapcat
                     #(map str (repeat %) (map (comp str char) (range (int \A) (inc (int \Z)))))
                     (last s))])))

(def alphabet-seq
  (->> (iterate alphabet-seq-gen [(map (comp str char) (range (int \A) (inc (int \Z))))])
       (take 80)
       last
       flatten))
