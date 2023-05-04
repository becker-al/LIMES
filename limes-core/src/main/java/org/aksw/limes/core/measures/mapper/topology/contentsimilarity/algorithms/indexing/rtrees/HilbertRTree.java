package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees;

import org.aksw.limes.core.exceptions.NotYetImplementedException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.*;

public class HilbertRTree extends AbstractRTree {

    public HilbertRTree() {
    }

    public HilbertRTree(int capacity) {
        super(capacity);
    }

    @Override
    public void build(List<Entry> entries) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;

        for (Entry entry : entries) {
            if (entry.getEnvelope().getMinX() < minX) {
                minX = entry.getEnvelope().getMinX();
            }
            if (entry.getEnvelope().getMinY() < minY) {
                minY = entry.getEnvelope().getMinY();
            }
        }

        int precision = (int) Math.pow(10, 6);
        int r = 29;
        double shiftX = -minX;
        double shiftY = -minY;

        Map<Integer, List<Entry>> map = new TreeMap<>(Integer::compareTo);
        for (Entry entry : entries) {
            int x = (int) ((entry.getEnvelope().getMinX() + shiftX) * precision);
            int y = (int) ((entry.getEnvelope().getMinY() + shiftY) * precision);
            int hilbert = encode(x, y, r);
            if (!map.containsKey(hilbert)) {
                map.put(hilbert, new ArrayList<>());
            }
            map.get(hilbert).add(entry);
        }
        List<AbstractRTree> rTrees = new ArrayList<>();
        for (Map.Entry<Integer, List<Entry>> hilbertEntry : map.entrySet()) {
            for (Entry entry : hilbertEntry.getValue()) {
                rTrees.add(createStaticLeaf(entry));
            }
        }

        List<AbstractRTree> upperLayer = new ArrayList<>();
        while (rTrees.size() != 1) {
            while (!rTrees.isEmpty()) {
                List<AbstractRTree> current = new ArrayList<>();
                for (int i = 0; i < capacity; i++) {
                    if (rTrees.size() > 0) {
                        AbstractRTree remove = rTrees.remove(0);
                        current.add(remove);
                    }
                }
                AbstractRTree parent = createStaticParent(current, capacity);
                upperLayer.add(parent);
            }
            rTrees = upperLayer;
            upperLayer = new ArrayList<>();
        }

        takeValues(rTrees.get(0));
    }

    public static int encode(int x, int y, int r) {
        int mask = (1 << r) - 1;
        int hodd = 0;
        int heven = x ^ y;
        int notx = ~x & mask;
        int noty = ~y & mask;
        int temp = notx ^ y;
        int v0 = 0, v1 = 0;
        for (int k = 1; k < r; k++) {
            v1 = ((v1 & heven) | ((v0 ^ noty) & temp)) >> 1;
            v0 = ((v0 & (v1 ^ notx)) | (~v0 & (v1 ^ noty))) >> 1;
        }
        hodd = (~v0 & (v1 ^ x)) | (v0 & (v1 ^ noty));
        return interleaveBits(hodd, heven);
    }

    private static int interleaveBits(int odd, int even) {
        int val = 0;
        int max = Math.max(odd, even);
        int n = 0;
        while (max > 0) {
            n++;
            max >>= 1;
        }
        for (int i = 0; i < n; i++) {
            int bitMask = 1 << i;
            int a = (even & bitMask) > 0 ? (1 << (2 * i)) : 0;
            int b = (odd & bitMask) > 0 ? (1 << (2 * i + 1)) : 0;
            val += a + b;
        }
        return val;
    }

}
