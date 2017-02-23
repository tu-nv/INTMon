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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.UDP;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.runtime.Bmv2FiveTupleFlow;
import org.onosproject.bmv2.api.runtime.Bmv2FlowsFilter;
import org.onosproject.bmv2.api.runtime.Bmv2IntUdp;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.bmv2.api.service.Bmv2IntMonService;
import org.onosproject.bmv2.ctl.Bmv2DeviceThriftClient;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;

/**
 * Skeletal ONOS application component.
 * make this act as a service
 */
@Component(immediate = true)
@Service
public class IntMon implements IntMonService {
    // Instantiates the relevant services.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2DeviceContextService bmv2ContextService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2Controller bmv2Controller;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2IntMonService bmv2IntMonService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int UDP_DST_PORT_INT = 54321;
    // variables
    private ApplicationId appId;
    private PacketProcessor processor;
    // no newHashSet --> error Failed creating the component instance
    private Set<DeviceId> switches = Sets.newHashSet();

    private Topology topo;

    private final HostListener hostListener = new InternalHostListener();

    //    private static final String APP_NAME = "org.onosproject.intmon";
    //    private static final String MODEL_NAME = "IntMon";
    //    private static final String JSON_CONFIG_PATH = "~/onos-p4-dev/p4src/build/default.json";
    private static final String JSON_CONFIG_PATH = "/intmon.json";
    private static final Bmv2Configuration INTMON_CONFIGURATION = loadConfiguration();
    private static final IntMonInterpreter INTMON_INTERPRETER = new IntMonInterpreter();
    protected static final Bmv2DeviceContext INTMON_CONTEXT = new Bmv2DeviceContext(INTMON_CONFIGURATION, INTMON_INTERPRETER);
    private static final int FLOW_PRIORITY = 100;
    private Integer flowsFilterId = 1; // should not start with 0

    private Integer fiveTupleFlowId = 1;

    private Map<String, Integer> tableMap;
    Set<FlowRule> dRules = Sets.newHashSet();
    // each flowFilter has 1 mask (integer) and a set of flowRules for it
    private Map<Bmv2FlowsFilter, Set<FlowRule>> flowsFilterRulesMap = Maps.newHashMap();
    // <id, insMask0007, priority>
    private Map<Bmv2FlowsFilter, Triple<Integer, Integer, Integer>> flowsFilterInsMap = Maps.newHashMap();

    private Map<Integer, Bmv2FlowsFilter> idFlowsFilterMap = Maps.newHashMap();
    private Map<Bmv2FiveTupleFlow, Pair<Integer, Bmv2IntUdp>> lastestMonDataMap = Maps.newHashMap();

    private Map<Integer, Bmv2FiveTupleFlow> idMonFlowMap = Maps.newHashMap();


