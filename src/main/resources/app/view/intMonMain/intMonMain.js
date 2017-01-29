// // js for sample app custom view
// (function () {
//     'use strict';

//     // injected refs
//     var $log, $scope, wss, ks;

//     // constants
//     var dataReq = 'intMonMainDataRequest';
//     var dataResp = 'intMonMainDataResponse';
//     var dataFlowFilterStringReq = 'intMonFlowFilterStringRequest';

//     /*function addKeyBindings() {
//         var map = {
//             space: [getData, 'Fetch data from server'],

//             _helpFormat: [
//                 ['space']
//             ]
//         };

//         ks.keyBindings(map);
//     }*/

//     function getData() {
//         wss.sendEvent(dataReq);
//     }

//     function sendFlowFilterString() {
//         var insMask0007 = 0;

//         if ($scope.switchId) insMask0007 |= 1 << 7;
//         if ($scope.ingressPortId) insMask0007 |= 1 << 6;
//         if ($scope.hopLatency) insMask0007 |= 1 << 5;
//         if ($scope.qOccupancy) insMask0007 |= 1 << 4;
//         if ($scope.ingressTstamp) insMask0007 |= 1 << 3;
//         if ($scope.egressPortId) insMask0007 |= 1 << 2;
//         if ($scope.qCongestion) insMask0007 |= 1 << 1;
//         if ($scope.egressPortTxUtilization) insMask0007 |= 1;

//         // if ( $scope.ip4SrcPrefix == "") 
//         var filterObjectNode = { "ip4SrcPrefix": $scope.ip4SrcPrefix, 
//             "ip4DstPrefix": $scope.ip4DstPrefix,
//             "ip4SrcPort": $scope.ip4SrcPort,
//             "ip4DstPort": $scope.ip4DstPort,
//             "priority": $scope.priority,
//             "insMask0007" : insMask0007
//         };
//         wss.sendEvent(dataFlowFilterStringReq, filterObjectNode);
//         $scope.data.cube = 100000;
//         $scope.$apply();
//     }

//     function respDataCb(data) {
//         $scope.data = data;
//         $scope.$apply();
//     }


//     angular.module('ovIntMonMain', [])
//         .controller('OvIntMonMainCtrl',
//         ['$log', '$scope', 'WebSocketService', 'KeyService',

//         function (_$log_, _$scope_, _wss_, _ks_) {
//             $log = _$log_;
//             $scope = _$scope_;
//             wss = _wss_;
//             ks = _ks_;

//             var handlers = {};
//             $scope.data = {};

//             // data response handler
//             handlers[dataResp] = respDataCb;
//             wss.bindHandlers(handlers);

//             // addKeyBindings();

//             // custom click handler
//             $scope.getData = getData;
//             $scope.sendFlowFilterString = sendFlowFilterString;

//             // get data the first time...
//             getData();

//             // cleanup
//             $scope.$on('$destroy', function () {
//                 wss.unbindHandlers(handlers);
//                 /*ks.unbindKeys();*/
//                 $log.log('OvIntMonMainCtrl has been destroyed');
//             });

//             $log.log('OvIntMonMainCtrl has been created');
//         }]);

// }());


