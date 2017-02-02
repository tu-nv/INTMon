package org.onosproject.intmon.lib;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;

public class IntUDP {

    private static final int UDP_HEADER_LEN_INT = 12;

    public int controlFields; // 16 bits below
    public byte ver; // 2
    public byte rep; // 2
    public byte c; // 1
    public byte e; // 1
    public byte o; // 1 // sent to onos
    public byte rsvd1; // 4 // rsvd1 reduced from 5 to 4 bits
    public byte insCnt; // 5

    public byte maxHopCnt; // 8
    public byte totalHopCnt; // 8

    public int instructionMask; // 16
//    protected byte instructionMask0007; // 8
//    public byte instructionMask0003; // 4 // split the bits for lookup
//    public byte instructionMask0407; // 4
//    public byte instructionMask0811; // 4
//    public byte instructionMask1215; // 4
    public int rsvd2; // 16
    public int intLen; // 16
    public int originalPort; // 16

    public int insMask0007;
    //    protected byte[] monData;
    public LinkedList<IntDataNode> intDataNodeArr = new LinkedList<>();

    public long recvTime = 0;

    public static IntUDP deserialize(final byte[] data, final int offset,
                                     final int length) {
        IntUDP intUDP = new IntUDP();

        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        intUDP.controlFields = (bb.getShort() & 0xffff);
        intUDP.maxHopCnt = bb.get();
        intUDP.totalHopCnt = bb.get();
        intUDP.instructionMask = bb.getShort() & 0xffff;
        intUDP.rsvd2 = bb.getShort() & 0xffff;
        intUDP.intLen = bb.getShort() & 0xffff;
        intUDP.originalPort = bb.getShort() & 0xffff;

        // parse detail fields
        intUDP.ver = (byte) ((intUDP.controlFields & 0xC000) >> 14);
        intUDP.rep = (byte) ((intUDP.controlFields & 0x3000) >> 12);
        intUDP.c = (byte) ((intUDP.controlFields & 0x0800) >> 11);
        intUDP.e = (byte) ((intUDP.controlFields & 0x0400) >> 10);
        intUDP.o = (byte) ((intUDP.controlFields & 0x0200) >> 9);
        intUDP.rsvd1 = (byte) ((intUDP.controlFields & 0x01E0) >> 5);
        intUDP.insCnt = (byte) ((intUDP.controlFields & 0x001F));

        int monDataLen = intUDP.intLen - UDP_HEADER_LEN_INT;
        if (monDataLen > 0) {
            if (bb.limit() < bb.position() + monDataLen) {
                monDataLen = bb.limit() - bb.position(); // should it return null?
            }
            try {
                int insMask07 = (intUDP.instructionMask >> 8) & 0xFF;
                intUDP.insMask0007 = insMask07;
                for (int i = 0; i < monDataLen / (4 * intUDP.insCnt); i++) {
                    IntDataNode intDataNode = new IntDataNode();
                    if ((insMask07 & 0x80) != 0)
                        intDataNode.switchId = bb.getInt() & 0x7FFF_FFFF; // remove the bos bit
                    if ((insMask07 & 0x40) != 0)
                        intDataNode.ingressPortId = bb.getInt() & 0x7FFF_FFFF;
                    if ((insMask07 & 0x20) != 0)
                        intDataNode.hopLatency = bb.getInt() & 0x7FFF_FFFF;
                    if ((insMask07 & 0x10) != 0)
                        intDataNode.qOccupancy = bb.getInt() & 0x7FFF_FFFF;
                    if ((insMask07 & 0x08) != 0)
                        intDataNode.ingressTstamp = bb.getInt() & 0x7FFF_FFFF;
                    if ((insMask07 & 0x04) != 0)
                        intDataNode.egressPortId = bb.getInt() & 0x7FFF_FFFF;
                    if ((insMask07 & 0x02) != 0)
                        intDataNode.qCongestion = bb.getInt() & 0x7FFF_FFFF;
                    if ((insMask07 & 0x01) != 0)
                        intDataNode.ePortTxUtilization = bb.getInt() & 0x7FFF_FFFF;
                    intUDP.intDataNodeArr.addFirst(intDataNode);
                }
            } catch (final IndexOutOfBoundsException e) {
                intUDP.intDataNodeArr = null;
            }
        }

        return intUDP;
    }

    public void setRecvTime(long recvTime) {
        this.recvTime = recvTime;
//        return this;
    }

    public String getIntDataString() {
//        MoreObjects.ToStringHelper tsh = toStringHelper(getClass());
//        for (IntDataNode intDataNode : intDataNodeArr) {
//            tsh.add("SWITCH", intDataNode.toString());
//            tsh.addValue(System.getProperty("line.separator"));
//        }
//        return tsh.toString();

        StringBuilder sb = new StringBuilder();
        for (IntDataNode intDataNode : intDataNodeArr) {
            sb = sb.append("SWITCH: ").append(intDataNode.toString()).append("\n");
        }
        return sb.toString();
    }

