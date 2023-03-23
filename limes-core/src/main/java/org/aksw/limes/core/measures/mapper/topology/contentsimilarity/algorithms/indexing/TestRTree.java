package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSTR;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;

public class TestRTree {

    int count = 0;

    public static void main(String[] args) {
        new TestRTree();
    }

    public TestRTree() {
        List<RTreeSTR.Entry> entries = new ArrayList<>(List.of(
                rec(136, 152, 195, 237),
                rec(205, 220, 350, 400),
                rec(130, 285, 254, 580),
                rec(290, 332, 440, 505),

                rec(600, 120, 700, 270),
                rec(640, 185, 820, 340),
                rec(550, 280, 820, 360),
                rec(685, 325, 770, 550),

                rec(560, 635, 670, 770),
                rec(630, 701, 760, 800),
                rec(705, 655, 740, 690),
                rec(770, 670, 970, 970),

                rec(415, 570, 480, 650),
                rec(310, 630, 440, 740),
                rec(260, 705, 390, 830),
                rec(125, 780, 450, 870)
        ));
        RTree rTree = new RTreeSTR();
        rTree.build(entries);
        System.out.println("a");

    }

    RTreeSTR.Entry rec(int minX, int minY, int maxX, int maxY){
        return new RTreeSTR.Entry(count++ + "", new Envelope(minX, maxX, minY, maxY), null);
    }

}
