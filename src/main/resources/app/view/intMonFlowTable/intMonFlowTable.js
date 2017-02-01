// js for sample app table view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, $interval, $timeout, fs, wss, ks, ls;

    // constants
    var detailsReq = 'intMonFlowTableDetailsRequest';
    var   detailsResp = 'intMonFlowTableDetailsResponse';
        // pName = 'ov-int-mon-flow-table-item-details-panel',

        // propOrder = ['id', 'label', 'code'],
        // friendlyProps = ['Item ID', 'Item Label', 'Special Code'];
    var refreshInterval = 1000;

    // function addProp(tbody, index, value) {
    //     var tr = tbody.append('tr');

    //     function addCell(cls, txt) {
    //         tr.append('td').attr('class', cls).html(txt);
    //     }
    //     addCell('label', friendlyProps[index] + ' :');
    //     addCell('value', value);
    // }

    // function populatePanel(panel) {
    //     var title = panel.append('h3'),
    //         tbody = panel.append('table').append('tbody');

    //     title.text('Item Details');

    //     propOrder.forEach(function (prop, i) {
    //         addProp(tbody, i, $scope.panelDetails[prop]);
    //     });

    //     panel.append('hr');
    //     panel.append('h4').text('Comments');
    //     panel.append('p').text($scope.panelDetails.comment);
    // }

    // function respDetailsCb(data) {
    //     $scope.panelDetails = data.details;
    //     $scope.$apply();
    // }

    function monFlowBuildTable(o) {
        var handlers = {},
            root = o.tag + 's',
            req = o.tag + 'DataRequest',
            resp = o.tag + 'DataResponse',
            onSel = fs.isF(o.selCb),
            onResp = fs.isF(o.respCb),
            idKey = o.idKey || 'id',
            oldTableData = [],
            refreshPromise;

        o.scope.tableData = [];
        o.scope.changedData = [];
        o.scope.sortParams = o.sortParams || {};
        o.scope.autoRefresh = true;
        o.scope.autoRefreshTip = 'Toggle auto refresh';

        // === websocket functions --------------------
        // response
        function respCb(data) {
            ls.stop();
            o.scope.tableData = data[root];
            o.scope.annots = data.annots;
            onResp && onResp();

            // checks if data changed for row flashing
            if (!angular.equals(o.scope.tableData, oldTableData)) {
                o.scope.changedData = [];
                // only flash the row if the data already exists
                if (oldTableData.length) {
                    angular.forEach(o.scope.tableData, function (item) {
                        if (!fs.containsObj(oldTableData, item)) {
                            o.scope.changedData.push(item);
                        }
                    });
                }
                angular.copy(o.scope.tableData, oldTableData);
            }
            o.scope.$apply();
        }
        handlers[resp] = respCb;
        wss.bindHandlers(handlers);

        // request
        function sortCb(params) {
            var p = angular.extend({}, params, o.query);
            if (wss.isConnected()) {
                wss.sendEvent(req, p);
                ls.start();
            }
        }
        o.scope.sortCallback = sortCb;


        // === selecting a row functions ----------------
        function selCb($event, selRow) {
            var selId = selRow[idKey];
            o.scope.selId = (o.scope.selId === selId) ? null : selId;
            onSel && onSel($event, selRow);
        }
        o.scope.selectCallback = selCb;

        // === autoRefresh functions ------------------
        function fetchDataIfNotWaiting() {
            if (!ls.waiting()) {
                if (fs.debugOn('widget')) {
                    $log.debug('Refreshing ' + root + ' page');
                }
                sortCb(o.scope.sortParams);
            }
        }

        function startRefresh() {
            refreshPromise = $interval(fetchDataIfNotWaiting, refreshInterval);
        }

        function stopRefresh() {
            if (refreshPromise) {
                $interval.cancel(refreshPromise);
                refreshPromise = null;
            }
        }

        function toggleRefresh() {
            o.scope.autoRefresh = !o.scope.autoRefresh;
            o.scope.autoRefresh ? startRefresh() : stopRefresh();
        }
        o.scope.toggleRefresh = toggleRefresh;

        // === Cleanup on destroyed scope -----------------
        o.scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
            stopRefresh();
            ls.stop();
        });

        sortCb(o.scope.sortParams);
        startRefresh();
    }

    var app = angular.module('ovIntMonFlowTable', []);
    app.controller('OvIntMonFlowTableCtrl',
        ['$log', '$scope', '$interval', '$timeout', 'TableBuilderService',
            'FnService', 'WebSocketService', 'KeyService', 'LoadingService',

            function (_$log_, _$scope_, _$interval_, _$timeout_, tbs, _fs_, _wss_, _ks_, _ls_) {
                $log = _$log_;
                $scope = _$scope_;
                $interval = _$interval_;
                $timeout = _$timeout_;
                fs = _fs_;
                wss = _wss_;
                ks = _ks_;
                ls = _ls_;

                // var handlers = {};
                // $scope.panelDetails = {};

                // // details response handler
                // handlers[detailsResp] = respDetailsCb;
                // wss.bindHandlers(handlers);

                // custom selection callback
                // function selCb($event, row) {
                //     if ($scope.selId) {
                //         wss.sendEvent(detailsReq, { id: row.id });
                //     } else {
                //         $scope.hidePanel();
                //     }
                //     $log.debug('Got a click on:', row);
                // }

                // TableBuilderService creating a table for us
                // tbs.buildTable({
                monFlowBuildTable({
                    scope: $scope,
                    // selCb: selCb,
                    tag: 'intMonFlowTable'
                });

                // cleanup
                $scope.$on('$destroy', function () {
                    // wss.unbindHandlers(handlers);
                    $log.log('OvIntMonFlowTableCtrl has been destroyed');
                });

                $log.log('OvIntMonFlowTableCtrl has been created');
            }]);

        // .directive('ovIntMonFlowTableItemDetailsPanel', ['PanelService', 'KeyService',
        //     function (ps, ks) {
        //     return {
        //         restrict: 'E',
        //         link: function (scope, element, attrs) {
        //             // insert details panel with PanelService
        //             // create the panel
        //             var panel = ps.createPanel(pName, {
        //                 width: 200,
        //                 margin: 20,
        //                 hideMargin: 0
        //             });
        //             panel.hide();
        //             scope.hidePanel = function () { panel.hide(); };

        //             function closePanel() {
        //                 if (panel.isVisible()) {
        //                     $scope.selId = null;
        //                     panel.hide();
        //                     return true;
        //                 }
        //                 return false;
        //             }

        //             // create key bindings to handle panel
        //             ks.keyBindings({
        //                 esc: [closePanel, 'Close the details panel'],
        //                 _helpFormat: ['esc']
        //             });
        //             ks.gestureNotes([
        //                 ['click', 'Select a row to show item details']
        //             ]);

        //             // update the panel's contents when the data is changed
        //             scope.$watch('panelDetails', function () {
        //                 if (!fs.isEmptyObject(scope.panelDetails)) {
        //                     panel.empty();
        //                     populatePanel(panel);
        //                     panel.show();
        //                 }
        //             });

        //             // cleanup on destroyed scope
        //             scope.$on('$destroy', function () {
        //                 ks.unbindKeys();
        //                 ps.destroyPanel(pName);
        //             });
        //         }
        //     };
        // }]);
}());
