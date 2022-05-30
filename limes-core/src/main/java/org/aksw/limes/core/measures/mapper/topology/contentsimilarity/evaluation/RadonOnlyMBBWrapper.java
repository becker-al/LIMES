package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

public class RadonOnlyMBBWrapper implements GeoMapper {

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        Map<String, Geometry> sourceMBB = new HashMap<>();
        Map<String, Geometry> targetMBB = new HashMap<>();
        sourceData.forEach((s, geometry) -> sourceMBB.put(s, geometry.getEnvelope()));
        targetData.forEach((s, geometry) -> targetMBB.put(s, geometry.getEnvelope()));

        return RADON.getMapping(sourceMBB, targetMBB, relation, numThreads);
    }

}
