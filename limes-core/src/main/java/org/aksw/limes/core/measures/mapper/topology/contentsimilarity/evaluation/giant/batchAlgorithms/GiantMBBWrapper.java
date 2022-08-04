package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.batchAlgorithms;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

public class GiantMBBWrapper implements GeoMapper {

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        Map<String, Geometry> sourceMBB = new HashMap<>();
        Map<String, Geometry> targetMBB = new HashMap<>();
        sourceData.forEach((s, geometry) -> sourceMBB.put(s, geometry.getEnvelope()));
        targetData.forEach((s, geometry) -> targetMBB.put(s, geometry.getEnvelope()));

        GIAnt giAnt = new GIAnt(sourceMBB, targetMBB, relation, numThreads);
        giAnt.applyProcessing();
        return giAnt.getM();
    }

    @Override
    public String getIndexingName() {
        return "GIANT";
    }

    @Override
    public String getMatcherName() {
        return "MBB_GIANT";
    }

}
