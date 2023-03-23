package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
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
public class RTreeIndexing implements Indexing {


    @Override
    public AMapping getMapping(Matcher relater, Map<String, Envelope> sourceData, Map<String, Envelope> targetData, String relation, int numThreads) {
        List<RTreeSTR.Entry> entries = new ArrayList<>(sourceData.size());
        sourceData.forEach((s, geometry) -> {
            entries.add(new RTreeSTR.Entry(s, geometry, null));
        });

        boolean disjointStrategy = relation.equals(DISJOINT);
        if (disjointStrategy)
            relation = INTERSECTS;

        RTree rTree = new RTreeSTR();
        rTree.build(entries);

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

                exec.submit(() -> {
                    List<RTreeSTR.Entry> search = rTree.search(envelope);
                    search.stream()
                            .filter(x -> {
                                        Envelope abb = x.getEnvelope();
                                        Envelope bbb = envelope;
                                        return relater.relate(abb, bbb, finalRelation);
                                    }
                            ).forEach(x -> value.add(x.getUri()));
                });
            } else {
                String finalRelation = relation;
                AMapping finalM = m;
                List<RTreeSTR.Entry> search = rTree.search(envelope);

                search.stream()
                        .filter(x -> {
                                    Envelope abb = x.getEnvelope();
                                    Envelope bbb = envelope;
                                    return relater.relate(abb, bbb, finalRelation);
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

    @Override
    public String getName() {
        return "RTREE";
    }
}
