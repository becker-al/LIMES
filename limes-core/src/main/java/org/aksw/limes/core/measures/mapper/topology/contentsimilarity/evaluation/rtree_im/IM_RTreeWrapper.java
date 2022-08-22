package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.rtree_im;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class IM_RTreeWrapper implements GeoMapper {

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        return IM_RTreeIndexing.getMapping(sourceData, targetData, relation, numThreads);
    }

    @Override
    public String getIndexingName() {
        return "RTREE";
    }

    @Override
    public String getMatcherName() {
        return "DE-9IM";
    }

}
