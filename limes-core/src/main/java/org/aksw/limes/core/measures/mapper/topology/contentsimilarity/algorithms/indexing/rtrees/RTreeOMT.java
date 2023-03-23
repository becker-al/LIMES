package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RTreeOMT extends AbstractRTree {
//http://ftp.informatik.rwth-aachen.de/Publications/CEUR-WS/Vol-74/files/FORUM_18.pdf

    public RTreeOMT() {
    }

    public RTreeOMT(int capacity) {
        super(capacity);
    }

    public AbstractRTree buildOMT(List<Entry> entries) {
        List<AbstractRTree> bottomLayerNodes = entries.stream().map(AbstractRTree::createStaticLeaf)
                .collect(Collectors.toList());
        return buildOMT(bottomLayerNodes, capacity);
    }

    private AbstractRTree buildOMT(List<AbstractRTree> entries, int capacity) {
        if(entries.size() <= capacity){
            return createStaticParent(entries, capacity);
        }

        int treeHeight = (int) Math.ceil(Math.log(entries.size()) / Math.log(capacity));
        int numberOfNodesPerSubtree = (int) Math.pow(capacity, treeHeight - 1);
        int s = (int) Math.floor(Math.sqrt(Math.ceil((entries.size() + 0.0)/(numberOfNodesPerSubtree + 0.0))));

        List<AbstractRTree> children = new ArrayList<>();

        if(treeHeight % 2 == 0){
            //Split x
            entries.sort(Comparator.comparingDouble(o -> o.getBoundary().centre().getX()));
        }else{
            //split y
            entries.sort(Comparator.comparingDouble(o -> o.getBoundary().centre().getY()));
        }
        outer: for (int i = 0; i < (numberOfNodesPerSubtree); i++) {
            List<AbstractRTree> x = new ArrayList<>();
            for (int j = 0; j < numberOfNodesPerSubtree; j++) {
                int index = i * numberOfNodesPerSubtree + j;
                if(index < entries.size()){
                    x.add(entries.get(index));
                }else{
                    if(x.size() > 0){
                        children.add(buildOMT(x, capacity));
                    }
                    break outer;
                }
            }
            children.add(buildOMT(x, capacity));
        }


        return createStaticParent(children, capacity);

    }

    @Override
    public void build(List<Entry> entries) {
        AbstractRTree rTree = buildOMT(entries);
        takeValues(rTree);
    }
}

