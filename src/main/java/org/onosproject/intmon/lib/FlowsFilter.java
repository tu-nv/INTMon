package org.onosproject.intmon.lib;

import org.onlab.packet.Ip4Prefix;

import java.util.Objects;

public class FlowsFilter {
    public Ip4Prefix ip4SrcPrefix;
    public Ip4Prefix ip4DstPrefix;
    public Integer srcPort;
    public Integer dstPort;

    public FlowsFilter(Ip4Prefix ip4SrcPrefix, Ip4Prefix ip4DstPrefix, Integer srcPort, Integer dstPort) {
        this.ip4SrcPrefix = ip4SrcPrefix;
        this.ip4DstPrefix = ip4DstPrefix;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip4SrcPrefix, ip4DstPrefix, srcPort, dstPort);
    }

//    private boolean equalsIp4Prefix(Ip4Prefix that, Ip4Prefix other) {
//        if (that == null && other == null) return true;
//        if (that == null) return false;
//        if (other == null) return false;
//
//        if (that.prefixLength() != other.prefixLength()) return false;
//        if (that.address().toInt() != other.address().toInt()) return false;
//
//        return true;
//    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
//        if (!super.equals(obj)) {
//            return false;
//        }
        if (!(obj instanceof FlowsFilter)) {
            return false;
        }
        FlowsFilter other = (FlowsFilter) obj;

//        if (!equalsIp4Prefix(this.ip4DstPrefix, other.ip4DstPrefix)) {
        if (!this.ip4DstPrefix.equals(other.ip4DstPrefix)) {
            return false;
        }

//        if (!equalsIp4Prefix(this.ip4SrcPrefix, other.ip4SrcPrefix)) {
        if (!this.ip4SrcPrefix.equals(other.ip4SrcPrefix)) {
            return false;
        }

        if (!this.srcPort.equals(other.srcPort)) {
            return false;
        }

        if (!this.dstPort.equals(other.dstPort)) {
            return false;
        }

        return true;
    }
}
