package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper;

import org.aksw.limes.core.io.mapping.AMapping;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public interface IndexingFull {
    public static final String EQUALS = "equals";
    public static final String DISJOINT = "disjoint";
    public static final String INTERSECTS = "intersects";
    public static final String TOUCHES = "touches";
    //public static final String CROSSES = "crosses"; //Crosses is not defined by godoy
    public static final String WITHIN = "within";
    public static final String CONTAINS = "contains";
    public static final String OVERLAPS = "overlaps";
    public static final String COVERS = "covers";
    public static final String COVEREDBY = "coveredby";

    public AMapping getMapping(MatcherFull relater, Map<String, Geometry> sourceData, Map<String, Geometry> targetData,
                               String relation, int numThreads);

    String getName();
}
