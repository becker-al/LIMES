package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class RadonWrapper implements GeoMapper{

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        return RADON.getMapping(sourceData, targetData, relation, numThreads);
    }

    @Override
    public AMapping getMapping(ACache source, ACache target, String sourceVar, String targetVar, String expression, double threshold, String relation, int numThreads) {
        return RADON.getMapping(source, target, sourceVar, targetVar, expression, threshold, relation, numThreads);
    }

}