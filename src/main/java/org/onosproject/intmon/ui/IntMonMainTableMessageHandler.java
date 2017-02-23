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
import org.apache.commons.lang3.tuple.Triple;
import org.onosproject.bmv2.api.runtime.Bmv2FiveTupleFlow;
import org.onosproject.bmv2.api.runtime.Bmv2FlowsFilter;
import org.onosproject.bmv2.api.runtime.Bmv2IntUdp;
import org.onosproject.bmv2.api.service.Bmv2IntMonService;
import org.onosproject.intmon.IntMonService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Override;
import java.util.Collection;
import java.util.Map;

/**
 * Skeletal ONOS UI Table-View message handler.
 */
public class IntMonMainTableMessageHandler extends UiMessageHandler {
    // the name need to follow the convention, if not there will be error
    private static final String INT_MON_MAIN_ADD_FLOW_FILTERS = "intMonMainAddFlowFilters";
    private static final String INT_MON_MAIN_ADD_FLOW_FILTER_DATA_REQUEST = "intMonMainAddFlowFilterDataRequest";
    private static final String INT_MON_MAIN_ADD_FLOW_FILTER_DATA_RESPONSE = "intMonMainAddFlowFilterDataResponse";

//    private static final String INT_MON_MAINS = "intMonMains";
//    private static final String INT_MON_MAIN_DATA_REQUEST = "intMonMainDataRequest";
//    private static final String INT_MON_MAIN_DATA_RESPONSE = "intMonMainDataResponse";

    private static final String INT_MON_DEL_FLOWS_FILTER_REQ = "intMonDelFlowsFilterRequest";

    private static final String INT_MON_MAIN_MON_DATA_REQ = "intMonMainMonDataRequest";
    private static final String INT_MON_MAIN_MON_DATA_RESP = "intMonMainMonDataResponse";

//    private static final String SAMPLE_TABLE_DETAIL_REQ = "intMonMainDetailsRequest";
//    private static final String SAMPLE_TABLE_DETAIL_RESP = "intMonMainDetailsResponse";
//    private static final String DETAILS = "details";

    private static final String NO_ROWS_MESSAGE = "No items found";
    private static final String NO_ROWS_MESSAGE_F = "No monitoring flow found";

    private static final String ID = "id";
    private static final String SRC_ADDR = "srcAddr";
    private static final String DST_ADDR = "dstAddr";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String INS_MASK = "insMask";
    private static final String PRIORITY = "priority";

    private static final String ID_F = "idF";
    private static final String SRC_ADDR_F = "srcAddrF";
    private static final String DST_ADDR_F = "dstAddrF";
    private static final String MON_DATA_F = "monDataF";

//    private static final String COMMENT = "comment";
//    private static final String RESULT = "result";

    private static final String[] COLUMN_IDS = { ID, SRC_ADDR, DST_ADDR, SRC_PORT, DST_PORT, INS_MASK, PRIORITY };
    private static final String[] COLUMN_IDS_F = { ID_F, SRC_ADDR_F, DST_ADDR_F, MON_DATA_F };

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Bmv2IntMonService bmv2IntMonService;
    protected IntMonService intMonService;
//    protected IntMonService intMonService = get(IntMonService.class);

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new intMonMainFlowFilterRequestHandler(),
//                new intMonMainDetailRequestHandler(),
                new intMonMainMonDataRequestHandler(),
                new intMonDelFlowsFilterRequestHandler()
        );
    }

    // handler for sample table requests
    private final class intMonMainFlowFilterRequestHandler extends TableRequestHandler {

        private intMonMainFlowFilterRequestHandler() {
            super(INT_MON_MAIN_ADD_FLOW_FILTER_DATA_REQUEST, INT_MON_MAIN_ADD_FLOW_FILTER_DATA_RESPONSE, INT_MON_MAIN_ADD_FLOW_FILTERS);
        }

        // if necessary, override defaultColumnId() -- if it isn't "id"

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

            Map<Bmv2FlowsFilter, Triple<Integer, Integer, Integer>> ffMap = getAllFlowsFilter();
            for (Bmv2FlowsFilter ff : ffMap.keySet()) {
                populateRow(tm.addRow(), ff, ffMap.get(ff));
            }
        }

        //        private void populateRow(TableModel.Row row, Item item) {
