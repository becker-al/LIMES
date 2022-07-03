package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater;

import org.locationtech.jts.geom.Envelope;

public class MbbOptimalRelater implements Relater {
    @Override
    public boolean relate(Envelope mbrA, Envelope mbrB, String relation) {
        switch (relation) {
            case EQUALS:
                return mbrA.equals(mbrB);
            case DISJOINT:
                return !mbrA.intersects(mbrB);
            case INTERSECTS:
                return mbrA.intersects(mbrB);
            case TOUCHES:
                return mbrA.getMaxX() == mbrB.getMinX() || mbrA.getMinX() == mbrB.getMaxX() || mbrA.getMaxY() == mbrB.getMinY() || mbrA.getMinY() == mbrB.getMaxY();
            case WITHIN:
                return mbrB.contains(mbrA);
            case CONTAINS:
                return mbrA.contains(mbrB);
            case COVERS:
                return mbrA.covers(mbrB);
            case COVEREDBY:
                return mbrB.covers(mbrA);
            case OVERLAPS:
                return mbrA.overlaps(mbrB);
        }
        throw new RuntimeException();
    }

    @Override
    public String getName() {
        return "Optimal";
    }
}