    public Map<DevicePair, Integer> getDPairLinkUltiMap () {
        if(!hasSwitchId() || !hasEPortTxUtilization()) return null;

        Map<DevicePair, Integer> dPairLinkUltiMap = Maps.newHashMap();
        for (int i = 0; i < intDataNodeArr.size() - 1; i++) {
            IntDataNode idn = intDataNodeArr.get(i);
            IntDataNode nextIdn = intDataNodeArr.get(i+1);
            DevicePair dPair = new DevicePair(idn.switchId, nextIdn.switchId);
            /*
            FIXME: this should be link utilization, but use hop latency for demo
            since the link utili has not been implemented in bmv2 yet
            */
            dPairLinkUltiMap.put(dPair, idn.hopLatency);
        }

        return dPairLinkUltiMap;
    }

    public boolean hasSwitchId() {
        return ((insMask0007 & 0x80) != 0);
    }

    public boolean hasIngressPortId() {
        return ((insMask0007 & 0x40) != 0);
    }

    public boolean hasHopLatency() {
        return ((insMask0007 & 0x20) != 0);
    }

    public boolean hasQOccupancy() {
        return ((insMask0007 & 0x10) != 0);
    }

    public boolean hasIngressTstamp() {
        return ((insMask0007 & 0x08) != 0);
    }

    public boolean hasEgressPortId() {
        return ((insMask0007 & 0x04) != 0);
    }

    public boolean hasQCongestion() {
        return ((insMask0007 & 0x02) != 0);
    }

    public boolean hasEPortTxUtilization() {
        return ((insMask0007 & 0x01) != 0);
    }

//    public static Deserializer<IntUDP> deserializer() {
//        return (data, offset, length) -> {
//            checkInput(data, offset, length, UDP_HEADER_LEN_INT);
//
//            IntUDP intUDP = new IntUDP();
//
//            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
//            intUDP.controlFields = (bb.getShort() & 0xffff);
//            intUDP.maxHopCnt = bb.get();
//            intUDP.totalHopCnt = bb.get();
//            intUDP.instructionMask = bb.getShort() & 0xffff;
//            intUDP.rsvd2 = bb.getShort() & 0xffff;
//            intUDP.intLen = bb.getShort() & 0xffff;
//            intUDP.originalPort = bb.getShort() & 0xffff;
//
////            int monDataLen = intUDP.intLen - UDP_HEADER_LEN_INT;
////            if (monDataLen > 0) {
////                checkHeaderLength(length, UDP_HEADER_LEN_INT + monDataLen);
////                intUDP.monData = new byte[monDataLen];
////                bb.get(intUDP.monData, 0, monDataLen);
////            }
//
//            // parse detail fields
//            intUDP.ver = (byte) ((intUDP.controlFields & 0xC000) >> 14);
//            intUDP.rep = (byte) ((intUDP.controlFields & 0x3000) >> 12);
//            intUDP.c = (byte) ((intUDP.controlFields & 0x0800) >> 11);
//            intUDP.e = (byte) ((intUDP.controlFields & 0x0400) >> 10);
//            intUDP.o = (byte) ((intUDP.controlFields & 0x0200) >> 9);
//            intUDP.rsvd1 = (byte) ((intUDP.controlFields & 0x01E0) >> 5);
//            intUDP.insCnt = (byte) ((intUDP.controlFields & 0x001F));
//
//            int monDataLen = intUDP.intLen - UDP_HEADER_LEN_INT;
//            if (monDataLen > 0) {
//                checkHeaderLength(length, UDP_HEADER_LEN_INT + monDataLen);
//
//                int insMask0007 = (intUDP.instructionMask >> 8) & 0xFF;
//                for (int i = 0; i < monDataLen / (4*intUDP.insCnt); i++) {
//                    IntDataNode intDataNode = new IntDataNode();
//                    if ((insMask0007 & 0x80) != 0) intDataNode.switchId = bb.getInt();
//                    if ((insMask0007 & 0x40) != 0) intDataNode.ingressPortId = bb.getInt();
//                    if ((insMask0007 & 0x20) != 0) intDataNode.hopLatency = bb.getInt();
//                    if ((insMask0007 & 0x10) != 0) intDataNode.qOccupancy = bb.getInt();
//                    if ((insMask0007 & 0x08) != 0) intDataNode.ingressTstamp = bb.getInt();
//                    if ((insMask0007 & 0x04) != 0) intDataNode.egressPortId = bb.getInt();
//                    if ((insMask0007 & 0x02) != 0) intDataNode.qCongestion = bb.getInt();
//                    if ((insMask0007 & 0x01) != 0) intDataNode.egressPortTxUtilization = bb.getInt();
//                    intUDP.intDataNodeArr.add(intDataNode);
//                }
//            }
//
//            intUDP.payload = Data.deserializer()
//                    .deserialize(data, bb.position(), bb.limit() - bb.position());
//            intUDP.payload.setParent(intUDP);
//            return intUDP;
//        };
//    }
}
