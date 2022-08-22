package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.GeoMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RadonSimplifiedWrapper implements GeoMapper {

    double value;

    public RadonSimplifiedWrapper(double value) {
        this.value = value;
    }

    @Override
    public AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        Map<String, Geometry> sourceSimpl = new HashMap<>();
        Map<String, Geometry> targetSimpl = new HashMap<>();

        sourceData.forEach((s, geometry) -> {
            Geometry simplify = TopologyPreservingSimplifier.simplify(geometry, value);
            if(!simplify.isValid()){
                simplify = geometry;
            }
            sourceSimpl.put(s, simplify);
        });
        targetData.forEach((s, geometry) -> {
            Geometry simplify = TopologyPreservingSimplifier.simplify(geometry, value);
            if(!simplify.isValid()){
                simplify = geometry;
            }
            targetSimpl.put(s, simplify);
        });

        return RADON.getMapping(sourceSimpl, targetSimpl, relation, numThreads);
    }

    @Override
    public String getIndexingName() {
        return "RADON_SIMPLIFIED_"+value;
    }

    @Override
    public String getMatcherName() {
        return getIndexingName();
    }

}
