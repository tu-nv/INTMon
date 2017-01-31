package org.onosproject.intmon;

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
        MoreObjects.ToStringHelper tsh = toStringHelper(getClass());
        if (switchId != null) tsh = tsh.add("Switch ID", Integer.toString(switchId));
        if (ingressPortId != null) tsh = tsh.add("Ingress Port ID", Integer.toString(ingressPortId));
        if (hopLatency != null) tsh = tsh.add("Hop Latency", Integer.toString(hopLatency));
        if (qOccupancy != null) tsh = tsh.add("Queue Occupancy", Integer.toString(qOccupancy));
        if (ingressTstamp != null) tsh = tsh.add("Ingress Time-stamp", Integer.toString(ingressTstamp));
        if (egressPortId != null) tsh = tsh.add("Egress Port ID", Integer.toString(egressPortId));
        if (qCongestion != null) tsh = tsh.add("Queue Congestion", Integer.toString(qCongestion));
        if (ePortTxUtilization != null) tsh = tsh.add("EPortTX Utilization", Integer.toString(ePortTxUtilization));

        return tsh.toString();
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
