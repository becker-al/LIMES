package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.rtree_full;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.RTree;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FullRTreeIndexing {

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


    public static AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        List<RTree.Entry> entries = new ArrayList<>(sourceData.size());
        sourceData.forEach((s, geometry) -> {
            entries.add(new RTree.Entry(s, geometry.getEnvelopeInternal(), geometry));
        });

        boolean disjointStrategy = relation.equals(DISJOINT);
        if (disjointStrategy)
            relation = INTERSECTS;

        RTree rTree = RTree.buildSTR(entries);

        AMapping m = MappingFactory.createDefaultMapping();

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        Map<String, Set<String>> results = new HashMap<>(); //Target -> Source Mappings

        for (Map.Entry<String, Geometry> entry : targetData.entrySet()) {
            String uri = entry.getKey();
            Envelope envelope = entry.getValue().getEnvelopeInternal();

            if (numThreads > 1) {
                HashSet<String> value = new HashSet<>();
                results.put(uri, value);
                String finalRelation = relation;

                    List<RTree.Entry> search = rTree.search(envelope);
                for (RTree.Entry x : search) {
                    exec.submit(() -> {
                        Envelope abb = x.getEnvelope();
                        Envelope bbb = envelope;
                        boolean compute = (finalRelation.equals(COVERS) && abb.covers(bbb))
                                || (finalRelation.equals(COVEREDBY) && bbb.covers(abb))
                                || (finalRelation.equals(CONTAINS) && abb.contains(bbb))
                                || (finalRelation.equals(WITHIN) && bbb.contains(abb)) || (finalRelation.equals(EQUALS) && abb.equals(bbb))
                                || finalRelation.equals(INTERSECTS) || finalRelation.equals(TOUCHES)
                                || finalRelation.equals(OVERLAPS);
                        if(compute){
                            Geometry geoA = x.getGeometry();
                            Geometry geoB = entry.getValue();
                            boolean related = relate(geoA, geoB, finalRelation);
                            if(related){
                                synchronized (value){
                                    value.add(x.getUri());
                                }
                            }
                        }
                    });
                }
            } else {
                String finalRelation = relation;
                AMapping finalM = m;
                List<RTree.Entry> search = rTree.search(envelope);
                search.stream()
                        .filter(x -> {
                                    Envelope abb = x.getEnvelope();
                                    Envelope bbb = envelope;
                                    boolean compute = (finalRelation.equals(COVERS) && abb.covers(bbb))
                                            || (finalRelation.equals(COVEREDBY) && bbb.covers(abb))
                                            || (finalRelation.equals(CONTAINS) && abb.contains(bbb))
                                            || (finalRelation.equals(WITHIN) && bbb.contains(abb)) || (finalRelation.equals(EQUALS) && abb.equals(bbb))
                                            || finalRelation.equals(INTERSECTS) || finalRelation.equals(TOUCHES)
                                            || finalRelation.equals(OVERLAPS);
                                    if(compute){
                                        Geometry geoA = x.getGeometry();
                                        Geometry geoB = entry.getValue();
                                        return relate(geoA, geoB, finalRelation);
                                    }else{
                                        return false;
                                    }
                                }
                        ).forEach(x -> finalM.add(x.getUri(), uri, 1.0));
            }
        }
        if (numThreads > 1) {
            exec.shutdown();
            try {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (Map.Entry<String, Set<String>> entry : results.entrySet()) {
                String t = entry.getKey();
                for (String s : entry.getValue()) {
                    m.add(s, t, 1.0);
                }
            }
        }

        if (disjointStrategy) {
            AMapping disjoint = MappingFactory.createDefaultMapping();
            for (String s : sourceData.keySet()) {
                for (String t : targetData.keySet()) {
                    if (!m.contains(s, t)) {
                        disjoint.add(s, t, 1.0d);
                    }
                }
            }
            m = disjoint;
        }

        return m;
    }

    private static boolean relate(Geometry geometry1, Geometry geometry2, String relation) {
        switch (relation) {
            case EQUALS:
                return geometry1.equals(geometry2);
            case DISJOINT:
                return geometry1.disjoint(geometry2);
            case INTERSECTS:
                return geometry1.intersects(geometry2);
            case TOUCHES:
                return geometry1.touches(geometry2);
            case WITHIN:
                return geometry1.within(geometry2);
            case CONTAINS:
                return geometry1.contains(geometry2);
            case COVERS:
                return geometry1.covers(geometry2);
            case COVEREDBY:
                return geometry1.coveredBy(geometry2);
            case OVERLAPS:
                return geometry1.overlaps(geometry2);
            default:
                return geometry1.relate(geometry2, relation);
        }
    }


}
