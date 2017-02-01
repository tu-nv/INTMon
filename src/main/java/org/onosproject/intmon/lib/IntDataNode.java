package org.onosproject.intmon.lib;

import com.google.common.base.MoreObjects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class IntDataNode {
    public Integer switchId;
    public Integer ingressPortId;
    public Integer hopLatency;
    public Integer qOccupancy;
    public Integer ingressTstamp;
    public Integer egressPortId;
    public Integer qCongestion;
    public Integer ePortTxUtilization;

    public IntDataNode() {
        switchId = null;
        ingressPortId = null;
        hopLatency = null;
        qOccupancy = null;
        ingressTstamp = null;
        egressPortId = null;
        qCongestion = null;
        ePortTxUtilization = null;
    }

    @Override
    public String toString() {
//        MoreObjects.ToStringHelper tsh = toStringHelper(getClass());
        StringBuilder sb = new StringBuilder();

//        if (switchId != null) tsh = tsh.add("Switch ID", Integer.toUnsignedString(switchId));
//        if (ingressPortId != null) tsh = tsh.add("Ingress Port ID", Integer.toUnsignedString(ingressPortId));
//        if (hopLatency != null) tsh = tsh.add("Hop Latency", Integer.toUnsignedString(hopLatency));
//        if (qOccupancy != null) tsh = tsh.add("Queue Occupancy", Integer.toUnsignedString(qOccupancy));
//        if (ingressTstamp != null) tsh = tsh.add("Ingress Time-stamp", Integer.toUnsignedString(ingressTstamp));
//        if (egressPortId != null) tsh = tsh.add("Egress Port ID", Integer.toUnsignedString(egressPortId));
//        if (qCongestion != null) tsh = tsh.add("Queue Congestion", Integer.toUnsignedString(qCongestion));
//        if (ePortTxUtilization != null) tsh = tsh.add("EPortTX Utilization", Integer.toUnsignedString(ePortTxUtilization));

        if (switchId != null) sb = sb.append("Switch ID = ").append(Integer.toUnsignedString(switchId)).append(", ");
        if (ingressPortId != null) sb = sb.append("Ingress Port ID = ").append(Integer.toUnsignedString(ingressPortId)).append(", ");
        if (hopLatency != null) sb = sb.append("Hop Latency = ").append(Integer.toUnsignedString(hopLatency)).append(", ");
        if (qOccupancy != null) sb = sb.append("Queue Occupancy = ").append(Integer.toUnsignedString(qOccupancy)).append(", ");
        if (ingressTstamp != null) sb = sb.append("Ingress Time-stamp = ").append(Integer.toUnsignedString(ingressTstamp)).append(", ");
        if (egressPortId != null) sb = sb.append("Egress Port ID = ").append(Integer.toUnsignedString(egressPortId)).append(", ");
        if (qCongestion != null) sb = sb.append("Queue Congestion = ").append(Integer.toUnsignedString(qCongestion)).append(", ");
        if (ePortTxUtilization != null) sb = sb.append("EPortTX Utilization = ").append(Integer.toUnsignedString(ePortTxUtilization)).append(", ");

//        return tsh.toString();
        return sb.toString();
    }
}

//    protected int controlFields; // 16 bits below
//    protected byte ver; // 2
//    protected byte rep; // 2
//    protected byte c; // 1
//    protected byte e; // 1
//    protected byte o; // 1 // sent to onos
//    protected byte rsvd1; // 4 // rsvd1 reduced from 5 to 4 bits
//    protected byte ins_cnt; // 5