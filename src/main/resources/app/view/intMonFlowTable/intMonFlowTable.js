// js for sample app table view
(function() {
    'use strict';

    // injected refs
    var $log, $scope, $interval, $timeout, fs, wss, ks, ls, cbs, $location;

    // constants
    var detailsReq = 'intMonFlowTableDetailsRequest';
    var detailsResp = 'intMonFlowTableDetailsResponse';

    var watchHopLatencyReq = 'intMonWatchHopLatencyRequest';

    var hopLatencyDataReq = 'intMonHopLatencyDataRequest';
    var hopLatencyDataResp = 'intMonHopLatencyDataResponse';
    // pName = 'ov-int-mon-flow-table-item-details-panel',

    // propOrder = ['id', 'label', 'code'],
    // friendlyProps = ['Item ID', 'Item Label', 'Special Code'];
    var refreshInterval = 1000;

    var chartRefreshInterval = 100; // miliseconds
    var monPeriod = 10; // seconds
    var numDataPoints = monPeriod*1000/chartRefreshInterval + 1; // for both head and tail
    var numLabels = monPeriod + 1;
    var labelDis = (numDataPoints-1)/(numLabels-1);

    var labels = new Array(1);
    var i = 0;

    // see: http://stackoverflow.com/questions/38239877/unable-to-parse-color-in-line-chart-angular-chart-js
    var data = new Array(1);

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
                    angular.forEach(o.scope.tableData, function(item) {
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
        o.scope.$on('$destroy', function() {
            wss.unbindHandlers(handlers);
            stopRefresh();
            ls.stop();
        });

        sortCb(o.scope.sortParams);
        startRefresh();
    }

    // function monFlowbuildChart(o) {
    //     var handlers = {},
    //         root = o.tag + 's',
    //         req = o.tag + 'DataRequest',
    //         resp = o.tag + 'DataResponse',
    //         onResp = fs.isF(o.respCb),
    //         oldChartData = [],
    //         refreshPromise;

    //     o.scope.chartData = [];
    //     o.scope.changedDataF = [];
    //     o.scope.reqParamsF = o.reqParamsF || {};
    //     o.scope.autoRefresh = true;
    //     o.scope.autoRefreshTip = 'Toggle auto refresh';

    //     // === websocket functions ===
    //     // response
    //     function respCb(data) {
    //         ls.stop();
    //         o.scope.chartData = data[root];
    //         // o.scope.annots = data.annots;
    //         onResp && onResp();

    //         // check if data changed
    //         if (!angular.equals(o.scope.chartData, oldChartData)) {
    //             o.scope.changedDataF = [];
    //             // only refresh the chart if there are new changes
    //             if (oldChartData.length) {
    //                 angular.forEach(o.scope.chartData, function (item) {
    //                     if (!fs.containsObj(oldChartData, item)) {
    //                         o.scope.changedDataF.push(item);
    //                     }
    //                 });
    //             }
    //             angular.copy(o.scope.chartData, oldChartData);
    //         }
    //         o.scope.$apply();
    //     }
    //     handlers[resp] = respCb;
    //     wss.bindHandlers(handlers);

    //     // request
    //     function requestCb(params) {
    //         var p = angular.extend({}, params, o.query);
    //         if (wss.isConnected()) {
    //             wss.sendEvent(req, p);
    //             ls.start();
    //         }
    //     }
    //     o.scope.requestCallback = requestCb;

    //     // === autoRefresh functions ===
    //     function fetchDataIfNotWaiting() {
    //         if (!ls.waiting()) {
    //             if (fs.debugOn('widget')) {
    //                 $log.debug('Refreshing ' + root + ' page');
    //             }
    //             requestCb(o.scope.reqParamsF);
    //         }
    //     }

    //     function startRefresh() {
    //         refreshPromise = $interval(fetchDataIfNotWaiting, chartRefreshInterval);
    //     }

    //     function stopRefresh() {
    //         if (refreshPromise) {
    //             $interval.cancel(refreshPromise);
    //             refreshPromise = null;
    //         }
    //     }

    //     // function toggleRefresh() {
    //     //     o.scope.autoRefresh = !o.scope.autoRefresh;
    //     //     o.scope.autoRefresh ? startRefresh() : stopRefresh();
    //     // }
    //     // o.scope.toggleRefresh = toggleRefresh;

    //     // === Cleanup on destroyed scope ===
    //     o.scope.$on('$destroy', function () {
    //         wss.unbindHandlers(handlers);
    //         stopRefresh();
    //         ls.stop();
    //     });

    //     requestCb(o.scope.reqParamsF);
    //     startRefresh();
    // }

    function watchHopLatency() {
        if (($scope.selId) && ($scope.swId !== "")) {
            wss.sendEvent(watchHopLatencyReq, { "flowId": $scope.selId, "swId": $scope.swId });
        }
    }

    // function reqHopLatencyData() {
    //     wss.sendEvent(hopLatencyDataReq);
    // }

    function respHopLatencyDataCb(latestHopLatency) {
        var i = 0;
        for ( i = 0; i < numDataPoints - 1; i++) {
            data[0][i] = data[0][i+1];
        }
        data[0][numDataPoints-1] = latestHopLatency.latency;
        $scope.data = data;
    }

    var app = angular.module('ovIntMonFlowTable', ['chart.js']);
    app.controller('OvIntMonFlowTableCtrl', ['$log', '$scope', '$interval', '$timeout', 'TableBuilderService',
        'FnService', 'WebSocketService', 'KeyService', 'LoadingService', 'ChartBuilderService', '$location',

        function(_$log_, _$scope_, _$interval_, _$timeout_, tbs, _fs_, _wss_, _ks_, _ls_, _cbs_, _location_) {
            $log = _$log_;
            $scope = _$scope_;
            $interval = _$interval_;
            $timeout = _$timeout_;
            fs = _fs_;
            wss = _wss_;
            ks = _ks_;
            ls = _ls_;
            cbs = _cbs_;
            $location = _location_;

//--------------------Angular merge problem of version 1.2---------------------
            if (!angular.merge) {
                angular.merge = (function mergePollyfill() {
                    function setHashKey(obj, h) {
                        if (h) {
                            obj.$$hashKey = h;
                        } else {
                            delete obj.$$hashKey;
                        }
                    }

                    function baseExtend(dst, objs, deep) {
                        var h = dst.$$hashKey;

                        for (var i = 0, ii = objs.length; i < ii; ++i) {
                            var obj = objs[i];
                            if (!angular.isObject(obj) && !angular.isFunction(obj)) continue;
                            var keys = Object.keys(obj);
                            for (var j = 0, jj = keys.length; j < jj; j++) {
                                var key = keys[j];
                                var src = obj[key];

                                if (deep && angular.isObject(src)) {
                                    if (angular.isDate(src)) {
                                        dst[key] = new Date(src.valueOf());
                                    } else {
                                        if (!angular.isObject(dst[key])) dst[key] = angular.isArray(src) ? [] : {};
                                        baseExtend(dst[key], [src], true);
                                    }
                                } else {
                                    dst[key] = src;
                                }
                            }
                        }

                        setHashKey(dst, h);
                        return dst;
                    }
                });
            }

//-----------------------------------------------------------------------------

            var handlers = {};
            handlers[hopLatencyDataResp] = respHopLatencyDataCb;
            wss.bindHandlers(handlers);

            // init values
            labels = new Array(numDataPoints);      
            for (i = 0; i < 1; i++) {
                data[i] = new Array(numDataPoints);
            }

            for (i = 0; i < numDataPoints; i++) {
                data[0][i] = 0;
            }
            for (i = 0; i < numLabels; i++) {
                labels[i*labelDis] = i+1-numLabels;
            }

            $scope.series = ['Hop Latency'];
            $scope.labels = labels;
            $scope.data = data;

            // $scope.onClick = function(points, evt) {
            //     console.log(points, evt);
            // };
            $scope.datasetOverride = [{ yAxisID: 'y-axis-1' }];
            $scope.options = {
                scales: {
                    yAxes: [{
                        id: 'y-axis-1',
                        type: 'linear',
                        display: true,
                        position: 'right'
                    }
                    // , {
                    //     id: 'y-axis-2',
                    //     type: 'linear',
                    //     display: true,
                    //     position: 'right'
                    // }
                    ]
                },
                responsive: false,
                maintainAspectRatio: false,
                animation : false,
                elements: {
                    line: {
                        tension: 0
                    }
                },
                // showLines: false
            };
            // async data update
            var chartRefresh = $interval(function () {
                // reqHopLatencyData();
                wss.sendEvent(hopLatencyDataReq);
                // data[0][1] = Math.random() * 10;
            }, chartRefreshInterval);

            $scope.watchHopLatency = watchHopLatency;

            // var handlers = {};
            // $scope.panelDetails = {};

            // // details response handler
            // handlers[detailsResp] = respDetailsCb;
            // wss.bindHandlers(handlers);

            // custom selection callback
            // request sw list to display in dropdown view
            // function selCb($event, row) {
            //     if ($scope.selId) {
            //         wss.sendEvent(swListReq, { "id": row.id });
            //     } else {
            //         // $scope.hidePanel();
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


            // cbs.buildChart({
            //     scope: $scope,
            //     // query: params,
            //     tag: 'intMonHopLatency'
            // });
            // monFlowbuildChart({
            //     scope: $scope,
            //     // query: params,
            //     tag: 'intMonHopLatency'
            // });

            // $scope.$watch('chartData', function () {
            //     if (!fs.isEmptyObject($scope.chartData)) {
            //         $scope.showLoader = false;
            //         var length = $scope.chartData.length;
            //         labels = new Array(length);
            //         data[0] = new Array(length);

            //         $scope.chartData.forEach(function (cm, idx) {
            //             data[0][idx] = cm.latency;
            //             labels[idx] = cm.label;
            //         });
            //     }

            //     // max = maxInArray(data);
            //     $scope.labels = labels;
            //     $scope.data = data;
            //     $scope.options = {
            //         scaleOverride : true,
            //         scaleSteps : 10,
            //         scaleStartValue : 0,
            //         scaleFontSize : 16,
            //         // for scatering, also linear scale xaxes
            //         scales: {
            //             yAxes: [{
            //                 id: 'y-axis-1',
            //                 type: 'linear',
            //                 display: true,
            //                 position: 'left'
            //             }]
            //         },
            //         fill: false,
            //         animation : false,
            //         showLines: false,
            //         responsive: false,
            //         // radius: 5,
            //         // pointBackgroundColor: 'rgba(255,0,0,0.3)',
            //         maintainAspectRatio: false
            //     };
            //     // $scope.onClick = function (points, evt) {
            //     //     var label = labels[points[0]._index];
            //     //     if (label) {
            //     //         ns.navTo('cpman', { devId: label });
            //     //         $log.log(label);
            //     //     }
            //     // };

            //     // if (!fs.isEmptyObject($scope.annots)) {
            //     //     $scope.deviceIds = JSON.parse($scope.annots.deviceIds);
            //     // }

            //     // $scope.onChange = function (deviceId) {
            //     //     ns.navTo('cpman', { devId: deviceId });
            //     // };
            // });

            // $scope.series = ['Hop Latency'];
            // $scope.labels = labels;
            // $scope.data = data;
            // $scope.showLoader = true;


            // cleanup
            $scope.$on('$destroy', function() {
                $interval.cancel(chartRefresh);
                wss.unbindHandlers(handlers);
                $log.log('OvIntMonFlowTableCtrl has been destroyed');
            });

            $log.log('OvIntMonFlowTableCtrl has been created');
        }
    ]);

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
