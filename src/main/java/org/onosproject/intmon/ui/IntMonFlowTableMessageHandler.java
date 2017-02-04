/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.intmon.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableDuration;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.intmon.IntMonService;
import org.onosproject.intmon.lib.FiveTupleFlow;
import org.onosproject.intmon.lib.IntUDP;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.chart.ChartModel;
import org.onosproject.ui.chart.ChartRequestHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Override;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Skeletal ONOS UI Table-View message handler.
 */
public class IntMonFlowTableMessageHandler extends UiMessageHandler {

    private static final String INT_MON_FLOW_TABLE_DATA_REQUEST = "intMonFlowTableDataRequest";
    private static final String INT_MON_FLOW_TABLE_DATA_RESPONSE = "intMonFlowTableDataResponse";
    private static final String INT_MON_FLOW_TABLES = "intMonFlowTables";

    private static final String INT_MON_HOP_LATENCY_DATA_REQUEST = "intMonHopLatencyDataRequest";
    private static final String INT_MON_HOP_LATENCY_DATA_RESPONSE = "intMonHopLatencyDataResponse";
//    private static final String INT_MON_HOP_LATENCYS = "intMonHopLatencys";

    private static final String INT_MON_WATCH_HOP_LATENCY_REQUEST = "intMonWatchHopLatencyRequest";
//    private static final String INT_MON_WATCH_HOP_LATENCY_RESPONSE = "intMonWatchHopLatencyResponse";

//    private static final String SAMPLE_TABLE_DETAIL_REQ = "intMonFlowTableDetailsRequest";
//    private static final String SAMPLE_TABLE_DETAIL_RESP = "intMonFlowTableDetailsResponse";
//    private static final String DETAILS = "details";

    private static final String NO_ROWS_MESSAGE = "No items found";

//    private static final String[] COLUMN_IDS = { ID, LABEL, CODE };
//    private static final String ID = "id";
//    private static final String LABEL = "label";
//    private static final String CODE = "code";
//    private static final String COMMENT = "comment";
//    private static final String RESULT = "result";

    private static final String ID = "id";
    private static final String SRC_ADDR = "srcAddr";
    private static final String DST_ADDR = "dstAddr";
    private static final String MON_DATA = "monData";
    private static final String[] COLUMN_IDS = { ID, SRC_ADDR, DST_ADDR, MON_DATA };

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected IntMonService intMonService;

    private FiveTupleFlow watchedFft = null;
    private Integer watchedSwId = null;

//    private static final Integer NUM_WATCH_POINTS = 20;
//    private static final Integer TIME_STEP_MILI_SEC = 1000;

