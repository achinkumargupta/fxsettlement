"use strict";

// Define your backend here.
angular.module('demoAppModule', ['ui.bootstrap']).controller('DemoAppCtrl', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/iou/";

    // Retrieves the identity of this and other nodes.
    let peers = [];
    let supportedCurrencies = [];
    $http.get(apiBaseURL + "me").then((response) => {demoApp.thisNode = response.data.me;console.log(response.data)});
    $http.get(apiBaseURL + "myorg").then((response) => {demoApp.thisNodeOrg = response.data.me;console.log(response.data)});
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);
    $http.get(apiBaseURL + "supported-currencies").then((response) => {supportedCurrencies = response.data.supportedCurrencies;console.log(response.data)});

    /** Displays the IOU creation modal. */
    demoApp.openCreateIOUModal = () => {
        const createIOUModal = $uibModal.open({
            templateUrl: 'createIOUModal.html',
            controller: 'CreateIOUModalCtrl',
            controllerAs: 'createIOUModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                supportedCurrencies: () => supportedCurrencies
            }
        });

        // Ignores the modal result events.
        createIOUModal.result.then(() => {}, () => {});
    };

    /** Displays the cash issuance modal. */
    demoApp.openIssueCashModal = () => {
        const issueCashModal = $uibModal.open({
            templateUrl: 'issueCashModal.html',
            controller: 'IssueCashModalCtrl',
            controllerAs: 'issueCashModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                supportedCurrencies: () => supportedCurrencies
            }
        });

        issueCashModal.result.then(() => {}, () => {});
    };

    /** Displays the netting trades modal. */
    demoApp.openNetTradesModal = () => {
        const netTradesModal = $uibModal.open({
            templateUrl: 'netTradesModal.html',
            controller: 'netTradesModalCtrl',
            controllerAs: 'netTradesModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                supportedCurrencies: () => supportedCurrencies
            }
        });

        netTradesModal.result.then(() => {}, () => {});
    };

    /** Displays the IOU transfer modal. */
    demoApp.openTransferModal = (id) => {
        const transferModal = $uibModal.open({
            templateUrl: 'transferModal.html',
            controller: 'TransferModalCtrl',
            controllerAs: 'transferModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                id: () => id
            }
        });

        transferModal.result.then(() => {}, () => {});
    };

    /** Displays the IOU settlement modal. */
    demoApp.openSettleModal = (id) => {
        const settleModal = $uibModal.open({
            templateUrl: 'settleModal.html',
            controller: 'SettleModalCtrl',
            controllerAs: 'settleModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                id: () => id
            }
        });

        settleModal.result.then(() => {}, () => {});
    };

    /** Refreshes the front-end. */
    demoApp.refresh = () => {
        // Update the list of trades.
        $http.get(apiBaseURL + "ious").then((response) => demoApp.ious =
            Object.keys(response.data).map((key) => response.data[key].state.data));

        // Update the cash balances.
        $http.get(apiBaseURL + "cash-balances").then((response) => demoApp.cashBalances =
            response.data);

        // Update the completed trades.
        $http.get(apiBaseURL + "settled-trades").then((response) => demoApp.settled_trades =
                    Object.keys(response.data).map((key) => response.data[key].state.data));

        $http.get(apiBaseURL + "net-cash-balances").then((response) => demoApp.net_cash_trades =
                            response.data);
    }

    demoApp.refresh();
});

// Causes the webapp to ignore unhandled modal dismissals.
angular.module('demoAppModule').config(['$qProvider', function($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);