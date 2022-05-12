package org.aksw.limes.core.measures.mapper.topology.contentsimilarity;

import org.locationtech.jts.geom.Envelope;

public class ContentMeasure {

    private static double area(Envelope mbr) {
        return mbr.getArea();
    }

    private static double diagonal(Envelope mbr) {
        return Math.sqrt(mbr.getHeight() * mbr.getHeight() + mbr.getWidth() * mbr.getWidth());
    }

    //Creates the union mbr of two mbrs
    private static Envelope union(Envelope mbrA, Envelope mbrB) {
        return new Envelope(
                Math.min(mbrA.getMinX(), mbrB.getMinX()),
                Math.max(mbrA.getMaxX(), mbrB.getMaxX()),
                Math.min(mbrA.getMinY(), mbrB.getMinY()),
                Math.max(mbrA.getMaxY(), mbrB.getMaxY())
        );
    }

    //Creates the intersection mbr of two non disjoint mbrs
    private static Envelope intersection(Envelope mbrA, Envelope mbrB) {
        return mbrA.intersection(mbrB);
    }

    //Checks if the two envelopes overlap on the x axis
    private static boolean projectionX(Envelope mbrA, Envelope mbrB) {
        if (mbrA.getMinX() > mbrB.getMaxX()) {
            return false;
        } else if (mbrA.getMaxX() < mbrB.getMinX()) {
            return false;
        } else {
            return true;
        }
    }

    //Checks if the two envelopes overlap on the y axis
    private static boolean projectionY(Envelope mbrA, Envelope mbrB) {
        if (mbrA.getMinY() > mbrB.getMaxY()) {
            return false;
        } else if (mbrA.getMaxY() < mbrB.getMinY()) {
            return false;
        } else {
            return true;
        }
    }

    //Computes the distance between two mbrs
    private static double distance(Envelope mbrA, Envelope mbrB) {
        if (!projectionX(mbrA, mbrB) && !projectionY(mbrA, mbrB)) {
            return Math.sqrt(
                    Math.pow(
                            Math.min(
                                    Math.abs(mbrA.getMinX() - mbrB.getMaxX()),
                                    Math.abs(mbrA.getMaxX() - mbrB.getMinX())
                            ), 2
                    ) + Math.pow(
                            Math.min(
                                    Math.abs(mbrA.getMinY() - mbrB.getMaxY()),
                                    Math.abs(mbrA.getMaxY() - mbrB.getMinY())
                            ), 2
                    )
            );
        } else if (union(mbrA, mbrB).getArea() == Math.max(mbrA.getArea(), mbrB.getArea())) {
            return Math.min(
                    Math.min(
                            Math.abs(mbrA.getMinX() - mbrB.getMinX()),
                            Math.abs(mbrA.getMaxX() - mbrB.getMaxX())
                    ),
                    Math.min(
                            Math.abs(mbrA.getMinY() - mbrB.getMinY()),
                            Math.abs(mbrA.getMaxY() - mbrB.getMaxY())
                    )
            );
        } else if (projectionX(mbrA, mbrB) && !projectionY(mbrA, mbrB)) {
            return Math.min(
                    Math.abs(mbrA.getMinY() - mbrB.getMaxY()),
                    Math.abs(mbrA.getMaxY() - mbrB.getMinY())
            );
        } else if (!projectionX(mbrA, mbrB) && projectionY(mbrA, mbrB)) {
            return Math.min(
                    Math.abs(mbrA.getMinX() - mbrB.getMaxX()),
                    Math.abs(mbrA.getMaxX() - mbrB.getMinX())
            );
        } else {
            return 0.0;
        }
    }

    //Computes the area based content measure Fa
    public static double fA(Envelope mbrA, Envelope mbrB){
        return area(mbrA) / (area(union(mbrA, mbrB)));
    }

    //Computes the diagonal based content measure Fd
    public static double fD(Envelope mbrA, Envelope mbrB){
        return diagonal(mbrA) / (diagonal(union(mbrA, mbrB)));
    }

    //Computes the mixed content measure Fm
    public static double fM(Envelope mbrA, Envelope mbrB){
        return ((area(mbrA) - 2 * area(intersection(mbrA, mbrB))) / area(mbrA))
                - (distance(mbrA, mbrB) / diagonal(mbrA));
    }

}
