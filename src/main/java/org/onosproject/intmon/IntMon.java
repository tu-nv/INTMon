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
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.TpPort;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.ctl.Bmv2DefaultInterpreterImpl;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration.parse;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class IntMon {
   // Instantiates the relevant services.
   @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
   protected PacketService packetService;

   @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
   protected FlowRuleService flowRuleService;

   @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
   protected DeviceService deviceService;

   @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
   protected CoreService coreService;

   @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
   private Bmv2DeviceContextService bmv2ContextService;

   @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
   protected TopologyService topologyService;

   private final Logger log = LoggerFactory.getLogger(getClass());

   //    private static final TpPort UDP_INT_PORT = TpPort.tpPort(5431);

   // variables
   private ApplicationId appId;
   private PacketProcessor processor;
   // no newHashSet --> error Failed creating the component instance
   private Set<DeviceId> switches = Sets.newHashSet();
   private Topology topo;

   //    private static final String APP_NAME = "org.onosproject.intmon";
   //    private static final String MODEL_NAME = "IntMon";
   //    private static final String JSON_CONFIG_PATH = "~/onos-p4-dev/p4src/build/default.json";
   private static final String JSON_CONFIG_PATH = "/intmon.json";

   private static final Bmv2Configuration INTMON_CONFIGURATION = loadConfiguration();
   private static final IntMonInterpreter INTMON_INTERPRETER = new IntMonInterpreter();
   protected static final Bmv2DeviceContext INTMON_CONTEXT = new Bmv2DeviceContext(INTMON_CONFIGURATION, INTMON_INTERPRETER);
   private static final int FLOW_PRIORITY = 100;
   Map<String, Integer> tableMap;


   @Activate
   protected void activate() {
      log.info("Started. PPAP");
      appId = coreService.getAppId("org.onosproject.intmon");
      processor = new SwitchPacketProcesser();
      packetService.addProcessor(processor, PacketProcessor.director(3));
      bmv2ContextService.registerInterpreterClassLoader(INTMON_CONTEXT.interpreter().getClass(),
                                                        this.getClass().getClassLoader());

      // deploy p4 program to devices
      deployDevices();

      // get all the device id
      topo = topologyService.currentTopology();
      TopologyGraph graph = topologyService.getGraph(topo);
      graph.getVertexes().stream()
            .map(TopologyVertex::deviceId)
            .forEach(did -> switches.add(did));
      //
      for (DeviceId did : switches) {
         // did.uri().getFragment()
         log.info("intmon: " + did.uri().getFragment());
      }

      // build flow rule

      // Bmv2Configuration myConfiguration = INTMON_CONTEXT.configuration();
      tableMap = INTMON_CONTEXT.interpreter().tableIdMap().inverse();

      for (String s : tableMap.keySet()) {
         log.info(s + " " + tableMap.get(s));
      }

      for (DeviceId did : switches) {
         // int didNum = Integer.parseInt(did.uri().getFragment());

//         installRuleMirrorIntToCpu(did);
//         installRuleIntBos(did);
         installRuleTbIntInst0003(did);
         installRuleTbIntInst0407(did);
         installRuleTbIntBos(did);
      }
   }

   @Deactivate
   protected void deactivate() {
      log.info("Stopped");
      packetService.removeProcessor(processor);
   }

   //
   private void installRuleMirrorIntToCpu(DeviceId did) {
      // mirror_int_to_cpu ---------------------
      ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
            .matchExact("standard_metadata", "instance_type", 0)
            // .matchLpm("ipv4", "dstAddr", dstPrefix.address().toOctets(), dstPrefix.prefixLength())
            .build();

      ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
            .setActionName("do_copy_to_cpu")
            // .addParameter("nhop_id", 4)
            .build();
      //
      FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
            // we need to map table name (string) to table id (number)
            .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
            .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
            .withPriority(FLOW_PRIORITY)
            .makePermanent()
            .forTable(tableMap.get("mirror_int_to_cpu"))
            .build();

      // install flow rule
      flowRuleService.applyFlowRules(rule);
   }

   private void installRuleIntBos(DeviceId did) {
      // mirror_int_to_cpu ---------------------
      ExtensionSelector extSelector = Bmv2ExtensionSelector.builder().forConfiguration(INTMON_CONFIGURATION)
            //              .matchExact("standard_metadata", "instance_type", 0)
            .build();

      ExtensionTreatment extTreatment = Bmv2ExtensionTreatment.builder().forConfiguration(INTMON_CONFIGURATION)
            .setActionName("int_set_header_7_bos")
            .build();
      //
      FlowRule rule = DefaultFlowRule.builder().forDevice(did).fromApp(appId)
            // we need to map table name (string) to table id (number)
            .withSelector(DefaultTrafficSelector.builder().extension(extSelector, did).build())
            .withTreatment(DefaultTrafficTreatment.builder().extension(extTreatment, did).build())
            .withPriority(FLOW_PRIORITY)
            .makePermanent()
            .forTable(tableMap.get("int_bos"))
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
      byte[] values0407 = new byte[] {1, 2, 4, 8, 0, 0, 0 ,0};
      byte[] masks0407 = new byte[] {0x1, 0x3, 0x7, 0xf, 0xf, 0xf, 0xf, 0xf};
      byte[] values0003 = new byte[] {0, 0, 0, 0, 1, 2, 4, 8};
      byte[] masks0003 = new byte[] {0, 0, 0, 0, 0x1, 0x3, 0x7, 0xf};
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

   private class SwitchPacketProcesser implements PacketProcessor {
      @Override
      public void process(PacketContext pc) {
         //            if (pc.inPacket().parsed().getEtherType() == Ethernet.TYPE_IPV4) {
         //                log.info("intmon: ipv4" + pc.toString());
         //            }
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
}