//            row.cell(ID, item.id())
//                    .cell(SRC_ADDR, item.srcAddr())
//                    .cell(DST_ADDR, item.dstAddr())
//                    .cell(SRC_PORT, item.srcPort())
//                    .cell(DST_PORT, item.dstPort())
//                    .cell(INS_MASK, item.insMask())
//                    .cell(PRIORITY, item.priority());
//        }

        private Map<Bmv2FlowsFilter, Triple<Integer, Integer, Integer>> getAllFlowsFilter() {
            // FIXME: is this efficient to get class every time the function is called?
            intMonService = get(IntMonService.class);
            return intMonService.getAllFlowsFilter();
        }

        private void populateRow(TableModel.Row row, Bmv2FlowsFilter ff,
                                 Triple<Integer, Integer, Integer> ffVal) {
            row.cell(ID, ffVal.getLeft())
                    .cell(SRC_ADDR, ff.ip4SrcPrefix)
                    .cell(DST_ADDR, ff.ip4DstPrefix)
                    .cell(SRC_PORT, ff.srcPort)
                    .cell(DST_PORT, ff.dstPort)
                    .cell(INS_MASK, ffVal.getMiddle())
                    .cell(PRIORITY, ffVal.getRight());
        }
    }

    private final class intMonMainMonDataRequestHandler extends IntTableRequestHandler {

        private intMonMainMonDataRequestHandler() {
            super(INT_MON_MAIN_MON_DATA_REQ, INT_MON_MAIN_MON_DATA_RESP, INT_MON_MAIN_ADD_FLOW_FILTERS);
        }

        // if necessary, override defaultColumnId() -- if it isn't "id"

        @Override
        protected String defaultColumnId() {
            return ID_F;
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS_F;
        }

        // if required, override createTableModel() to set column formatters / comparators

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE_F;
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

            Map<Bmv2FiveTupleFlow, Pair<Integer, Bmv2IntUdp>> intUDPMap = getRawMonData();
            for (Bmv2FiveTupleFlow ftf : intUDPMap.keySet()) {
                populateRowF(tm.addRow(), ftf, intUDPMap.get(ftf));
            }
        }

        private void populateRowF(TableModel.Row row, Bmv2FiveTupleFlow ftf,
                                  Pair<Integer, Bmv2IntUdp> pairIdIntUDP) {
            row.cell(ID_F, pairIdIntUDP.getLeft())
//                    .cell(SRC_ADDR_F, ftf.srcAddr.toString()+ ":" + ftf.srcPort.toString())
//                    .cell(DST_ADDR_F, ftf.dstAddr.toString()+ ":" + ftf.dstAddr.toString())
                    .cell(SRC_ADDR_F, ftf.srcAddr)
                    .cell(DST_ADDR_F, ftf.dstAddr)
//                    .cell(MON_DATA_F, pairIdIntUDP.getRight().getIntDataString());
                    .cell(MON_DATA_F, "none");
        }

        private Map<Bmv2FiveTupleFlow, Pair<Integer, Bmv2IntUdp>> getRawMonData() {
            bmv2IntMonService = get(Bmv2IntMonService.class);
            return bmv2IntMonService.getLatestRawMonData();
        }
    }


    private final class intMonDelFlowsFilterRequestHandler extends RequestHandler {

        private intMonDelFlowsFilterRequestHandler() {
            super(INT_MON_DEL_FLOWS_FILTER_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
//            String id = string(payload, ID, "(none)");

            // SomeService ss = get(SomeService.class);
            // Item item = ss.getItemDetails(id)

            intMonService = get(IntMonService.class);
            if (payload.get("id") != null) {
                intMonService.delFlowFilter(payload.get("id").asInt());
            }

        }
    }




    // handler for sample item details requests
//    private final class intMonMainDetailRequestHandler extends RequestHandler {
//
//        private intMonMainDetailRequestHandler() {
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
//                data.put(SRC_ADDR, item.srcAddr());
//                data.put(DST_ADDR, item.dstAddr());
//                data.put(SRC_PORT, item.srcPort());
//                data.put(DST_PORT, item.dstPort());
//                data.put(INS_MASK, item.insMask());
//                data.put(PRIORITY, item.priority());
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
//
////     Produce a list of items.
//    private static List<Item> getItems() {
//        List<Item> items = new ArrayList<>();
////        items.add(new Item("item-1", "foo", 42));
////        items.add(new Item("item-2", "bar", 99));
////        items.add(new Item("item-3", "baz", 65));
//        items.add(new Item("item-1", "1", "2", "3", "4", "5", "6"));
//        return items;
//    }
//
//
////     Simple model class to provide sample data
//    private static class Item {
//        private final String id;
//        private final String srcAddr;
//        private final String dstAddr;
//        private final String srcPort;
//        private final String dstPort;
//        private final String insMask;
//        private final String priority;
//
//
//        Item(String id, String srcAddr, String dstAddr, String srcPort, String dstPort, String insMask, String priority) {
//            this.id = id;
//            this.srcAddr = srcAddr;
//            this.dstAddr = dstAddr;
//            this.srcPort = srcPort;
//            this.dstPort = dstPort;
//            this.insMask = insMask;
//            this.priority = priority;
//        }
//
//        String id() { return id; }
//        String srcAddr() { return srcAddr; }
//        String dstAddr() { return dstAddr; }
//        String srcPort() { return srcPort; }
//        String dstPort() { return dstPort; }
//        String insMask() {return insMask;}
//        String priority() { return priority; }
//
//    }
}