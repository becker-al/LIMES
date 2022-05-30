package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class RadonWrapper implements GeoMapper{

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        return RADON.getMapping(sourceData, targetData, relation, numThreads);
    }

}
