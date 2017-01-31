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
package org.onosproject.intmon;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Skeletal ONOS UI Custom-View message handler.
 */
public class IntMonUiMessageHandler extends UiMessageHandler {

//    private static final String SAMPLE_CUSTOM_DATA_REQ = "intMonMainDataRequest";
    private static final String INT_MON_FLOW_FILTER_ADD_REQUEST = "intMonFlowFilterAddRequest";
//    private static final String SAMPLE_CUSTOM_DATA_RESP = "intMonMainDataResponse";

//    private static final String NUMBER = "number";
//    private static final String SQUARE = "square";
//    private static final String CUBE = "cube";
//    private static final String MESSAGE = "message";
//    private static final String MSG_FORMAT = "Next incrememt is %d units";

    private final Logger log = LoggerFactory.getLogger(getClass());

//    private long someNumber = 1;
//    private long someIncrement = 1;

    protected IntMonService intMonService;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
//                new IntMonMainDataRequestHandler(),
                new intMonFlowFilterStringRequestHandler()
        );
    }

    // handler for sample data requests
//    private final class IntMonMainDataRequestHandler extends RequestHandler {
//
//        private IntMonMainDataRequestHandler() {
//            super(SAMPLE_CUSTOM_DATA_REQ);
//        }
//
//        @Override
//        public void process(long sid, ObjectNode payload) {
//            someIncrement++;
//            someNumber += someIncrement;
//            log.debug("Computing data for {}...", someNumber);
//
//            ObjectNode result = objectNode();
//            result.put(NUMBER, someNumber);
//            result.put(SQUARE, someNumber * someNumber);
//            result.put(CUBE, someNumber * someNumber * someNumber);
//            result.put(MESSAGE, String.format(MSG_FORMAT, someIncrement + 1));
//            sendMessage(SAMPLE_CUSTOM_DATA_RESP, 0, result);
//        }
//    }

    private final class intMonFlowFilterStringRequestHandler extends RequestHandler {

        private intMonFlowFilterStringRequestHandler() {
            super(INT_MON_FLOW_FILTER_ADD_REQUEST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            log.info("intMonFlowFilterStringRequest" + payload.toString());

            intMonService = get(IntMonService.class);

            // all should not be null (to prevent null pointer exception
            Ip4Prefix ip4SrcPrefix = Ip4Prefix.valueOf(0, 0); // 0.0.0.0/0 match all
            Ip4Prefix ip4DstPrefix = Ip4Prefix.valueOf(0, 0); // 0.0.0.0/0 match all
            Integer ip4SrcPort = -1; // assume -1 is for all
            Integer ip4DstPort = -1; // assume -1 is for all
            Integer insMask0007 = 0;
            Integer priority = 100;

            if (payload.get("ip4SrcPrefix") != null) {
//                ip4SrcPrefix = parseIp4Prefix((String)((Object)payload.get("ip4SrcPrefix")));
                ip4SrcPrefix = parseIp4Prefix(payload.get("ip4SrcPrefix").asText());
            }

            if (payload.get("ip4DstPrefix") != null) {
                ip4DstPrefix = parseIp4Prefix(payload.get("ip4DstPrefix").asText());
            }

            if(payload.get("ip4SrcPort") != null) {
                ip4SrcPort = payload.get("ip4SrcPort").asInt();
            }

            if(payload.get("ip4DstPort") != null) {
                ip4DstPort = payload.get("ip4DstPort").asInt();
            }

            if(payload.get("insMask0007") != null) {
                insMask0007 = payload.get("insMask0007").asInt();
            }

            if(payload.get("priority") != null) {
                priority = payload.get("priority").asInt();
            }

            intMonService.setFlowFilter(new FlowsFilter(
                    ip4SrcPrefix, ip4DstPrefix,
                    ip4SrcPort, ip4DstPort), insMask0007, priority
            );

        }

        Ip4Prefix parseIp4Prefix (String prefixString) {
            if (prefixString == null) return null;
            String[] splitString = prefixString.split("/");
            Ip4Address ip4Address = Ip4Address.valueOf(splitString[0]);
            int mask = Integer.parseInt(splitString[1]);
            return Ip4Prefix.valueOf(ip4Address, mask);
        }
    }
}