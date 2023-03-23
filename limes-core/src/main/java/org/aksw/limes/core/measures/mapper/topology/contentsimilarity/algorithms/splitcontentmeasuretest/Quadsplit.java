package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.splitcontentmeasuretest;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.Matcher;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper.MatcherFull;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Quadsplit implements MatcherFull {

    private Map<String, Envelope[][]> splittedA = new ConcurrentHashMap<>();
    private Map<String, Envelope[][]> splittedB = new ConcurrentHashMap<>();
    private GeometryFactory factory = new GeometryFactory();
    private Geometry emptyGeo;
    private Geometry[][] empty2x2Geo;
    private int splitTimes;
    private Splitter splitter;
    private Matcher matcher;


    public Quadsplit(int splitTimes, Splitter splitter, Matcher matcher) {
        this.splitTimes = splitTimes;
        this.splitter = splitter;
        this.matcher = matcher;
        this.emptyGeo = factory.createPolygon();
        this.empty2x2Geo = new Geometry[][]{
                new Geometry[]{
                        emptyGeo, emptyGeo
                },
                new Geometry[]{
                        emptyGeo, emptyGeo
                }
        };
    }

    @Override
    public boolean relate(String uriA, Geometry geoA, String uriB, Geometry geoB, String relation) {
        Envelope[][] splitA;
        if (splittedA.containsKey(uriA)) {
            splitA = splittedA.get(uriA);
        } else {
            splitA = splitter.getSplit(geoA, splitTimes);
            splittedA.put(uriA, splitA);
        }

        Envelope[][] splitB;
        if (splittedB.containsKey(uriB)) {
            splitB = splittedB.get(uriB);
        } else {
            splitB = splitter.getSplit(geoB, splitTimes);
            splittedB.put(uriB, splitB);
        }


        switch (relation) {
            case EQUALS:
                for (int i = 0; i < splitA.length; i++) {
                    for (int j = 0; j < splitA[i].length; j++) {
                        Envelope eA = splitA[i][j];
                        Envelope eB = splitB[i][j];
                        if (!matcher.relate(eA, eB, EQUALS) && !(eA.isNull() && eB.isNull()) && !(eA.getArea() == 0 && eB.getArea() == 0)) {
                            return false;
                        }
                    }
                }
                return true;
            case INTERSECTS:
                for (int i1 = 0; i1 < splitA.length; i1++) {
                    for (int j1 = 0; j1 < splitA[i1].length; j1++) {
                        for (int i2 = 0; i2 < splitB.length; i2++) {
                            for (int j2 = 0; j2 < splitB[i2].length; j2++) {
                                Envelope eA = splitA[i1][j1];
                                Envelope eB = splitB[i2][j2];
                                if (!eA.isNull() && !eB.isNull() && matcher.relate(eA, eB, INTERSECTS)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;

            case TOUCHES: //meet
                boolean atLeastOneTouch = false;
                for (int i1 = 0; i1 < splitA.length; i1++) {
                    for (int j1 = 0; j1 < splitA[i1].length; j1++) {
                        for (int i2 = 0; i2 < splitB.length; i2++) {
                            for (int j2 = 0; j2 < splitB[i2].length; j2++) {
                                Envelope eA = splitA[i1][j1];
                                Envelope eB = splitB[i2][j2];
                                if (!eA.isNull() && !eB.isNull() && matcher.relate(eA, eB, TOUCHES)) {
                                    atLeastOneTouch = true;
                                } else {
                                    if (!eA.isNull() && !eB.isNull() && matcher.relate(eA, eB, INTERSECTS)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
                return atLeastOneTouch;
            case CONTAINS: //All points of B are in some part of A
            case COVERS:
                //TODO
                return relate(uriB, geoB, uriA, geoA, WITHIN);

            case WITHIN: //All points of A are in some part of B
            case COVEREDBY:
                for (int i1 = 0; i1 < splitA.length; i1++) {
                    for (int j1 = 0; j1 < splitA[i1].length; j1++) {
                        Envelope eA = splitA[i1][j1]; //This piece of a should be contained in the pieces of b it intersects
                        if (!eA.isNull() && !(eA.getArea() == 0)) {
                            ArrayList<Envelope> intersecting = new ArrayList<>();
                            for (int i2 = 0; i2 < splitB.length; i2++) {
                                for (int j2 = 0; j2 < splitB[i2].length; j2++) {
                                    Envelope eB = splitB[i2][j2];
                                    if (matcher.relate(eA, eB, INTERSECTS)) {
                                        intersecting.add(eB);
                                    }
                                }
                            }
                            if (intersecting.isEmpty()) {
                                return false;
                            }
                            double minX = Double.POSITIVE_INFINITY;
                            double minY = Double.POSITIVE_INFINITY;
                            double maxX = Double.NEGATIVE_INFINITY;
                            double maxY = Double.NEGATIVE_INFINITY;
                            for (Envelope envelope : intersecting) {
                                if (envelope.getMinX() < minX) {
                                    minX = envelope.getMinX();
                                }
                                if (envelope.getMinY() < minY) {
                                    minY = envelope.getMinY();
                                }
                                if (envelope.getMaxX() > maxX) {
                                    maxX = envelope.getMaxX();
                                }
                                if (envelope.getMaxY() > maxY) {
                                    maxY = envelope.getMaxY();
                                }
                            }
                            Envelope boundary = new Envelope(minX, maxX, minY, maxY);
                            if (!matcher.relate(eA, boundary, Matcher.WITHIN)) {
                                return false;
                            }
                        }
                    }
                }
                return true;

            case OVERLAPS:
                //Check if both of them have at least one tile which does not intersect the other
                boolean anyAOutsideB = checkAnyPartOfAOutsideAllPartsOfB(splitA, splitB);
                if (!anyAOutsideB) {
                    return false;
                }
                boolean anyBOutsideA = checkAnyPartOfAOutsideAllPartsOfB(splitB, splitA);
                if (!anyBOutsideA) {
                    return false;
                }
                //Check if they have any common point on the inside (intersects except touch)
                for (int i1 = 0; i1 < splitA.length; i1++) {
                    for (int j1 = 0; j1 < splitA[i1].length; j1++) {
                        for (int i2 = 0; i2 < splitB.length; i2++) {
                            for (int j2 = 0; j2 < splitB[i2].length; j2++) {
                                Envelope eA = splitA[i1][j1];
                                Envelope eB = splitB[i2][j2];
                                if (!eA.isNull() && !eB.isNull() &&
                                        matcher.relate(eA, eB, CONTAINS)
                                        || matcher.relate(eA, eB, WITHIN)
                                        || matcher.relate(eA, eB, OVERLAPS)
                                ) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            default:
                return false;
        }
    }

    private boolean checkAnyPartOfAOutsideAllPartsOfB(Envelope[][] splitA, Envelope[][] splitB) {
        for (int i1 = 0; i1 < splitA.length; i1++) {
            for (int j1 = 0; j1 < splitA[i1].length; j1++) {
                Envelope eA = splitA[i1][j1];
                if (!eA.isNull()) {
                    ArrayList<Envelope> intersecting = new ArrayList<>();
                    for (int i2 = 0; i2 < splitB.length; i2++) {
                        for (int j2 = 0; j2 < splitB[i2].length; j2++) {
                            Envelope eB = splitB[i2][j2];
                            if (matcher.relate(eA, eB, INTERSECTS)) {
                                intersecting.add(eB);
                            }
                        }
                    }
                    if (intersecting.isEmpty()) {
                        return true;
                    }
                    double minX = Double.POSITIVE_INFINITY;
                    double minY = Double.POSITIVE_INFINITY;
                    double maxX = Double.NEGATIVE_INFINITY;
                    double maxY = Double.NEGATIVE_INFINITY;
                    for (Envelope envelope : intersecting) {
                        if (envelope.getMinX() < minX) {
                            minX = envelope.getMinX();
                        }
                        if (envelope.getMinY() < minY) {
                            minY = envelope.getMinY();
                        }
                        if (envelope.getMaxX() > maxX) {
                            maxX = envelope.getMaxX();
                        }
                        if (envelope.getMaxY() > maxY) {
                            maxY = envelope.getMaxY();
                        }
                    }
                    Envelope boundary = new Envelope(minX, maxX, minY, maxY);
                    if (!matcher.relate(eA, boundary, Matcher.WITHIN)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "QuadsplitFA";
    }

    @Override
    public void reset() {
        splittedA = new ConcurrentHashMap<>();
        splittedB = new ConcurrentHashMap<>();
    }
}
