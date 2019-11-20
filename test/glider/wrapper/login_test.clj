(ns glider.wrapper.login-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [clj-http.client :refer [request] :as http]
            [glider.wrapper.login :as login]))


(deftest test-extract->cookie
  (let [http-response
        {:headers
         {"Set-Cookie"
          ["JSESSIONID=FAE753B185A2E51D01E74B697834804A.worker1; Path=/vba/; HttpOnly"
           "NSC_JOxowpqucj3yneeb0fo0hbduv21smct=ffffffff0935322c45525d5f4f58455e445a4a423660;expires=Mon, 18-Nov-2019 11:11:58 GMT;path=/;secure;httponly"]}}]
  (is (= "JSESSIONID=FAE753B185A2E51D01E74B697834804A.worker1"
         (login/extract->cookie http-response)))))

