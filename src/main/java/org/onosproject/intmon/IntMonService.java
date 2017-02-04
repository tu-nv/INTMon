package org.onosproject.intmon;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.onosproject.intmon.lib.FiveTupleFlow;
import org.onosproject.intmon.lib.FlowsFilter;
import org.onosproject.intmon.lib.IntUDP;

import java.util.Map;

public interface IntMonService {
    public void setFlowFilter(FlowsFilter flowsFilter, Integer insMask0007, Integer priority);
    public void delFlowFilter(Integer id);
    public Map<FlowsFilter, Triple<Integer, Integer, Integer>> getAllFlowsFilter();
    public Map<FiveTupleFlow, Pair<Integer, IntUDP>> getLatestRawMonData();
    public Map<Integer, FiveTupleFlow> getIdMonFlowMap();
}