    @Activate
    protected void activate() {
        log.info("Started. PPAP");
        appId = coreService.getAppId("org.onosproject.intmon");
        processor = new SwitchPacketProcesser();
        packetService.addProcessor(processor, PacketProcessor.director(0));
//        packetService.addProcessor(processor, PacketProcessor.advisor(3));
        hostService.addListener(hostListener);
        bmv2ContextService.registerInterpreterClassLoader(INTMON_CONTEXT.interpreter().getClass(),
                                                          this.getClass().getClassLoader());

        // set flow filter id starting to 1
        flowsFilterId = 1;
        fiveTupleFlowId = 1;
        // deploy p4 program to devices
        deployDevices();

        // get all the device id: already got it from deployDevices()
//        topo = topologyService.currentTopology();
//        TopologyGraph graph = topologyService.getGraph(topo);
//        graph.getVertexes().stream()
//                .map(TopologyVertex::deviceId)
//                .forEach(did -> switches.add(did));
        //
        for (DeviceId did : switches) {
            // did.uri().getFragment()

//         log.info("intmon:----------- " + did.uri().getFragment());
            Set<Host> conHosts = Sets.newHashSet();
            conHosts = hostService.getConnectedHosts(did);
            for (Host host : conHosts) {
                log.info("hosts: " + host.ipAddresses().toString());
            }
        }

        // build flow rule

        // Bmv2Configuration myConfiguration = INTMON_CONTEXT.configuration();
        tableMap = INTMON_CONTEXT.interpreter().tableIdMap().inverse();

        for (String s : tableMap.keySet()) {
            log.info(s + " " + tableMap.get(s));
        }

        for (DeviceId did : switches) {
            // int didNum = Integer.parseInt(did.uri().getFragment());

            installFixedRules(did);
            installRuleSetSink(did);
            installRuleMirrorIntToCpu(did);
            installRuleSetFirstSw(did);
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
//        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        hostService.removeListener(hostListener);
        processor = null;
    }
/*
* Flowrules that depend on host-did and choice, so we only need to store those rules
* for add or removes. also these rule need to be refresh when new host or switch
* is added
*   - ruleIntSource
*   - ruleSetSource
*   depend on host-did
*   - installRuleSetSink
*   - installRuleMirrorIntToCpu
* */

    private void installRuleIntSink(DeviceId did) {
        ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                .matchExact("i2e", "sink", 1)
                .build();

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_sink")
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(FLOW_PRIORITY)
                .makePermanent()
                .forTable(tableMap.get("tb_int_sink"))
                .build();

        // install flow rule
        flowRuleService.applyFlowRules(rule);
    }

    private void installRuleIntToOnos(DeviceId did) {
        ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                .matchExact("standard_metadata", "instance_type", 1)
                .build();

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_to_onos")
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(FLOW_PRIORITY)
                .makePermanent()
                .forTable(tableMap.get("tb_int_to_onos"))
                .build();

        // install flow rule
        flowRuleService.applyFlowRules(rule);
    }

    private void installRuleIntMetaHeaderUpdate(DeviceId did) {
        ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                .matchExact("i2e", "sink", 0)
                .build();

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_update_total_hop_cnt")
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(FLOW_PRIORITY)
                .makePermanent()
                .forTable(tableMap.get("tb_int_meta_header_update"))
                .build();

        // install flow rule
        flowRuleService.applyFlowRules(rule);
    }

    private void installRuleIntouterEncap(DeviceId did) {
        ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                .matchTernary("i2e", "source", 0, 0) // always true
                .build();

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_update_udp")
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(FLOW_PRIORITY)
                .makePermanent()
                .forTable(tableMap.get("tb_int_outer_encap"))
                .build();

        // install flow rule
        flowRuleService.applyFlowRules(rule);
    }

    private void installRuleIntInsert(DeviceId did) {
        ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                .matchExact("i2e", "sink", 0)
                .build();

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_transit")
                .addParameter("switch_id", Integer.parseInt(did.uri().getFragment()))
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(FLOW_PRIORITY)
                .makePermanent()
                .forTable(tableMap.get("tb_int_insert"))
                .build();

        // install flow rule
        flowRuleService.applyFlowRules(rule);
    }

    private void installRuleIntRestorePort(DeviceId did) {
        ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                .matchExact("i2e", "sink", 1)
                .build();

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("restore_port")
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(FLOW_PRIORITY)
                .makePermanent()
                .forTable(tableMap.get("tb_restore_port"))
                .build();

        // install flow rule
        flowRuleService.applyFlowRules(rule);
    }

    private void installRuleTbIntInst0003(DeviceId did) {
        for (int i = 0; i < 16; i++) {
            ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                    .matchExact("int_header", "instruction_mask_0003", i)
                    .build();

            ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                    .setActionName("int_set_header_0003_i" + i)
                    .build();
            //
            FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                    // we need to map table name (string) to table id (number)
                    .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                    .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                    .withPriority(FLOW_PRIORITY + i)
                    .makePermanent()
                    .forTable(tableMap.get("tb_int_inst_0003"))
                    .build();

            // install flow rule
            flowRuleService.applyFlowRules(rule);
        }
    }

    private void installRuleTbIntInst0407(DeviceId did) {
        for (int i = 0; i < 16; i++) {
            ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                    .matchExact("int_header", "instruction_mask_0407", i)
                    .build();

            ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                    .setActionName("int_set_header_0407_i" + i)
                    .build();
            //
            FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                    // we need to map table name (string) to table id (number)
                    .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                    .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                    .withPriority(FLOW_PRIORITY + i)
                    .makePermanent()
                    .forTable(tableMap.get("tb_int_inst_0407"))
                    .build();

            // install flow rule
            flowRuleService.applyFlowRules(rule);
        }
    }

    private void installRuleTbIntBos(DeviceId did) {
        byte[] values0407 = new byte[]{1, 2, 4, 8, 0, 0, 0, 0};
        byte[] masks0407 = new byte[]{0x1, 0x3, 0x7, 0xf, 0xf, 0xf, 0xf, 0xf};
        byte[] values0003 = new byte[]{0, 0, 0, 0, 1, 2, 4, 8};
        byte[] masks0003 = new byte[]{0, 0, 0, 0, 0x1, 0x3, 0x7, 0xf};
        // 7: 1 mask 1
        // 6: 10 mask 11
        // 5: 100 mask 111
        // 4: 1000 mask 1111
        // 3: 0407: 0 mask 1111 and 0003: 1 mask 1, and so on
        for (int i = 0; i < 8; i++) {
            int j = 7 - i;
            ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                    .matchTernary("int_header", "instruction_mask_0407", values0407[i], masks0407[i])// high priority
                    .matchTernary("int_header", "instruction_mask_0003", values0003[i], masks0003[i])// low priority
                    .build();

            ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                    .setActionName("int_set_header_" + j + "_bos")
                    .build();
            //
            FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                    // we need to map table name (string) to table id (number)
                    .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                    .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                    .withPriority(FLOW_PRIORITY + i)
                    .makePermanent()
                    .forTable(tableMap.get("tb_int_bos"))
                    .build();

            // install flow rule
            flowRuleService.applyFlowRules(rule);
        }
    }

    // those above install* rules do not depend on the input, so just install it at
    // starting and then we are free from them :)
    private void installFixedRules(DeviceId did) {
        installRuleIntSink(did);
        installRuleIntToOnos(did);
        installRuleIntMetaHeaderUpdate(did);
        installRuleIntouterEncap(did);
        installRuleIntInsert(did);
        installRuleIntRestorePort(did);
        installRuleTbIntInst0003(did);
        installRuleTbIntInst0407(did);
        installRuleTbIntBos(did);
        installMirrorId(did);
    }

    private FlowRule ruleIntSource(DeviceId did, Bmv2FlowsFilter flowsFilter, Integer insMask0007, Integer priority) {
        int ins_cnt = 0;
        for (int j = 0; j < 8; j++) {
            if (((insMask0007 >> j) & 0x01) != 0) {
                ins_cnt++;
            }
        }

        int maxHop = 24;
        if (ins_cnt > 1) {
            maxHop = 24/ins_cnt;
        }

        Bmv2ExtensionSelector.Builder exSelectorBuilder = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION);

        exSelectorBuilder = exSelectorBuilder.matchExact("i2e", "sink", 0);
        exSelectorBuilder = exSelectorBuilder.matchExact("i2e", "source", 1);

        if ((flowsFilter.ip4SrcPrefix != null) && (!flowsFilter.ip4SrcPrefix.equals(Ip4Prefix.valueOf(0, 0)))) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("ipv4", "srcAddr",
                                                               flowsFilter.ip4SrcPrefix.address().toInt(),
                                                               (0xFFFFFFFF << (32 - flowsFilter.ip4SrcPrefix.prefixLength())) & 0xFFFFFFFF);
        }

