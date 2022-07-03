package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.mapping.AMapping;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

public interface GeoMapper {

    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData,
                                      String relation, int numThreads);


    public default Map<String, Envelope> toEnvelopeMap(Map<String, Geometry> data){
        Map<String, Envelope> envelopeMap = new HashMap<>();
        data.forEach((s, geometry) -> envelopeMap.put(s, geometry.getEnvelopeInternal()));
        return envelopeMap;
    }

    String getIndexingName();
    String getMatcherName();

}

