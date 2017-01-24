// js for sample app custom view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, wss, ks;

    // constants
    var dataReq = 'intMonMainDataRequest';
    var dataResp = 'intMonMainDataResponse';
    var dataFlowFilterStringReq = 'intMonFlowFilterStringRequest';

    /*function addKeyBindings() {
        var map = {
            space: [getData, 'Fetch data from server'],

            _helpFormat: [
                ['space']
            ]
        };

        ks.keyBindings(map);
    }*/

    function getData() {
        wss.sendEvent(dataReq);
    }

    function sendFlowFilterString() {
        var filterObjectNode = { "ip4SrcPrefix": $scope.ip4SrcPrefix, 
            "ip4DstPrefix": $scope.ip4DstPrefix,
            "ip4SrcPort": $scope.ip4SrcPort,
            "ip4DstPort": $scope.ip4DstPort
        };
        wss.sendEvent(dataFlowFilterStringReq, filterObjectNode);
        $scope.data.cube = 100000;
        $scope.$apply();
    }

    function respDataCb(data) {
        $scope.data = data;
        $scope.$apply();
    }


    angular.module('ovIntMonMain', [])
        .controller('OvIntMonMainCtrl',
        ['$log', '$scope', 'WebSocketService', 'KeyService',

        function (_$log_, _$scope_, _wss_, _ks_) {
            $log = _$log_;
            $scope = _$scope_;
            wss = _wss_;
            ks = _ks_;

            var handlers = {};
            $scope.data = {};

            // data response handler
            handlers[dataResp] = respDataCb;
            wss.bindHandlers(handlers);

            // addKeyBindings();

            // custom click handler
            $scope.getData = getData;
            $scope.sendFlowFilterString = sendFlowFilterString;

            // get data the first time...
            getData();

            // cleanup
            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
                /*ks.unbindKeys();*/
                $log.log('OvIntMonMainCtrl has been destroyed');
            });

            $log.log('OvIntMonMainCtrl has been created');
        }]);

}());
