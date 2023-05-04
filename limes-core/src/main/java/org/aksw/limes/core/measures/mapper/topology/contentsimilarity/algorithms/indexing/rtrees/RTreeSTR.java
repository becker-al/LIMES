package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RTreeSTR extends AbstractRTree {


    public RTreeSTR(int capacity) {
        super(capacity);
    }

    public RTreeSTR() {
    }

    public AbstractRTree buildSTR(List<Entry> entries) {
        List<AbstractRTree> bottomLayerNodes = entries.stream().map(AbstractRTree::createStaticLeaf).collect(Collectors.toList());
        return buildSTRRec(bottomLayerNodes, capacity);
    }

    private static AbstractRTree buildSTRRec(List<AbstractRTree> entries, int capacity) {
        int requiredNodeAmount = (int) Math.ceil((0.0 + entries.size()) / capacity);

        if (requiredNodeAmount == 0) {
            return createStaticLeaf(null); //Empty RTree
        }
        if (requiredNodeAmount == 1) {
            return createStaticParent(entries, capacity); //Create the root
        }

        int sliceAmount = (int) Math.ceil(Math.sqrt(requiredNodeAmount));
        int entriesPerSlice = (int) Math.ceil((0.0 + entries.size()) / sliceAmount);
        int nodesPerSlice = (int) Math.ceil((0.0 + entriesPerSlice) / capacity);

        List<AbstractRTree> nodes = new ArrayList<>(requiredNodeAmount);

        entries.sort(Comparator.comparingDouble(o -> o.boundary.getMinX() + o.boundary.getMaxX()));
        for (int i = 0; i < sliceAmount; i++) {
            List<AbstractRTree> sliceEntries = entries.subList(i * entriesPerSlice, Math.min((i + 1) * entriesPerSlice, entries.size()));
            sliceEntries.sort(Comparator.comparingDouble(o -> o.boundary.getMinY() + o.boundary.getMaxY()));
            for (int j = 0; j < nodesPerSlice; j++) {
                List<AbstractRTree> nodeEntries = sliceEntries.subList(Math.min(j * capacity, sliceEntries.size()), Math.min((j + 1) * capacity, sliceEntries.size()));
                if (!nodeEntries.isEmpty()) {
                    nodes.add(createStaticParent(nodeEntries, capacity));
                }
            }
        }
        return buildSTRRec(nodes, capacity);
    }

    @Override
    public void build(List<Entry> entries) {
        AbstractRTree rTree = buildSTR(entries);
        takeValues(rTree);
    }
}

