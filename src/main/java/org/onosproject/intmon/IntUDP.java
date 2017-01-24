package org.onosproject.intmon;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Data;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.onlab.packet.PacketUtils.checkHeaderLength;
import static org.onlab.packet.PacketUtils.checkInput;

public class IntUDP {

    private static final int UDP_HEADER_LEN_INT = 12;

    protected int controlFields; // 16 bits below
    protected byte ver; // 2
    protected byte rep; // 2
    protected byte c; // 1
    protected byte e; // 1
    protected byte o; // 1 // sent to onos
    protected byte rsvd1; // 4 // rsvd1 reduced from 5 to 4 bits
    protected byte insCnt; // 5

    protected byte maxHopCnt; // 8
    protected byte totalHopCnt; // 8

    protected int instructionMask; // 16
//    protected byte instructionMask0007; // 8
    protected byte instructionMask0003; // 4 // split the bits for lookup
    protected byte instructionMask0407; // 4
    protected byte instructionMask0811; // 4
    protected byte instructionMask1215; // 4
    protected int rsvd2; // 16
    protected int intLen; // 16
    protected int originalPort; // 16

//    protected byte[] monData;
    protected ArrayList<IntDataNode> intDataNodeArr = new ArrayList<>();

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
                int insMask0007 = (intUDP.instructionMask >> 8) & 0xFF;
                for (int i = 0; i < monDataLen / (4 * intUDP.insCnt); i++) {
                    IntDataNode intDataNode = new IntDataNode();
                    if ((insMask0007 & 0x80) != 0)
                        intDataNode.switchId = bb.getInt();
                    if ((insMask0007 & 0x40) != 0)
                        intDataNode.ingressPortId = bb.getInt();
                    if ((insMask0007 & 0x20) != 0)
                        intDataNode.hopLatency = bb.getInt();
                    if ((insMask0007 & 0x10) != 0)
                        intDataNode.qOccupancy = bb.getInt();
                    if ((insMask0007 & 0x08) != 0)
                        intDataNode.ingressTstamp = bb.getInt();
                    if ((insMask0007 & 0x04) != 0)
                        intDataNode.egressPortId = bb.getInt();
                    if ((insMask0007 & 0x02) != 0)
                        intDataNode.qCongestion = bb.getInt();
                    if ((insMask0007 & 0x01) != 0)
                        intDataNode.egressPortTxUtilization = bb.getInt();
                    intUDP.intDataNodeArr.add(intDataNode);
                }
            }catch (final IndexOutOfBoundsException e) {
                intUDP.intDataNodeArr = null;
            }
        }

        return intUDP;
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
