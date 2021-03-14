(ns glider.domains.collaborator.collaborator
  (:require [glider.domains.collaborator.contact-method :as contact-method]
            [glider.domains.collaborator.address :as address]
            [malli.core :as m]
            [malli.generator :as mg]))

;;Data model
(def collaborator
[:map
 [::account-creation-date inst?]
 [::status [:enum
            "Active"
            "Inactive"
            "New user"
            "Deceased user"
            "Deleted"
            "Not Approved"]]
 [::legacy-Uid {:optional true} int?]
 [::uuid uuid?]
 [::given-name string?]
 [::surname string?]
 [::login-name string?]
 [::other-name {:optional true} string?]
 [::reason-of-use string?]
 [::role [:enum
          "Expert Reviewer"
          "View only"
          "Contributor"
          "Taxon Manager"
          "Administrator"]]
 [::batch-upload-access boolean?]
 [::restricted-viewing-access boolean?]
 [::terms-and-conditions-accepted-date {:optional true} inst?]
 [::contacts [:+ contact-method/Schema]]
 [::address [:+ address/Schema]]])

(mg/generate
 [:+
  [:multi
   {:dispatch :type}
   [:phone [:map [:type [:= :phone]] [:number string?]]]
   [:email
    [:map
     [:type [:= :email]]
     [:address string?]]]]])
(mg/generate
 collaborator
  
 )

[:schema {:registry {::cons [:maybe [:tuple pos-int? [:ref ::cons]]]}}
   ::cons]
(def contact
  [:multi {:dispatch :type
           :ref[:fax :phone :email]
           }
   [:fax [:map [:type keyword?] [:number string?]]]
   [:phone [:map [:type keyword?] [:number string?] ]]
   [:email [:map [:type keyword?] [:address string?]]]])

(m/validate [:+ contact] (mg/generate [:+ contact]))

{"UserInfoView_DS" [{"confirmUserPasswordTxt" nil
                     ;"creationTsp" 1313416800000
                     "preferredContactMethodCde" "pcme"
                     ;"statusCde" "active"
                     "userPasswordTxt" "Married2015"
                     ;"nameId" 757
                     "organisationId" 356
                     "modifiedTsp" 1508971815338
                     "changePasswordCde" "false"
                     ;"fullName" "Julie Whitfield"
                     ;"statusDesc" "Active"
                     "organisationNme" "Department of Environment Land Water & Planning - Loddon Mallee Region"
                     ;"roleDesc" "Contributor"
                     "primaryAddressId" 642
                     ;"roleCde" "con"
                     "primaryContactId" 1340
                     ;"loginNameNme" "Whi757"
                     ;"surnameNme" "Whitfield"
                     ;"otherNme" nil
                     ;"batchUploadViewCde" "true"
                     ;"givenNme" "Julie"
                     "lastSystemAccessTsp" 1508971815276
                     ;"userUid" 757
                     ;"reasonTxt" "DSE Contributor and data analysis"
                     ;"restrictedViewingCde" "true"
                     ;"dateAcceptedTcTsp" nil
                     }]
 "UserOrganisationLink_DS" [{"creationTsp" 1547730000000
                             "orgLinkStatus" "c"
                             "organisationId" 302
                             "modifiedTsp" nil
                             "organisationTitleTxt" nil
                             "organisationNme" "Department of Environment Land Water & Planning "
                             "curator" "n"
                             "statusMessage" nil
                             "userUid" 757
                             "startTsp" 1546261200000
                             "endTsp" 4102405200000}
                            {"creationTsp" 1385604138501
                             "orgLinkStatus" "c"
                             "organisationId" 903310
                             "modifiedTsp" 1609736230000
                             "organisationTitleTxt" "Consultant"
                             "organisationNme" "Amaryllis Environmental"
                             "curator" "n"
                             "statusMessage" nil
                             "userUid" 757
                             "startTsp" 1402976018339
                             "endTsp" 4102405200000}
                            {"creationTsp" nil
                             "orgLinkStatus" "c"
                             "organisationId" 356
                             "modifiedTsp" 1609736230000
                             "organisationTitleTxt" nil
                             "organisationNme" "Department of Environment Land Water & Planning - Loddon Mallee Region"
                             "curator" "n"
                             "statusMessage" nil
                             "userUid" 757
                             "startTsp" 1402976018000
                             "endTsp" 4102405200000}]
 "AddressDetail_DS" [{"addressId" 92
                      "creationTsp" 1346201961323
                      "mainAddressId" ""
                      "modifiedTsp" nil
                      "streetNme" "PO Box 3001 Bendigo Delivery Center"
                      "countryNme" "Australia"
                      "postcodeTxt" "3554"
                      "cityNme" "Epsom"
                      "streetNumberTxt" nil
                      "stateNme" "VIC"}
                     {"addressId" 82
                      "creationTsp" 1345794846653
                      "mainAddressId" ""
                      "modifiedTsp" nil
                      "streetNme" "1234"
                      "countryNme" "Australia"
                      "postcodeTxt" "123"
                      "cityNme" "Bendigo"
                      "streetNumberTxt" nil
                      "stateNme" "VIC"}
                     {"addressId" 642
                      "creationTsp" 1385938116735
                      "mainAddressId" ""
                      "modifiedTsp" nil
                      "streetNme" "Mackenzie Street west"
                      "countryNme" "Australia"
                      "postcodeTxt" "3555"
                      "cityNme" "Kangaroo Flat"
                      "streetNumberTxt" "248 "
                      "stateNme" "VIC"}]
 "ContactDetail_DS" [{"contactCde" "cde"
                      "contactId" 147
                      "creationTsp" 1346201961057
                      "emailOrPhoneTxt" "julie.whitfield@delwp.vic.gov.au"
                      "modifiedTsp" nil}
                     {"contactCde" "cdm"
                      "contactId" 148
                      "creationTsp" 1346201961089
                      "emailOrPhoneTxt" "0407 340 729"
                      "modifiedTsp" nil}
                     {"contactCde" "cdw"
                      "contactId" 149
                      "creationTsp" 1346201961120
                      "emailOrPhoneTxt" "03 5430 4461"
                      "modifiedTsp" nil}
                     {"contactCde" "cde"
                      "contactId" 1339
                      "creationTsp" 1385604138110
                      "emailOrPhoneTxt" "amaryllisenvironmental@hotmail.com"
                      "modifiedTsp" nil}
                     {"contactCde" "cdm"
                      "contactId" 1340
                      "creationTsp" 1385604263657
                      "emailOrPhoneTxt" "0400 909 073"
                      "modifiedTsp" nil}]}

