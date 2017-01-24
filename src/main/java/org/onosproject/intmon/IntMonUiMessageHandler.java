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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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

    private static final String SAMPLE_CUSTOM_DATA_REQ = "intMonMainDataRequest";
    private static final String DATA_FLOW_FILTER_STRING_REQ = "intMonFlowFilterStringRequest";
    private static final String SAMPLE_CUSTOM_DATA_RESP = "intMonMainDataResponse";

    private static final String NUMBER = "number";
    private static final String SQUARE = "square";
    private static final String CUBE = "cube";
    private static final String MESSAGE = "message";
    private static final String MSG_FORMAT = "Next incrememt is %d units";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private long someNumber = 1;
    private long someIncrement = 1;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntMonService intMonService;
//    private IntMonService intMonService;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new IntMonMainDataRequestHandler(),
                new intMonFlowFilterStringRequestHandler()
        );
    }

    // handler for sample data requests
    private final class IntMonMainDataRequestHandler extends RequestHandler {

        private IntMonMainDataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            someIncrement++;
            someNumber += someIncrement;
            log.debug("Computing data for {}...", someNumber);

            ObjectNode result = objectNode();
            result.put(NUMBER, someNumber);
            result.put(SQUARE, someNumber * someNumber);
            result.put(CUBE, someNumber * someNumber * someNumber);
            result.put(MESSAGE, String.format(MSG_FORMAT, someIncrement + 1));
            sendMessage(SAMPLE_CUSTOM_DATA_RESP, 0, result);
        }
    }

    private final class intMonFlowFilterStringRequestHandler extends RequestHandler {

        private intMonFlowFilterStringRequestHandler() {
            super(DATA_FLOW_FILTER_STRING_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            log.info("intMonFlowFilterStringRequest" + payload.toString());

            intMonService = get(IntMonService.class);

            Ip4Prefix ip4SrcPrefix = null;
            Ip4Prefix ip4DstPrefix = null;
            Integer ip4SrcPort = null;
            Integer ip4DstPort = null;

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

            intMonService.setMonFlows(new FlowsFilter(
                    ip4SrcPrefix, ip4DstPrefix,
                    ip4SrcPort, ip4DstPort)
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