package org.onosproject.intmon;

/**
 * Created by tu on 17. 1. 24.
 */
public class IntDataNode {
    public Integer switchId;
    public Integer ingressPortId;
    public Integer hopLatency;
    public Integer qOccupancy;
    public Integer ingressTstamp;
    public Integer egressPortId;
    public Integer qCongestion;
    public Integer egressPortTxUtilization;

    public IntDataNode() {
        switchId = null;
        ingressPortId = null;
        hopLatency = null;
        qOccupancy = null;
        ingressTstamp = null;
        egressPortId = null;
        qCongestion = null;
        egressPortTxUtilization = null;
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
