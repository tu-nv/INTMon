package org.onosproject.intmon;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.onosproject.bmv2.api.runtime.Bmv2FiveTupleFlow;
import org.onosproject.bmv2.api.runtime.Bmv2FlowsFilter;
import org.onosproject.bmv2.api.runtime.Bmv2IntUdp;

import java.util.Map;

public interface IntMonService {
    public void setFlowFilter(Bmv2FlowsFilter flowsFilter, Integer insMask0007, Integer priority);
    public void delFlowFilter(Integer id);
    public Map<Bmv2FlowsFilter, Triple<Integer, Integer, Integer>> getAllFlowsFilter();
    public Map<Bmv2FiveTupleFlow, Pair<Integer, Bmv2IntUdp>> getLatestRawMonData();
    public Map<Integer, Bmv2FiveTupleFlow> getIdMonFlowMap();
}
