package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.batchAlgorithms;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class GiantWrapper implements GeoMapper {

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        GIAnt giAnt = new GIAnt(sourceData, targetData, relation, numThreads);
        giAnt.applyProcessing();
        return giAnt.getM();
    }

    @Override
    public String getIndexingName() {
        return "GIANT";
    }

    @Override
    public String getMatcherName() {
        return "GIANT";
    }

}
