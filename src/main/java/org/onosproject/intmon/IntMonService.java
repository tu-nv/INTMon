package org.onosproject.intmon;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Map;

public interface IntMonService {
    public void setFlowFilter(FlowsFilter flowsFilter, Integer insMask0007, Integer priority);
    public void delFlowFilter(Integer id);
    public Map<FlowsFilter, Triple<Integer, Integer, Integer>> getAllFlowsFilter();
    public Map<FiveTupleFlow, Pair<Integer, IntUDP>> getRawMonData();
}
