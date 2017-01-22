package org.onosproject.intmon;

import com.google.common.collect.ImmutableBiMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import javax.annotation.Nullable;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.net.PortNumber.CONTROLLER;

public class IntMonInterpreter implements Bmv2Interpreter {
    public static final String TABLE0 = "table0";
    public static final String PORT_COUNT_TABLE = "port_count_table";
    public static final String SEND_TO_CPU = "send_to_cpu";
    public static final String PORT = "port";
    public static final String DROP = "_drop";
    public static final String SET_EGRESS_PORT = "set_egress_port";
    public static final String TB_INT_INSERT = "tb_int_insert";
    public static final String TB_INT_INST_0003 = "tb_int_inst_0003";
    public static final String TB_INT_INST_0407 = "tb_int_inst_0407";
    public static final String TB_INT_BOS = "tb_int_bos";
    public static final String TB_INT_META_HEADER_UPDATE = "tb_int_meta_header_update";
    public static final String MIRROR_INT_TO_CPU = "mirror_int_to_cpu";
    public static final String TB_INT_OUTER_ENCAP = "tb_int_outer_encap";


    private static final ImmutableBiMap<Criterion.Type, String> CRITERION_MAP = ImmutableBiMap.of(
            Criterion.Type.IN_PORT, "standard_metadata.ingress_port",
            Criterion.Type.ETH_DST, "ethernet.dstAddr",
            Criterion.Type.ETH_SRC, "ethernet.srcAddr",
            Criterion.Type.ETH_TYPE, "ethernet.etherType");

    //    private static final ImmutableBiMap<Integer, String> TABLE_MAP3 = ImmutableBiMap.of(
//            0, TABLE0,
//            1, INT_INSERT,
//            2, INT_INST_0003,
//            3, INT_INST_0407,
//            4, INT_BOS);
    private static final ImmutableBiMap<Integer, String> TABLE_MAP = new ImmutableBiMap.Builder<Integer, String>()
            .put(0, TABLE0)
            .put(1, PORT_COUNT_TABLE)
            .put(2, TB_INT_INST_0003)
            .put(3, TB_INT_INST_0407)
            .put(4, TB_INT_BOS)
            .put(5, TB_INT_META_HEADER_UPDATE)
            .put(6, TB_INT_INSERT)
            .put(7, TB_INT_OUTER_ENCAP)
            .put(8, MIRROR_INT_TO_CPU)
            .put(9, "tb_set_source")
            .put(10, "tb_set_sink")
            .put(11, "tb_mirror_int_to_cpu")
            .put(12, "tb_int_source")
            .put(13, "tb_int_sink")
            .put(14, "tb_int_to_onos")
            .put(15, "tb_restore_port")
            .build();

    @Override
    public ImmutableBiMap<Criterion.Type, String> criterionTypeMap() {
        return CRITERION_MAP;
    }

    @Override
    public ImmutableBiMap<Integer, String> tableIdMap() {
        return TABLE_MAP;
    }

    @Override
    public Bmv2Action mapTreatment(TrafficTreatment treatment, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        if (treatment.allInstructions().size() == 0) {
            // No instructions means drop for us.
            return actionWithName(DROP);
        } else if (treatment.allInstructions().size() > 1) {
            // Otherwise, we understand treatments with only 1 instruction.
            throw new Bmv2InterpreterException("Treatment has multiple instructions");
        }

        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                Instructions.OutputInstruction outInstruction = (Instructions.OutputInstruction) instruction;
                PortNumber port = outInstruction.port();
                if (!port.isLogical()) {
                    return buildEgressAction(port, configuration);
                } else if (port.equals(CONTROLLER)) {
                    return actionWithName(SEND_TO_CPU);
                } else {
                    throw new Bmv2InterpreterException("Egress on logical port not supported: " + port);
                }
            case NOACTION:
                return actionWithName(DROP);
            default:
                throw new Bmv2InterpreterException("Instruction type not supported: " + instruction.type().name());
        }
    }

    private Bmv2Action buildEgressAction(PortNumber port, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        int portBitWidth = configuration.action(SET_EGRESS_PORT).runtimeData(PORT).bitWidth();

        try {
            ImmutableByteSequence portBs = fitByteSequence(copyFrom(port.toLong()), portBitWidth);
            return Bmv2Action.builder()
                    .withName(SET_EGRESS_PORT)
                    .addParameter(portBs)
                    .build();
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new Bmv2InterpreterException(e.getMessage());
        }
    }

    private Bmv2Action actionWithName(String name) {
        return Bmv2Action.builder().withName(name).build();
    }
}
