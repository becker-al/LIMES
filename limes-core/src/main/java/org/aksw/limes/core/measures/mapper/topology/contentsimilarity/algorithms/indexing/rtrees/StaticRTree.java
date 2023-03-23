package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees;

import java.util.List;

public class StaticRTree extends AbstractRTree {
    public StaticRTree(int capacity) {
        super(capacity);
    }

    @Override
    public void build(List<Entry> entries) {
        throw new RuntimeException();
    }
}
