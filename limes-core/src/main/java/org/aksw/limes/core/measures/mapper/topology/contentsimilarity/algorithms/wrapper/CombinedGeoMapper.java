package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.Indexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.Matcher;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class CombinedGeoMapper implements GeoMapper {

    private Indexing indexing;
    private Matcher matcher;

    public CombinedGeoMapper(Indexing indexing, Matcher matcher) {
        this.indexing = indexing;
        this.matcher = matcher;
    }

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        return indexing.getMapping(matcher, toEnvelopeMap(sourceData), toEnvelopeMap(targetData), relation, numThreads);
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
