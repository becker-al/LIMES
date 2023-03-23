package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing;

import org.aksw.commons.util.Pair;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSTR;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.Matcher;
import org.locationtech.jts.geom.Envelope;

import java.util.*;

/**
 * Some parts of this class regarding the disjoint strategy are taken from RADON / kdressler
 *
 * @see org.aksw.limes.core.measures.mapper.topology.RADON
 */
public class RTreeIndexingMatchTwoTrees implements Indexing {


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


        boolean disjointStrategy = relation.equals(DISJOINT);
        if (disjointStrategy)
            relation = INTERSECTS;

        RTree sourceTree = new RTreeSTR();
        sourceTree.build(sourceEntries);
        RTree targetTree = new RTreeSTR();
        targetTree.build(targetEntries);

        AMapping m = MappingFactory.createDefaultMapping();

        List<Pair<RTree.Entry, RTree.Entry>> pairs = match(sourceTree, targetTree, new ArrayList<>());
        for (Pair<RTree.Entry, RTree.Entry> pair : pairs) {
            if (relater.relate(pair.getKey().getEnvelope(), pair.getValue().getEnvelope(), relation)) {
                m.add(pair.getKey().getUri(), pair.getValue().getUri(), 1.0);
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
        return "RTREE2";
    }
}
