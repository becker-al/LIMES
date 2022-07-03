package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.Relater;
import org.locationtech.jts.geom.Envelope;

import java.util.Map;

public interface Indexing {
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

    public AMapping getMapping(Relater relater, Map<String, Envelope> sourceData, Map<String, Envelope> targetData,
                               String relation, int numThreads);

    String getName();
}