        if ((flowsFilter.ip4DstPrefix != null) && (!flowsFilter.ip4DstPrefix.equals(Ip4Prefix.valueOf(0, 0)))) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("ipv4", "dstAddr",
                                                               flowsFilter.ip4DstPrefix.address().toInt(),
                                                               (0xFFFFFFFF << (32 - flowsFilter.ip4DstPrefix.prefixLength())) & 0xFFFFFFFF);
        }

        // assume -1 is for match all
        if (flowsFilter.srcPort != null && flowsFilter.srcPort != -1) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("udp", "srcPort",
                                                               flowsFilter.srcPort, 0xFFFF);
        }

        if (flowsFilter.dstPort != null && flowsFilter.dstPort != -1) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("udp", "dstPort",
                                                               flowsFilter.dstPort, 0xFFFF);
        }

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_source")
                .addParameter("max_hop", maxHop) // max 24 fields
                .addParameter("ins_cnt", ins_cnt)
                .addParameter("ins_mask0003", (insMask0007 >> 4) & 0x0F)
                .addParameter("ins_mask0407", insMask0007 & 0x0F)
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(exSelectorBuilder.build(), did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(priority)
                .makePermanent()
                .forTable(tableMap.get("tb_int_source"))
                .build();

        return rule;
    }

    private FlowRule ruleSetSource(DeviceId did, Bmv2FlowsFilter flowsFilter, Integer priority) {
//        Set<Host> conHosts = Sets.newHashSet();
//        List<FlowRule> flowRulesList = new ArrayList<>();
//
//        conHosts = hostService.getConnectedHosts(did);
//        for (Host host : conHosts) {
//            for (IpAddress ipAddress : host.ipAddresses()) {
//                if (ipAddress != null) {
//         log.info("hosts: " + host.ipAddresses().toString());
        Bmv2ExtensionSelector.Builder exSelectorBuilder = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION);

