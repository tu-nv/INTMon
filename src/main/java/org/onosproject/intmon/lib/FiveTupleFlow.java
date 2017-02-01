package org.onosproject.intmon.lib;

import org.onlab.packet.Ip4Address;

import java.util.Objects;

public class FiveTupleFlow {
    public Ip4Address srcAddr;
    public Ip4Address dstAddr;
    public Integer srcPort;
    public Integer dstPort;

    public FiveTupleFlow(Ip4Address srcAddr, Integer srcPort,
                         Ip4Address dstAddr, Integer dstPort) {
        this.srcAddr = srcAddr;
        this.srcPort = srcPort;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }

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
        if (!(obj instanceof FiveTupleFlow)) {
            return false;
        }
        FiveTupleFlow other = (FiveTupleFlow) obj;

        if (!this.srcAddr.equals(other.srcAddr)) {
            return false;
        }

        if (!this.srcPort.equals(other.srcPort)) {
            return false;
        }

        if (!this.dstAddr.equals(other.dstAddr)) {
            return false;
        }

        if (!this.dstPort.equals(other.dstPort)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcAddr, srcPort, dstAddr, dstPort);
    }
}
