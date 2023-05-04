package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSTR;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.Matcher;
import org.locationtech.jts.geom.Envelope;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AbstractRTreeIndexing implements Indexing {

    private Callable<RTree> treeBuilder;

    public AbstractRTreeIndexing(Callable<RTree> treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    @Override
    public AMapping getMapping(Matcher relater, Map<String, Envelope> sourceData, Map<String, Envelope> targetData, String relation, int numThreads) {
        List<RTree.Entry> entries = new ArrayList<>(sourceData.size());
        sourceData.forEach((s, geometry) -> {
            entries.add(new RTree.Entry(s, geometry, null));
        });

        AMapping m = MappingFactory.createDefaultMapping();
        RTree rTree = null;
        try {
            rTree = treeBuilder.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        rTree.build(entries);

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        Map<String, Set<String>> results = new HashMap<>(); //Target -> Source Mappings

        for (Map.Entry<String, Envelope> entry : targetData.entrySet()) {
            String uri = entry.getKey();
            Envelope envelope = entry.getValue();

            if (numThreads > 1) {
                HashSet<String> value = new HashSet<>();
                results.put(uri, value);

                RTree finalRTree = rTree;
                exec.submit(() -> {
                    List<RTree.Entry> search = finalRTree.search(envelope);
                    search.stream()
                            .filter(x -> {
                                        Envelope abb = x.getEnvelope();
                                        Envelope bbb = envelope;
                                        return relater.relate(abb, bbb, relation);
                                    }
                            ).forEach(x -> value.add(x.getUri()));
                    if (relation.equals(DISJOINT)) {
                        value.addAll(finalRTree.searchExcept(envelope).stream().map(RTree.Entry::getUri).collect(Collectors.toList()));
                    }
                });
            } else {
                List<RTree.Entry> search = rTree.search(envelope);

                search.stream()
                        .filter(x -> {
                                    Envelope abb = x.getEnvelope();
                                    Envelope bbb = envelope;
                                    return relater.relate(abb, bbb, relation);
                                }
                        ).forEach(x -> m.add(x.getUri(), uri, 1.0));
                if (relation.equals(DISJOINT)) {
                    rTree.searchExcept(envelope).stream().map(RTree.Entry::getUri).forEach(sourceUri -> m.add(sourceUri, uri, 1.0));
                }
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

        return m;
    }

    @Override
    public String getName() {
        return "RTREE";
    }
}