//                    if ((flowsFilter.ip4SrcPrefix != null) && (!flowsFilter.ip4SrcPrefix.equals(Ip4Prefix.valueOf(0, 0)))) {
//                        if (flowsFilter.ip4SrcPrefix.contains(ipAddress)) {
//                            exSelectorBuilder = exSelectorBuilder.matchTernary("ipv4", "srcAddr",
//                                                                               ipAddress.getIp4Address().toInt(),
//                                                                               0xFFFF_FFFF);
//                        }
//                    } else {
//                        exSelectorBuilder = exSelectorBuilder.matchTernary("ipv4", "srcAddr",
//                                                                           ipAddress.getIp4Address().toInt(),
//                                                                           0xFFFF_FFFF);
//                    }

        exSelectorBuilder = exSelectorBuilder.matchExact("i2e", "first_sw", 1);

        if ((flowsFilter.ip4SrcPrefix != null) && (!flowsFilter.ip4SrcPrefix.equals(Ip4Prefix.valueOf(0, 0)))) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("ipv4", "srcAddr",
                                                               flowsFilter.ip4SrcPrefix.address().toInt(),
                                                               (0xFFFFFFFF << (32 - flowsFilter.ip4SrcPrefix.prefixLength())) & 0xFFFFFFFF);
        }

        if ((flowsFilter.ip4DstPrefix != null) && (!flowsFilter.ip4DstPrefix.equals(Ip4Prefix.valueOf(0, 0)))) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("ipv4", "dstAddr",
                                                               flowsFilter.ip4DstPrefix.address().toInt(),
                                                               (0xFFFFFFFF << (32 - flowsFilter.ip4DstPrefix.prefixLength())) & 0xFFFFFFFF);
        }

        // assume -1 is for match all
        if (flowsFilter.srcPort != null && flowsFilter.srcPort != -1) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("udp", "srcPort",
                                                               flowsFilter.srcPort, 0xFFFF);
        }

        if (flowsFilter.dstPort != null && flowsFilter.dstPort != -1) {
            exSelectorBuilder = exSelectorBuilder.matchTernary("udp", "dstPort",
                                                               flowsFilter.dstPort, 0xFFFF);
        }

        ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                .setActionName("int_set_source")
                .build();
        //
        FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                // we need to map table name (string) to table id (number)
                .withSelector(DefaultTrafficSelector.builder().extension(exSelectorBuilder.build(), did).build())
                .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                .withPriority(priority)
                .makePermanent()
                .forTable(tableMap.get("tb_set_source"))
                .build();

        // install flow rule
        // flowRuleService.applyFlowRules(rule);

        return rule;


    }

    // installRuleSetSink and mirror to cpu do not depend on floefilter and instruction mask
    // so install it at starting and anytime a new host is added
    private void installRuleSetFirstSw(DeviceId did) {
        Set<Host> conHosts = Sets.newHashSet();

        conHosts = hostService.getConnectedHosts(did);
        for (Host host : conHosts) {
            for (IpAddress ipAddress : host.ipAddresses()) {
//         log.info("hosts: " + host.ipAddresses().toString());
                if (ipAddress != null) {
                    ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                            .matchExact("ipv4", "srcAddr", ipAddress.getIp4Address().toInt())
                            .build();

                    ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                            .setActionName("int_set_first_sw")
                            .build();
                    //
                    FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                            // we need to map table name (string) to table id (number)
                            .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                            .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                            .withPriority(FLOW_PRIORITY)
                            .makePermanent()
                            .forTable(tableMap.get("tb_set_first_sw"))
                            .build();

                    // install flow rule
                    flowRuleService.applyFlowRules(rule);
                }
            }
        }

    }

    private void installRuleSetSink(DeviceId did) {
        Set<Host> conHosts = Sets.newHashSet();
        conHosts = hostService.getConnectedHosts(did);
        int localPriority = 1; // should not from 0
        for (Host host : conHosts) {
            for (IpAddress ipAddress : host.ipAddresses()) {
                if (ipAddress != null) {
//         log.info("hosts: " + host.ipAddresses().toString());
                    ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                            .matchTernary("ipv4", "dstAddr", ipAddress.getIp4Address().toInt(), 0xFFFF_FFFF) //----------------- right? (int type)
                            .build();

                    ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                            .setActionName("int_set_sink")
                            .build();
                    //
                    FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                            // we need to map table name (string) to table id (number)
                            .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                            .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                            .withPriority(localPriority++)
                            .makePermanent()
                            .forTable(tableMap.get("tb_set_sink"))
                            .build();

                    // install flow rule
                     flowRuleService.applyFlowRules(rule);
                }
            }
        }
    }

    private void installRuleMirrorIntToCpu(DeviceId did) {
        Set<Host> conHosts = Sets.newHashSet();
        conHosts = hostService.getConnectedHosts(did);

        for (Host host : conHosts) {
            for (IpAddress ipAddress : host.ipAddresses()) {
                if (ipAddress != null) {
//         log.info("hosts: " + host.ipAddresses().toString());
                    ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
                            .matchExact("ipv4", "dstAddr", ipAddress.getIp4Address().toInt())
                            .build();

                    ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
                            .setActionName("mirror_int_to_cpu")
                            .build();
                    //
                    FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
                            // we need to map table name (string) to table id (number)
                            .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
                            .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
                            .withPriority(FLOW_PRIORITY)
                            .makePermanent()
                            .forTable(tableMap.get("tb_mirror_int_to_cpu"))
                            .build();

                    // install flow rule
                     flowRuleService.applyFlowRules(rule);
                }
            }
        }
    }

    private static Bmv2Configuration loadConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    IntMon.class.getResourceAsStream(JSON_CONFIG_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
    }

    public void deployDevices() {
        //        Set<Device> devices = Sets.newHashSet();
        //        switches.stream().map(deviceService::getDevice)
        //              .forEach(device -> devices.add(device));
        //        for (Device device : devices) {
        //            DeviceId deviceId = device.id();

        topo = topologyService.currentTopology();
        TopologyGraph graph = topologyService.getGraph(topo);
        graph.getVertexes().stream()
                .map(TopologyVertex::deviceId)
                .forEach(did -> switches.add(did));

        for (DeviceId deviceId : switches) {

            // Synchronize executions over the same device.
            //        Lock lock = deviceLocks.computeIfAbsent(deviceId, k -> new ReentrantLock());
            //        lock.lock();

            try {
                // Set context if not already done.
                //                if (!contextFlags.getOrDefault(deviceId, false)) {
                //                    log.info("Setting context to {} for {}...", configurationName, deviceId);
                bmv2ContextService.setContext(deviceId, INTMON_CONTEXT);
                //                    contextFlags.put(device.id(), true);
                //                }

            } finally {
                //            lock.unlock();
            }
        }
    }

    private void installMirrorId(DeviceId did) {
        try {
            Bmv2DeviceThriftClient client = (Bmv2DeviceThriftClient) bmv2Controller.getAgent(did);
            client.addMirrorId(250, 255);
        } catch (Bmv2RuntimeException e) {
            log.info("error--- mirroring---");
        }
    }


    @Override
    public void setFlowFilter(Bmv2FlowsFilter flowsFilter, Integer insMask0007, Integer priority) {
        /*
        * 3 case:
        * + same all: return;
        * + same flowFilter, diff insMask: remove old ones, build and apply new ones
        * + new flowFilter: build and apply new ones
        * */
        Set<FlowRule> newRules = Sets.newHashSet();

        if (flowsFilterInsMap.containsKey(flowsFilter)) {
            if (flowsFilterInsMap.get(flowsFilter).getMiddle().equals(insMask0007) &&
                    flowsFilterInsMap.get(flowsFilter).getRight().equals(priority) ) {
                return;
            }
            // same flowFilter but diff insMask or priority
             /*
             FIXME: only remove flowrules for intended flowfilter, but this cause sw misbehavior
             so we temporary change to remove and re-install all flowrule (performance decrease)
             the problem is on the action parameter of set_int_src
             */
//            for( FlowRule rule: flowsFilterRulesMap.get(flowsFilter)) {
//                flowRuleService.removeFlowRules(rule);
//            }
//
//            for (FlowRule rule : newRules) {
//                flowRuleService.applyFlowRules(rule);
//            }
            FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
            for(DeviceId did: switches) {
                newRules.add(ruleIntSource(did, flowsFilter, insMask0007, priority));
                newRules.add(ruleSetSource(did, flowsFilter, priority));
            }
            for( Bmv2FlowsFilter ff: flowsFilterRulesMap.keySet()) {
//                removeFlowRules(flowsFilterRulesMap.get(ff));
                flowsFilterRulesMap.get(ff).forEach(opsBuilder::remove);
            }
//            removeFlowRules(dRules);

//            try {
//                java.lang.Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            // keep the id
            Integer oldId = flowsFilterInsMap.get(flowsFilter).getLeft();
            flowsFilterInsMap.replace(flowsFilter, Triple.of(oldId, insMask0007, priority));
//            dRules.removeAll(flowsFilterRulesMap.get(flowsFilter));
//            dRules.addAll(newRules);
            flowsFilterRulesMap.replace(flowsFilter, newRules);
            idFlowsFilterMap.replace(oldId, flowsFilter);

            // reinstall all rule
            opsBuilder = opsBuilder.newStage();
            for( Bmv2FlowsFilter ff: flowsFilterRulesMap.keySet()) {
//                installFlowRules(flowsFilterRulesMap.get(ff));
                flowsFilterRulesMap.get(ff).forEach(opsBuilder::add);
            }
            flowRuleService.apply(opsBuilder.build());
//            installFlowRules(dRules);

            return;
        }

        // new flowsFilter
        flowsFilterInsMap.put(flowsFilter, Triple.of(flowsFilterId, insMask0007, priority));
        idFlowsFilterMap.put(flowsFilterId, flowsFilter);
        flowsFilterId++;


        for (DeviceId did : switches) {
            // int didNum = Integer.parseInt(did.uri().getFragment());
            // new rules
            newRules.add(ruleIntSource(did, flowsFilter, insMask0007, priority));
            newRules.add(ruleSetSource(did, flowsFilter, priority));
        }
        flowsFilterRulesMap.put(flowsFilter, newRules);
        dRules.addAll(newRules);
//        dRules.addAll(newRules);
//        installFlowRules(newRules);
        for (FlowRule rule : newRules) {
            flowRuleService.applyFlowRules(rule);
        }
        log.info("---setFlowFilter");
    }

    @Override
    public void delFlowFilter(Integer id) {
//        flowsFilterInsMap.replace(flowsFilter, Triple.of(oldId, insMask0007, priority));
        if (id == null) return;
        /*
        FIXME: only remove flowrules for intended flowfilter, but this cause sw misbehavior
        so we temporary change to remove and re-install all flowrule (performance decrease)
        */
       /*
        Bmv2FlowsFilter ffToDel = idFlowsFilterMap.get(id);
        for( FlowRule rule: flowsFilterRulesMap.get(ffToDel)) {
            flowRuleService.removeFlowRules(rule);
        }

        flowsFilterRulesMap.remove(ffToDel);
        flowsFilterInsMap.remove(ffToDel);
        idFlowsFilterMap.remove(id);
        */

        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
        Bmv2FlowsFilter ffToDel = idFlowsFilterMap.get(id);
        // remove all rules
        for( Bmv2FlowsFilter ff: flowsFilterRulesMap.keySet()) {
//            removeFlowRules(flowsFilterRulesMap.get(ff));
            flowsFilterRulesMap.get(ff).forEach(opsBuilder::remove);
        }
        //***************
        // can build using the operation builder to do remove then add sequentially?
//        try {
//            java.lang.Thread.sleep(100);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


//        removeFlowRules(dRules);

        // remove ffToDel
//        dRules.removeAll(flowsFilterRulesMap.get(ffToDel));
        flowsFilterRulesMap.remove(ffToDel);
        flowsFilterInsMap.remove(ffToDel);
        idFlowsFilterMap.remove(id);



        // reinstall remain rules
//        installFlowRules(dRules);
        opsBuilder = opsBuilder.newStage();
        for( Bmv2FlowsFilter ff: flowsFilterRulesMap.keySet()) {
//            installFlowRules(flowsFilterRulesMap.get(ff));
            flowsFilterRulesMap.get(ff).forEach(opsBuilder::add);
        }

        flowRuleService.apply(opsBuilder.build());
        log.info("--- delete Flow Rules");
//        flowRuleService.apply(flowRuleOperationBuilder.build());
    }

    @Override
    public Map<Bmv2FlowsFilter, Triple<Integer, Integer, Integer>> getAllFlowsFilter() {
        return Collections.unmodifiableMap(flowsFilterInsMap);
    }

    private void installFlowRules(Collection<FlowRule> rules) {
        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
        rules.forEach(opsBuilder::add);
        flowRuleService.apply(opsBuilder.build());
    }

    private void removeFlowRules(Collection<FlowRule> rules) {
        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
        rules.forEach(opsBuilder::remove);
        flowRuleService.apply(opsBuilder.build());
    }

    // A listener of host events that generates flow rules each time a new host is added.
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            // host added. we do not need to do anything if host removed
            Set<FlowRule> newRules = Sets.newHashSet();
            if (event.type() == HOST_ADDED) {
                // add rules for switch contains this host
//                Host addedHost = event.prevSubject();
//                DeviceId did = null;

//                for (DeviceId did : switches) {
//                    if (hostService.getConnectedHosts(did).contains(addedHost)) {
//                        did = did;
//                        break; // do not need to find anymore
//                    }
//                }
                for (DeviceId did : switches) {
                    installRuleSetSink(did);
                    installRuleMirrorIntToCpu(did);
                    installRuleSetFirstSw(did);
                }

                FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
                for (Bmv2FlowsFilter flowsFilter: flowsFilterInsMap.keySet()) {
                    for (DeviceId did : switches) {
                        newRules.add(ruleIntSource(did, flowsFilter, flowsFilterInsMap.get(flowsFilter).getMiddle(),
                                                   flowsFilterInsMap.get(flowsFilter).getRight()));
                        newRules.add(ruleSetSource(did, flowsFilter, flowsFilterInsMap.get(flowsFilter).getRight()));
                    }
//                    newRules.forEach(opsBuilder::remove);
                    flowsFilterRulesMap.get(flowsFilter).addAll(newRules);
                }

//                opsBuilder = opsBuilder.newStage();
                for (Bmv2FlowsFilter flowsFilter: flowsFilterInsMap.keySet()) {
                    flowsFilterRulesMap.get(flowsFilter).forEach(opsBuilder::add);
                }
                flowRuleService.apply(opsBuilder.build());
//                for (FlowRule rule : newRules) {
//                    flowRuleService.applyFlowRules(rule);
//                }

                log.info("----------- new hosts ------------");
            }
        }

        @Override
        public boolean isRelevant(HostEvent event) {
            return event.type() == HOST_ADDED;
        }
    }

    // This part is not used, since the packet Processor is moved to provided
    private class SwitchPacketProcesser implements PacketProcessor {
        @Override
        public void process(PacketContext pc) {
//            if (pc.inPacket().parsed().getEtherType() == Ethernet.TYPE_IPV4) {
//                log.info("intmon: ipv4" + pc.toString());
//            }

            InboundPacket pkt = pc.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) return;

            IPv4 ipv4Pkt = (IPv4) ethPkt.getPayload();
            if (ipv4Pkt.getProtocol() != IPv4.PROTOCOL_UDP) return;

            UDP udpPkt = (UDP) ipv4Pkt.getPayload();
            if (udpPkt.getDestinationPort() != UDP_DST_PORT_INT) return;

            byte[] intRaw = udpPkt.getPayload().serialize();
            Bmv2IntUdp intUdpPkt = Bmv2IntUdp.deserialize(intRaw, 0, intRaw.length);

            if (intUdpPkt.o != 1) return;


            // now we has the intUDP pkt that send to onos at the last sw.
            // we will store the last intUDP for each Bmv2FiveTupleFlow
            // set time first
            // TODO: improve performance (this process take much time)
            intUdpPkt.setRecvTime(pc.time());

            Ip4Address srcAddr = Ip4Address.valueOf(ipv4Pkt.getSourceAddress());
            Integer srcPort = udpPkt.getSourcePort();
            Ip4Address dstAddr = Ip4Address.valueOf(ipv4Pkt.getDestinationAddress());
            Integer dstPort = intUdpPkt.originalPort;

            Bmv2FiveTupleFlow fiveTupleFlow = new Bmv2FiveTupleFlow(srcAddr, srcPort, dstAddr, dstPort);

            if (lastestMonDataMap.containsKey(fiveTupleFlow)) {
                Integer oldId = lastestMonDataMap.get(fiveTupleFlow).getLeft();
                lastestMonDataMap.put(fiveTupleFlow, Pair.of(oldId, intUdpPkt));
            } else {
                lastestMonDataMap.put(fiveTupleFlow, Pair.of(fiveTupleFlowId, intUdpPkt));
                idMonFlowMap.put(fiveTupleFlowId, fiveTupleFlow);
                fiveTupleFlowId++;

            }

//            pc.block();
//            log.info(intUdpPkt.getIntDataString());
            log.info("---received int to onos packet");
        }
    }

    @Override
    public Map<Bmv2FiveTupleFlow, Pair<Integer, Bmv2IntUdp>> getLatestRawMonData(){
        removeOldMonData();
        return Collections.unmodifiableMap(lastestMonDataMap);
    }

    @Override
    public Map<Integer, Bmv2FiveTupleFlow> getIdMonFlowMap() {
        removeOldMonData();
        return Collections.unmodifiableMap(idMonFlowMap);
    }

    private void removeOldMonData() {
        long curTime = System.currentTimeMillis();
        // see ConcurrentModificationException for details
        Set<Bmv2FiveTupleFlow> toRemoveFtf = Sets.newHashSet();
        Set<Integer> toRemoveId = Sets.newHashSet();
        for (Bmv2FiveTupleFlow ftf : lastestMonDataMap.keySet()) {
            // remove if timeout
            if (curTime - lastestMonDataMap.get(ftf).getRight().recvTime > 5000) {
                Integer id = lastestMonDataMap.get(ftf).getLeft();
                toRemoveFtf.add(ftf);
                toRemoveId.add(id);
            }
        }
        toRemoveFtf.forEach(lastestMonDataMap::remove);
        toRemoveId.forEach(idMonFlowMap::remove);
    }
}
