package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.radon;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class RadonWrapper implements GeoMapper {

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        return RADON.getMapping(sourceData, targetData, relation, numThreads);
    }

    @Override
    public String getIndexingName() {
        return "RADON";
    }

    @Override
    public String getMatcherName() {
        return "RADON";
    }

}
