"use strict";

angular.module('demoAppModule').controller('CreateIOUModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const createIOUModal = this;

    createIOUModal.peers = peers;
    createIOUModal.form = {};
    createIOUModal.formError = false;

    /** Validate and create an IOU. */
    createIOUModal.create = () => {
        if (invalidFormInput()) {
            createIOUModal.formError = true;
        } else {
            createIOUModal.formError = false;
            const valueDate = createIOUModal.form.valueDate;
            const counterparty = createIOUModal.form.counterparty;
            const tradedAmount = createIOUModal.form.tradedAmount;
            const tradedCurrency = createIOUModal.form.tradedCurrency;
            const counterAmount = createIOUModal.form.counterAmount;
            const counterCurrency = createIOUModal.form.counterCurrency;

            $uibModalInstance.close();

            // We define the IOU creation endpoint.
            const issueIOUEndpoint =
                apiBaseURL +
                `issue-iou?valueDate=${valueDate}&counterparty=${counterparty}&tradedAmount=${tradedAmount}&tradedCurrency=${tradedCurrency}&counterAmount=${counterAmount}&counterCurrency=${counterCurrency}`;

            // We hit the endpoint to create the IOU and handle success/failure responses.
            $http.put(issueIOUEndpoint).then(
                (result) => createIOUModal.displayMessage(result),
                (result) => createIOUModal.displayMessage(result)
            );
        }
    };

    /** Displays the success/failure response from attempting to create an IOU. */
    createIOUModal.displayMessage = (message) => {
        const createIOUMsgModal = $uibModal.open({
            templateUrl: 'createIOUMsgModal.html',
            controller: 'createIOUMsgModalCtrl',
            controllerAs: 'createIOUMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createIOUMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the IOU creation modal. */
    createIOUModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the IOU.
    function invalidFormInput() {
        // TODO put more validations here
       const valueDate = createIOUModal.form.valueDate;
                const counterparty = createIOUModal.form.counterparty;
                const tradedAmount = createIOUModal.form.tradedAmount;
                const tradedCurrency = createIOUModal.form.tradedCurrency;
                const counterAmount = createIOUModal.form.counterAmount;
                const counterCurrency = createIOUModal.form.counterCurrency;
        return isNaN(createIOUModal.form.tradedAmount) ||
                    (createIOUModal.form.counterparty === undefined) ||
                    isNaN(createIOUModal.form.counterAmount);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('createIOUMsgModalCtrl', function($uibModalInstance, message) {
    const createIOUMsgModal = this;
    createIOUMsgModal.message = message.data;
});