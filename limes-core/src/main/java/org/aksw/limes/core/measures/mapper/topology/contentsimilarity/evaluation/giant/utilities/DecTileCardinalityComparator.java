package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.utilities;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.datamodel.Tile;

import java.util.Comparator;

public class DecTileCardinalityComparator implements Comparator<Tile> {

    /* 
    * This comparator orders tiles in decreasing order of cardinality, i.e.,
    * from the smallest number of pairs to the largest one.
    *
    */
    @Override
    public int compare(Tile block1, Tile block2) {
        return Double.compare(block2.getNoOfPairs(), block1.getNoOfPairs());
    }
}
