package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RTreeNearestX extends AbstractRTree {


    public RTreeNearestX() {
    }

    public RTreeNearestX(int capacity) {
        super(capacity);
    }

    public AbstractRTree buildNearestX(List<Entry> entries) {
        List<AbstractRTree> bottomLayerNodes = entries.stream().map(AbstractRTree::createStaticLeaf)
                .sorted(Comparator.comparingDouble(o -> o.getContent().getEnvelope().centre().getX()))
                .collect(Collectors.toList());
        return buildNearestX(bottomLayerNodes, capacity);
    }

    private AbstractRTree buildNearestX(List<AbstractRTree> entries, int capacity) {
        if(capacity >= entries.size()){
            return createStaticParent(entries, capacity);
        }
        if(entries.size() == 0){
            return new RTreeNearestX();
        }

        //When we have more entries than capacity
        int splitSize = (entries.size() / capacity) + 1;

        List<AbstractRTree> children = new ArrayList<>(capacity);

        for (int i = 0; i < capacity; i++) {
            List<AbstractRTree> split = new ArrayList<>();
            for (int j = 0; j < splitSize; j++) {
                int index = i * splitSize + j;
                if(index < entries.size()){
                    split.add(entries.get(index));
                }
            }
            children.add(buildNearestX(split, capacity));
        }
        return createStaticParent(children,capacity);
    }

    @Override
    public void build(List<Entry> entries) {
        AbstractRTree rTree = buildNearestX(entries);
        takeValues(rTree);
    }
}

