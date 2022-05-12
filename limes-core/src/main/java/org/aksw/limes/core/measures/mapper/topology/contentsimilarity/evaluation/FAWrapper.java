package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.radon.ContentSimilarityArea;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class FAWrapper implements GeoMapper{

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation) {
        return ContentSimilarityArea.getMapping(sourceData, targetData, relation);
    }

    @Override
    public AMapping getMapping(ACache source, ACache target, String sourceVar, String targetVar, String expression, double threshold, String relation) {
        return ContentSimilarityArea.getMapping(source, target, sourceVar, targetVar, expression, threshold, relation);
    }

}
