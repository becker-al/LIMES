package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater;

import org.locationtech.jts.geom.Envelope;

public interface Relater {
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

    public boolean relate(Envelope mbrA, Envelope mbrB, String relation);

    String getName();

}
