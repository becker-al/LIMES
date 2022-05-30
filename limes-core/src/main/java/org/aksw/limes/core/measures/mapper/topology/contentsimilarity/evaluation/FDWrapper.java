package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.ContentSimilarityDiagonal;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

public class FDWrapper implements GeoMapper{

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        Map<String, Envelope> sourceMBB = new HashMap<>();
        Map<String, Envelope> targetMBB = new HashMap<>();
        sourceData.forEach((s, geometry) -> sourceMBB.put(s, geometry.getEnvelopeInternal()));
        targetData.forEach((s, geometry) -> targetMBB.put(s, geometry.getEnvelopeInternal()));

        return ContentSimilarityDiagonal.getMapping(sourceMBB, targetMBB, relation, numThreads);
    }

}
