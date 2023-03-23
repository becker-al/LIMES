package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class CombinedFullGeoMapper implements GeoMapper {

    private IndexingFull indexing;
    private MatcherFull matcher;

    public CombinedFullGeoMapper(IndexingFull indexing, MatcherFull matcher) {
        this.indexing = indexing;
        this.matcher = matcher;
    }

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        matcher.reset();
        AMapping mapping = indexing.getMapping(matcher, sourceData, targetData, relation, numThreads);
        matcher.reset();
        return mapping;
    }

    @Override
    public String getIndexingName() {
        return indexing.getName();
    }

    @Override
    public String getMatcherName() {
        return matcher.getName();
    }
}