(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, wss, ks;

    // constants
    // var dataReq = 'intMonMainDataRequest';
    // var dataResp = 'intMonMainDataResponse';
    var dataFlowFilterStringReq = 'intMonFlowFilterStringRequest';
    var delFlowsFilterReq = 'intMonDelFlowsFilterRequest';
    //-------------------------------------------------------------------------
    var detailsReq = 'intMonMainDetailsRequest',
        detailsResp = 'intMonMainDetailsResponse',
        pName = 'ov-int-mon-main-item-details-panel',
      


        propOrder = ['id', 'srcAddr', 'dstAddr', 'srcPort', 'dstPort', 'insMask', 'priority'],
        friendlyProps = ['FlowsFilter ID', 'Src Address', 'Dst Address', 'Src Port', 'Dst Port', 'Ins Mask', 'Priority'];
    //-------------------------------------------------------------------------
    /*function addKeyBindings() {
        var map = {
            space: [getData, 'Fetch data from server'],

            _helpFormat: [
                ['space']
            ]
        };

        ks.keyBindings(map);
    }*/

    // function getData() {
    //     wss.sendEvent(dataReq);
    // }

    function sendFlowFilterString() {
        var insMask0007 = 0;

        if ($scope.switchId) insMask0007 |= 1 << 7;
        if ($scope.ingressPortId) insMask0007 |= 1 << 6;
        if ($scope.hopLatency) insMask0007 |= 1 << 5;
        if ($scope.qOccupancy) insMask0007 |= 1 << 4;
        if ($scope.ingressTstamp) insMask0007 |= 1 << 3;
        if ($scope.egressPortId) insMask0007 |= 1 << 2;
        if ($scope.qCongestion) insMask0007 |= 1 << 1;
        if ($scope.egressPortTxUtilization) insMask0007 |= 1;

        // if ( $scope.ip4SrcPrefix == "") 
        var filterObjectNode = { "ip4SrcPrefix": $scope.ip4SrcPrefix, 
            "ip4DstPrefix": $scope.ip4DstPrefix,
            "ip4SrcPort": $scope.ip4SrcPort,
            "ip4DstPort": $scope.ip4DstPort,
            "insMask0007" : insMask0007,
            "priority": $scope.priority
        };
        wss.sendEvent(dataFlowFilterStringReq, filterObjectNode);
    }

    function delFlowsFilter() {
        if ($scope.selId) {
            wss.sendEvent(delFlowsFilterReq, { "id": $scope.selId });
        }
    }

    // function respDataCb(data) {
    //     $scope.data = data;
    //     $scope.$apply();
    // }
    //-------------------------------------------------------------------------
    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populatePanel(panel) {
        var title = panel.append('h3'),
            tbody = panel.append('table').append('tbody');

        title.text('Item Details');

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, $scope.panelDetails[prop]);
        });

        panel.append('hr');
        panel.append('h4').text('Comments');
        panel.append('p').text($scope.panelDetails.comment);
    }

    function respDetailsCb(data) {
        $scope.panelDetails = data.details;
        $scope.$apply();
    }
    //-------------------------------------------------------------------------
    angular.module('ovIntMonMain', [])
        .controller('OvIntMonMainCtrl',
        ['$log', '$scope', 'TableBuilderService',
            'FnService', 'WebSocketService', 'KeyService',

            function (_$log_, _$scope_, tbs, _fs_, _wss_, _ks_) {
                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                wss = _wss_;
                ks = _ks_;

                var handlers = {};
                // $scope.data = {};
                //-------------------------------------------------------------
                $scope.panelDetails = {};
                 handlers[detailsResp] = respDetailsCb;
                //-------------------------------------------------------------
                // data response handler
                // handlers[dataResp] = respDataCb;
                wss.bindHandlers(handlers);
                // debugger;
                //-----------------------------------------------------------------
                // custom selection callback
                    function selCb($event, row) {
                        if ($scope.selId) {
                            wss.sendEvent(detailsReq, { id: row.id });
                        } else {
                            $scope.hidePanel();
                        }
                        $log.debug('Got a click on:', row);
                    }

                    // TableBuilderService creating a table for us
                    tbs.buildTable({
                        scope: $scope,
                        tag: 'intMonMain',
                        selCb: selCb
                    });
                //-----------------------------------------------------------------
                // addKeyBindings();

                // custom click handler
                // $scope.getData = getData;
                $scope.sendFlowFilterString = sendFlowFilterString;
                $scope.delFlowsFilter = delFlowsFilter;

                // get data the first time...
                // getData();

                // cleanup
                $scope.$on('$destroy', function () {
                    wss.unbindHandlers(handlers);
                    /*ks.unbindKeys();*/
                    $log.log('OvIntMonMainCtrl has been destroyed');
                });

                $log.log('OvIntMonMainCtrl has been created');
            }])
        //---------------------------------------------------------------------
        .directive('ovIntMonMainItemDetailsPanel', ['PanelService', 'KeyService',
            function (ps, ks) {
            return {
                restrict: 'E',
                link: function (scope, element, attrs) {
                    // insert details panel with PanelService
                    // create the panel
                    var panel = ps.createPanel(pName, {
                        width: 200,
                        margin: 20,
                        hideMargin: 0
                    });
                    panel.hide();
                    scope.hidePanel = function () { panel.hide(); };

                    function closePanel() {
                        if (panel.isVisible()) {
                            $scope.selId = null;
                            panel.hide();
                            return true;
                        }
                        return false;
                    }

                    // create key bindings to handle panel
                    ks.keyBindings({
                        esc: [closePanel, 'Close the details panel'],
                        _helpFormat: ['esc']
                    });
                    ks.gestureNotes([
                        ['click', 'Select a row to show item details']
                    ]);

                    // update the panel's contents when the data is changed
                    scope.$watch('panelDetails', function () {
                        if (!fs.isEmptyObject(scope.panelDetails)) {
                            panel.empty();
                            populatePanel(panel);
                            panel.show();
                        }
                    });

                    // cleanup on destroyed scope
                    scope.$on('$destroy', function () {
                        ks.unbindKeys();
                        ps.destroyPanel(pName);
                    });
                }
            };
        }]);
        //---------------------------------------------------------------------

}());