    // <time, latency>
//    private TimeValue[] wHopLatencyData = new TimeValue[NUM_WATCH_POINTS];




    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
//                new IntMonFlowTableDetailRequestHandler(),
                new IntMonWatchHopLatencyRequestHandler(),
//                new intMonHopLatencyRequestHandler(),
                new IntMonHopLatencyDataRequestHandler(),
            new IntMonFlowTableDataRequestHandler()
        );
    }

    // handler for sample table requests
    private final class IntMonFlowTableDataRequestHandler extends TableRequestHandler {

        private IntMonFlowTableDataRequestHandler() {
            super(INT_MON_FLOW_TABLE_DATA_REQUEST, INT_MON_FLOW_TABLE_DATA_RESPONSE, INT_MON_FLOW_TABLES);
        }

        // if necessary, override defaultColumnId() -- if it isn't "id"

//        @Override
//        protected String defaultColumnId() {
//            return ID_F;
//        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        // if required, override createTableModel() to set column formatters / comparators

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            // === NOTE: the table model supplied here will have been created
            // via  a call to createTableModel(). To assign non-default
            // cell formatters or comparators to the table model, override
            // createTableModel() and set them there.

            // === retrieve table row items from some service...
            // SomeService ss = get(SomeService.class);
            // List<Item> items = ss.getItems()

            // fake data for demonstration purposes...
//            List<Item> items = getItems();
//            for (Item item: items) {
//                populateRow(tm.addRow(), item);
//            }

            Map<FiveTupleFlow, Pair<Integer, IntUDP>> intUDPMap = getRawMonData();
            for (FiveTupleFlow ftf : intUDPMap.keySet()) {
                populateRowF(tm.addRow(), ftf, intUDPMap.get(ftf));
            }
        }

        private void populateRowF(TableModel.Row row, FiveTupleFlow ftf,
                                  Pair<Integer, IntUDP> pairIdIntUDP) {
            row.cell(ID, pairIdIntUDP.getLeft())
                    .cell(SRC_ADDR, ftf.srcAddr.toString()+ ":" + ftf.srcPort.toString())
                    .cell(DST_ADDR, ftf.dstAddr.toString()+ ":" + ftf.dstPort.toString())
//                    .cell(SRC_ADDR, ftf.srcAddr)
//                    .cell(DST_ADDR, ftf.dstAddr)
                    .cell(MON_DATA, pairIdIntUDP.getRight().getIntDataString());
//                    .cell(MON_DATA, "none");
        }

        private Map<FiveTupleFlow, Pair<Integer, IntUDP>> getRawMonData() {
            intMonService = get(IntMonService.class);
            return intMonService.getLatestRawMonData();
        }
    }

    //set the watch paramenter: FiveTupleFlow and the switch ID
    private final class IntMonWatchHopLatencyRequestHandler extends RequestHandler {

        private IntMonWatchHopLatencyRequestHandler() {
            super(INT_MON_WATCH_HOP_LATENCY_REQUEST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
//            log.info("intMonFlowFilterStringRequest" + payload.toString());

            intMonService = get(IntMonService.class);
            Integer monFlowId = payload.get("flowId").asInt();
            Integer monSwId = payload.get("swId").asInt();

            Map<Integer, FiveTupleFlow> idMonFlowMap = intMonService.getIdMonFlowMap();
            watchedFft = idMonFlowMap.get(monFlowId);
            watchedSwId = monSwId;
            // init wHopLatencyData
//            for (int i = 0; i < NUM_WATCH_POINTS; i ++) {
//                wHopLatencyData[i] = new TimeValue(0,0);
//            }

        }
    }

    private final class IntMonHopLatencyDataRequestHandler extends RequestHandler {

        private IntMonHopLatencyDataRequestHandler() {
            super(INT_MON_HOP_LATENCY_DATA_REQUEST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
//            log.info("intMonFlowFilterStringRequest" + payload.toString());

            if(watchedFft == null || watchedSwId == null) return;
//
            intMonService = get(IntMonService.class);
            Pair<Integer, IntUDP> idIntUDPMap = intMonService.getLatestRawMonData().get(watchedFft);
            if (idIntUDPMap == null) return;

            IntUDP intUDP = idIntUDPMap.getRight();
            int hLatency = intUDP.getHopLatencyOfDevId(watchedSwId);

            ObjectNode result = objectNode();
            result.put("time", intUDP.recvTime);
            result.put("latency", hLatency);
            sendMessage(INT_MON_HOP_LATENCY_DATA_RESPONSE, 0, result);

            // debug
//            log.info(result.toString());
        }
    }

//    private final class intMonHopLatencyRequestHandler extends ChartRequestHandler {
//
//        private intMonHopLatencyRequestHandler() {
//            super(INT_MON_HOP_LATENCY_DATA_REQUEST, INT_MON_HOP_LATENCY_DATA_RESPONSE, INT_MON_HOP_LATENCYS);
//        }
//
//        @Override
//        protected String[] getSeries() {
//            String[] series = new String[]{"latency",};
//            return series;
//        }
//
//        @Override
//        protected void populateChart(ChartModel cm, ObjectNode payload) {
////            Integer value = 100;
////            cm.addDataPoint("Jan").data("latency", "100").data("label", "Jan");
////            cm.addDataPoint("Feb").data("latency", "200").data("label", "Feb");
////            cm.addDataPoint("Mar").data("latency", "250").data("label", "Mar");
//
//            if(watchedFft == null || watchedSwId == null) return;
//
//            intMonService = get(IntMonService.class);
//            Pair<Integer, IntUDP> idIntUDPMap = intMonService.getLatestRawMonData().get(watchedFft);
//            if (idIntUDPMap == null) return;
//
//            IntUDP intUDP = idIntUDPMap.getRight();
//            long lastTime = wHopLatencyData[NUM_WATCH_POINTS-1].time;
//            long timeStepDis = (intUDP.recvTime - lastTime)/TIME_STEP_MILI_SEC;
//            long recvTime = intUDP.recvTime;
//            if (timeStepDis >= NUM_WATCH_POINTS) {
//                // reset data to 0
//                for (int i = 0; i < NUM_WATCH_POINTS; i++) {
//                    wHopLatencyData[i] = new TimeValue(recvTime - (NUM_WATCH_POINTS-i-1)*TIME_STEP_MILI_SEC, 0);
//                }
//            } else if (timeStepDis > 1) {
//                shiftLeftIntArr(wHopLatencyData, (int)timeStepDis);
//            } else {
//                shiftLeftIntArr(wHopLatencyData, 1);
//            }
//
//            int hLatency = intUDP.getHopLatencyOfDevId(watchedSwId);
//            wHopLatencyData[NUM_WATCH_POINTS-1] = new TimeValue(recvTime, hLatency);
//
////            LocalDateTime ldt = new LocalDateTime(recvTime);
//            fillChartModel(cm, wHopLatencyData, recvTime, NUM_WATCH_POINTS);
//
//        }
//
//        private TimeValue[] shiftLeftIntArr(TimeValue[] timeValueArr, int step) {
//            int lastPos = timeValueArr.length - step;
//            for(int i = 0; i < lastPos; i++) {
//                TimeValue tv = timeValueArr[i+step];
//                timeValueArr[i] = new TimeValue(tv.time, tv.value);
//            }
//            long lastTime = timeValueArr[timeValueArr.length - 1].time;
//            for(int i = lastPos; i < timeValueArr.length - 1; i ++) {
//                timeValueArr[i] = new TimeValue(lastTime + TIME_STEP_MILI_SEC*(i-lastPos), 0);
//            }
//            return timeValueArr;
//        }
//
//        private void fillChartModel(ChartModel cm, TimeValue[] data, long time, int numOfDp) {
//            for (int i = 0; i < numOfDp; i++) {
////                Duration timeDis = new Duration(i * TIME_STEP_MILI_SEC);
//                String pointTime = Long.toString(data[i].time - time) ;
//                cm.addDataPoint(pointTime).data("latency", data[i].value).data("label", pointTime);
//            }
//        }
//
//    }

//    private class TimeValue {
//        public long time;
//        public int value;
//
//        public TimeValue (long time, int value) {
//            this.time = time;
//            this.value = value;
//        }
//    }

    // handler for sample item details requests
//    private final class IntMonFlowTableDetailRequestHandler extends RequestHandler {
//
//        private IntMonFlowTableDetailRequestHandler() {
//            super(SAMPLE_TABLE_DETAIL_REQ);
//        }
//
//        @Override
//        public void process(long sid, ObjectNode payload) {
//            String id = string(payload, ID, "(none)");
//
//            // SomeService ss = get(SomeService.class);
//            // Item item = ss.getItemDetails(id)
//
//            // fake data for demonstration purposes...
//            Item item = getItem(id);
//
//            ObjectNode rootNode = objectNode();
//            ObjectNode data = objectNode();
//            rootNode.set(DETAILS, data);
//
//            if (item == null) {
//                rootNode.put(RESULT, "Item with id '" + id + "' not found");
//                log.warn("attempted to get item detail for id '{}'", id);
//
//            } else {
//                rootNode.put(RESULT, "Found item with id '" + id + "'");
//
//                data.put(ID, item.id());
//                data.put(LABEL, item.label());
//                data.put(CODE, item.code());
//                data.put(COMMENT, "Some arbitrary comment");
//            }
//
//            sendMessage(SAMPLE_TABLE_DETAIL_RESP, 0, rootNode);
//        }
//    }


    // ===================================================================
    // NOTE: The code below this line is to create fake data for this
    //       sample code. Normally you would use existing services to
    //       provide real data.

    // Lookup a single item.
//    private static Item getItem(String id) {
//        // We realize this code is really inefficient, but
//        // it suffices for our purposes of demonstration...
//        for (Item item : getItems()) {
//            if (item.id().equals(id)) {
//                return item;
//            }
//        }
//        return null;
//    }

    // Produce a list of items.
//    private static List<Item> getItems() {
//        List<Item> items = new ArrayList<>();
//        items.add(new Item("item-1", "foo", 42));
//        items.add(new Item("item-2", "bar", 99));
//        items.add(new Item("item-3", "baz", 65));
//        return items;
//    }

    // Simple model class to provide sample data
//    private static class Item {
//        private final String id;
//        private final String label;
//        private final int code;
//
//        Item(String id, String label, int code) {
//            this.id = id;
//            this.label = label;
//            this.code = code;
//        }
//
//        String id() { return id; }
//        String label() { return label; }
//        int code() { return code; }
//    }
}