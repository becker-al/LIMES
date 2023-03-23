package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.rstar;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.Indexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSTR;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.Matcher;
import org.locationtech.jts.geom.Envelope;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Some parts of this class regarding the disjoint strategy are taken from RADON / kdressler
 * @see org.aksw.limes.core.measures.mapper.topology.RADON
 */
public class RStarTreeIndexing implements Indexing {

    int capacity;

    public RStarTreeIndexing(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public AMapping getMapping(Matcher relater, Map<String, Envelope> sourceData, Map<String, Envelope> targetData, String relation, int numThreads) {
        RTree<String, Rectangle> tree = RTree.star().maxChildren(capacity).create();
        for (Map.Entry<String, Envelope> entry : sourceData.entrySet()) {
            String s = entry.getKey();
            Envelope geometry = entry.getValue();
            tree = tree.add(s, Geometries.rectangle(geometry.getMinX(), geometry.getMinY(), geometry.getMaxX(), geometry.getMaxY()));
        }

        boolean disjointStrategy = relation.equals(DISJOINT);
        if (disjointStrategy)
            relation = INTERSECTS;

        AMapping m = MappingFactory.createDefaultMapping();

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        Map<String, Set<String>> results = new HashMap<>(); //Target -> Source Mappings

        for (Map.Entry<String, Envelope> entry : targetData.entrySet()) {
            String uri = entry.getKey();
            Envelope envelope = entry.getValue();

            if (numThreads > 1) {
                HashSet<String> value = new HashSet<>();
                results.put(uri, value);
                String finalRelation = relation;

                RTree<String, Rectangle> finalTree = tree;
                exec.submit(() -> {
                    finalTree.search(Geometries.rectangle(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()))
                            .filter(x -> {
                                        Envelope abb = new Envelope(x.geometry().x1(), x.geometry().x2(), x.geometry().y1(), x.geometry().y2());
                                        Envelope bbb = envelope;
                                        return relater.relate(abb, bbb, finalRelation);
                                    }
                            ).forEach(x -> value.add(x.value()));
                });
            } else {
                String finalRelation = relation;
                AMapping finalM = m;
                tree.search(Geometries.rectangle(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()))
                        .filter(x -> {
                                    Envelope abb = new Envelope(x.geometry().x1(), x.geometry().x2(), x.geometry().y1(), x.geometry().y2());
                                    Envelope bbb = envelope;
                                    return relater.relate(abb, bbb, finalRelation);
                                }
                        ).forEach(x -> finalM.add(x.value(), uri, 1.0));
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

    @Override
    public String getName() {
        return "RTREE";
    }
}
