package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.ContentMeasure;
import org.locationtech.jts.geom.Envelope;

public class FMMatcher implements Matcher {

    public boolean relate(Envelope mbrA, Envelope mbrB, String relation) {
        double X = ContentMeasure.fM(mbrA, mbrB);
        double Y = ContentMeasure.fM(mbrB, mbrA);
        double Z = X + Y;

        return relate(X, Y, Z, relation);
    }

    @Override
    public String getName() {
        return "ContentMixed";
    }


    public boolean relate(double X, double Y, double Z, String relation) {
        switch (relation) {
            case EQUALS:
                if (X == -1 && Y == -1) {
                    return true;
                } else {
                    return false;
                }
            case DISJOINT:
                if (1 < X && 1 < Y) {
                    return true;
                } else {
                    return false;
                }
            case INTERSECTS:
                if (relate(X, Y, Z, EQUALS) || relate(X, Y, Z, TOUCHES) || relate(X, Y, Z, CONTAINS)
                        || relate(X, Y, Z, COVERS) || relate(X, Y, Z, COVEREDBY) || relate(X, Y, Z, WITHIN)
                        || relate(X, Y, Z, OVERLAPS)
                ) {
                    return true;
                } else {
                    return false;
                }
            case TOUCHES: //meet
                if (X == 1 && Y == 1) {
                    return true;
                } else {
                    return false;
                }

            case CONTAINS:
            case COVERS:
                return (Math.abs(X) < 1 && Y == -1) || (Math.abs(X) < 1 && Y < -1) || relate(X, Y, Z, EQUALS);

            case WITHIN:
            case COVEREDBY:
                return (X < -1 && Math.abs(Y) < 1) || (X == -1 && Math.abs(Y) < 1) || relate(X, Y, Z, EQUALS);

            case OVERLAPS:
                if (Math.abs(X) < 1 && Math.abs(Y) < 1) {
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

}
