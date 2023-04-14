package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper;

import org.locationtech.jts.geom.Geometry;

public interface MatcherFull {

    String EQUALS = "equals";
    String DISJOINT = "disjoint";
    String INTERSECTS = "intersects";
    String TOUCHES = "touches";
    String WITHIN = "within";
    String CONTAINS = "contains";
    String OVERLAPS = "overlaps";
    String COVERS = "covers";
    String COVEREDBY = "coveredby";

    boolean relate(String uriA, Geometry geoA, String uriB, Geometry geoB, String relation);

    String getName();

    void reset();
}
