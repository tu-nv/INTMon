package org.onosproject.intmon;

import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.net.Port;

public class FlowsFilter {
    Ip4Prefix ip4SrcPrefix;
    Ip4Prefix ip4DstPrefix;
    Integer srcPort;
    Integer dstPort;

    public FlowsFilter(Ip4Prefix ip4SrcPrefix, Ip4Prefix ip4DstPrefix, Integer srcPort, Integer dstPort) {
        this.ip4SrcPrefix = ip4SrcPrefix;
        this.ip4DstPrefix = ip4DstPrefix;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }
}
