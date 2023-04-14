package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper;

import org.aksw.limes.core.io.mapping.AMapping;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public interface IndexingFull {
    String EQUALS = "equals";
    String DISJOINT = "disjoint";
    String INTERSECTS = "intersects";
    String TOUCHES = "touches";
    String WITHIN = "within";
    String CONTAINS = "contains";
    String OVERLAPS = "overlaps";
    String COVERS = "covers";
    String COVEREDBY = "coveredby";

    AMapping getMapping(MatcherFull relater, Map<String, Geometry> sourceData, Map<String, Geometry> targetData,
                               String relation, int numThreads);

    String getName();
}
