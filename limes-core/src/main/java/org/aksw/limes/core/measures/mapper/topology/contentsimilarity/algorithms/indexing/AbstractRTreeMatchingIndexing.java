package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing;

import org.aksw.commons.util.Pair;
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

public class AbstractRTreeMatchingIndexing implements Indexing{

    private Callable<RTree> treeBuilder;

    public AbstractRTreeMatchingIndexing(Callable <RTree> treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    @Override
    public AMapping getMapping(Matcher relater, Map<String, Envelope> sourceData, Map<String, Envelope> targetData, String relation, int numThreads) {
        List<RTreeSTR.Entry> sourceEntries = new ArrayList<>(sourceData.size());
        sourceData.forEach((s, geometry) -> {
            sourceEntries.add(new RTreeSTR.Entry(s, geometry, null));
        });

        List<RTreeSTR.Entry> targetEntries = new ArrayList<>(targetData.size());
        targetData.forEach((s, geometry) -> {
            targetEntries.add(new RTreeSTR.Entry(s, geometry, null));
        });

        AMapping m = MappingFactory.createDefaultMapping();
        RTree sourceTree = null;
        RTree targetTree = null;
        try {
            sourceTree = treeBuilder.call();
            targetTree = treeBuilder.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sourceTree.build(sourceEntries);
        targetTree.build(targetEntries);

        List<Pair<RTree.Entry, RTree.Entry>> pairs = match(sourceTree, targetTree, new ArrayList<>());
        for (Pair<RTree.Entry, RTree.Entry> pair : pairs) {
            if (relater.relate(pair.getKey().getEnvelope(), pair.getValue().getEnvelope(), relation)) {
                m.add(pair.getKey().getUri(), pair.getValue().getUri(), 1.0);
            }
        }

        if (relation.equals(DISJOINT)) {
            RTree finalSourceTree = sourceTree;
            targetData.forEach((s, envelope) -> {
                List<RTree.Entry> entries = finalSourceTree.searchExcept(envelope);
                for (RTree.Entry entry : entries) {
                    m.add(entry.uri, s, 1.0);
                }
            });
        }


        return m;
    }

    private List<Pair<RTree.Entry, RTree.Entry>> match(RTree sourceTree, RTree targetTree, List<Pair<RTree.Entry, RTree.Entry>> result) {
        //Split the bigger one
        boolean splitSource = sourceTree.getBoundary().getArea() > targetTree.getBoundary().getArea();
        if (splitSource) {
            for (RTree child : sourceTree.getChildren()) {
                if (child.getBoundary().intersects(targetTree.getBoundary())) {
                    if (child.isLeaf()) {
                        List<RTreeSTR.Entry> search = targetTree.search(child.getBoundary());
                        for (RTreeSTR.Entry tG : search) {
                            result.add(new Pair<>(child.getContent(), tG));
                        }
                    } else {
                        match(child, targetTree, result);
                    }
                }
            }
        } else {
            for (RTree child : targetTree.getChildren()) {
                if (child.getBoundary().intersects(sourceTree.getBoundary())) {
                    if (child.isLeaf()) {
                        List<RTreeSTR.Entry> search = sourceTree.search(child.getBoundary());
                        for (RTreeSTR.Entry sG : search) {
                            result.add(new Pair<>(sG, child.getContent()));
                        }
                    } else {
                        match(sourceTree, child, result);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "RTREE";
    }
}