{"UserInfoView_DS" [{"confirmUserPasswordTxt" nil
                     "creationTsp" 1313416800000
                     "preferredContactMethodCde" "pcmm"
                     "statusCde" "active"
                     "userPasswordTxt" "vba123"
                     "nameId" 2
                     "organisationId" 349
                     "modifiedTsp" 1614913232348
                     "changePasswordCde" "false"
                     "fullName" "David Cameron"
                     "statusDesc" "Active"
                     "organisationNme" "Department of Environment Land Water & Planning - Environment & Natural Resources Division"
                     "roleDesc" "Administrator"
                     "primaryAddressId" 1
                     "roleCde" "admin"
                     "primaryContactId" 1
                     "loginNameNme" "Cam200"
                     "surnameNme" "Cameron"
                     "otherNme" nil
                     "batchUploadViewCde" "false"
                     "givenNme" "David"
                     "lastSystemAccessTsp" 1614913232331
                     "userUid" 2
                     "reasonTxt" "Taxon Manager"
                     "restrictedViewingCde" "true"
                     "dateAcceptedTcTsp" nil}]
 "UserOrganisationLink_DS" [{"creationTsp" 1313416800000
                             "orgLinkStatus" "c"
                             "organisationId" 349
                             "modifiedTsp" 1609736230000
                             "organisationTitleTxt" "Senior Botanist"
                             "organisationNme" "Department of Environment Land Water & Planning - Environment & Natural Resources Division"
                             "curator" "n"
                             "statusMessage" nil
                             "userUid" 2
                             "startTsp" 1402976018000
                             "endTsp" 4102405200000}
                            {"creationTsp" 1547470800000
                             "orgLinkStatus" "c"
                             "organisationId" 302
                             "modifiedTsp" nil
                             "organisationTitleTxt" nil
                             "organisationNme" "Department of Environment Land Water & Planning "
                             "curator" "n"
                             "statusMessage" nil
                             "userUid" 2
                             "startTsp" 1546261200000
                             "endTsp" 4102405200000}]
 "AddressDetail_DS" [{"addressId" 1
                      "creationTsp" 1313416800000
                      "mainAddressId" ""
                      "modifiedTsp" nil
                      "streetNme" nil
                      "countryNme" nil
                      "postcodeTxt" nil
                      "cityNme" nil
                      "streetNumberTxt" nil
                      "stateNme" nil}]
 "ContactDetail_DS" [{"contactCde" nil
                      "contactId" 1
                      "creationTsp" nil
                      "emailOrPhoneTxt" nil
                      "modifiedTsp" nil}]}
