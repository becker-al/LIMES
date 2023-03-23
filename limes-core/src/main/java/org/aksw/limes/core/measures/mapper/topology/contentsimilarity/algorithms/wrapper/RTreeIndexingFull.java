package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.RTree;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSTR;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RTreeIndexingFull implements IndexingFull {

    @Override
    public AMapping getMapping(MatcherFull relater, Map<String, Geometry> sourceData, Map<String, Geometry> targetData, String relation, int numThreads) {
        List<RTreeSTR.Entry> entries = new ArrayList<>(sourceData.size());
        sourceData.forEach((s, geometry) -> {
            entries.add(new RTreeSTR.Entry(s, geometry.getEnvelopeInternal(), geometry));
        });

        boolean disjointStrategy = relation.equals(DISJOINT);
        if (disjointStrategy)
            relation = INTERSECTS;

        RTree rTree = new RTreeSTR();
        rTree.build(entries);


        AMapping m = MappingFactory.createDefaultMapping();

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        Map<String, Set<String>> results = new HashMap<>(); //Target -> Source Mappings

        for (Map.Entry<String, Geometry> entry : targetData.entrySet()) {
            String uri = entry.getKey();
            Geometry geoEntry = entry.getValue();
            Envelope envelope = geoEntry.getEnvelopeInternal();

            if (numThreads > 1) {
                HashSet<String> value = new HashSet<>();
                results.put(uri, value);
                String finalRelation = relation;

                exec.submit(() -> {
                    List<RTreeSTR.Entry> search = rTree.search(envelope);
                    search.stream()
                            .filter(x -> relater.relate(x.getUri(), x.getGeometry(), uri, geoEntry, finalRelation)
                            ).forEach(x -> value.add(x.getUri()));
                });
            } else {
                String finalRelation = relation;
                AMapping finalM = m;
                List<RTreeSTR.Entry> search = rTree.search(envelope);

                search.stream()
                        .filter(x -> relater.relate(x.getUri(), x.getGeometry(), uri, geoEntry, finalRelation)
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

    @Override
    public String getName() {
        return "RTREE-FULL";
    }

}