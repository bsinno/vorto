/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
define(["../init/appController"], function (repositoryControllers) {

  repositoryControllers.controller('RemoveAccountModalController',
      ['$location', '$scope', '$rootScope', '$http', '$uibModalInstance',
        '$window',
        function ($location, $scope, $rootScope, $http, $uibModalInstance) {

          $scope.isOnlyAdminForNamespaces = function () {
            $http
            .get("./rest/namespaces/userIsOnlyAdmin")
            .then(
                function (result) {
                  // result.data cannot be undefined, i.e. falsey, so either
                  // this or error
                  $scope.onlyNamespaceAdmin = result.data;
                },
                function (error) {
                  // no error handling within context but setting onlyNamespaceAdmin
                  // as false for safety
                  $scope.onlyNamespaceAdmin = true;
                }
            );
          };

          $scope.isOnlyAdminForNamespaces();

          $scope.deleteAccount = function () {
            $http.delete('./rest/accounts',
                {
                  params:
                      {
                        username: $rootScope.userInfo.name,
                        authenticationProvider: $rootScope.userInfo.provider.id
                      }
                }
            )
            .then(
                function (response) {
                  $scope.user = response.data;
                  if (response.status === 204) {
                    $rootScope.logout();
                    $uibModalInstance.dismiss("cancel");
                  }
                },
                function(error){
                  console.log(error);
                }
            );
          };

          $scope.cancel = function () {
            $uibModalInstance.dismiss("cancel");
          };
        }]);

  /*global repositoryControllers*/
  repositoryControllers.controller("LoginController",
      ["$location", "$scope", "$rootScope",
        function ($location, $scope, $rootScope) {

          if ($rootScope.authenticated === true) {
            $location.path("/");
          }
        }]);

});
