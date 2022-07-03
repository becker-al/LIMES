package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.wrapper;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing.Indexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.Relater;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class AbstractWrapper implements GeoMapper {

    private Indexing indexing;
    private Relater relater;

    public AbstractWrapper(Indexing indexing, Relater relater) {
        this.indexing = indexing;
        this.relater = relater;
    }

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        return indexing.getMapping(relater, toEnvelopeMap(sourceData), toEnvelopeMap(targetData), relation, numThreads);
    }

    @Override
    public String getIndexingName() {
        return indexing.getName();
    }

    @Override
    public String getMatcherName() {
        return relater.getName();
    }
}
