"use strict";

angular.module('demoAppModule').controller('netTradesModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const netTradesModal = this;

    netTradesModal.peers = peers;
    netTradesModal.form = {};
    netTradesModal.formError = false;

    /** Validate and create an IOU. */
    netTradesModal.create = () => {
        if (invalidFormInput()) {
            netTradesModal.formError = true;
        } else {
            netTradesModal.formError = false;

            const currency = netTradesModal.form.currency;
            const party = netTradesModal.form.counterparty;

            $uibModalInstance.close();

            // We define the IOU creation endpoint.
            const issueIOUEndpoint =
                apiBaseURL +
                `issue-iou?amount=0&currency=${currency}&party=${party}`;

            // We hit the endpoint to create the IOU and handle success/failure responses.
            $http.put(issueIOUEndpoint).then(
                (result) => netTradesModal.displayMessage(result),
                (result) => netTradesModal.displayMessage(result)
            );
        }
    };

    /** Displays the success/failure response from attempting to create an IOU. */
    netTradesModal.displayMessage = (message) => {
        const netTradesMsgModal = $uibModal.open({
            templateUrl: 'netTradesMsgModal.html',
            controller: 'netTradesMsgModalCtrl',
            controllerAs: 'netTradesMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        netTradesMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the IOU creation modal. */
    netTradesModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the IOU.
    function invalidFormInput() {
        return (netTradesModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('netTradesMsgModalCtrl', function($uibModalInstance, message) {
    const netTradesMsgModal = this;
    netTradesMsgModal.message = message.data;
});