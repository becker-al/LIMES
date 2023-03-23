package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.splitcontentmeasuretest;

import org.locationtech.jts.geom.*;

public class EqualSplitterSlow implements Splitter {

    private GeometryFactory factory = new GeometryFactory();

    private Geometry emptyGeo;
    private Geometry[][] empty2x2Geo;

    public EqualSplitterSlow() {
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
    public Envelope[][] getSplit(Geometry geo, int times) {
        Geometry[][] splitGeo = new Geometry[][]{
                new Geometry[]{
                        geo
                }
        };
        Envelope envelope = geo.getEnvelopeInternal();
        double startX = envelope.getMinX();
        double endX = envelope.getMaxX();
        double diffX = endX - startX;

        double startY = envelope.getMinY();
        double endY = envelope.getMaxY();
        double diffY = endY - startY;

        for (int splitIteration = 1; splitIteration <= times; splitIteration++) {
            double stepX = diffX / Math.pow(2, splitIteration);
            double stepY = diffY / Math.pow(2, splitIteration);

            Geometry[][] temp = new Geometry[splitGeo.length * 2][];
            for (int i = 0; i < splitGeo.length; i++) {
                temp[i * 2] = new Geometry[splitGeo[i].length * 2];
                temp[i * 2 + 1] = new Geometry[splitGeo[i].length * 2];

                for (int j = 0; j < splitGeo[i].length; j++) {
                    Geometry[][] partSplit = getSplitGeo(splitGeo[i][j], startX, stepX, endX, startY, stepY, endY);
                    temp[i * 2][j * 2] = partSplit[0][0];
                    temp[i * 2 + 1][j * 2] = partSplit[1][0];
                    temp[i * 2][j * 2 + 1] = partSplit[0][1];
                    temp[i * 2 + 1][j * 2 + 1] = partSplit[1][1];
                }

            }
            splitGeo = temp;
        }

        Envelope[][] split = new Envelope[splitGeo.length][];
        for (int i = 0; i < splitGeo.length; i++) {
            split[i] = new Envelope[splitGeo[i].length];
            for (int j = 0; j < splitGeo[i].length; j++) {
                Envelope splitEnvelope = splitGeo[i][j].getEnvelopeInternal();
                if(splitEnvelope.getArea() == 0){//If area = 0, this is not a polygon and cannot be used with cobalt
                    splitEnvelope = new Envelope();
                }
                split[i][j] = splitEnvelope;
            }
        }
        return split;
    }

    //TODO: Splitting is uneven. It should instead be a grid pattern, but at the moment it is not
    public Geometry[][] getSplitGeo(Geometry geo, double startX, double stepX, double endX, double startY, double stepY, double endY) {
        if (geo.isEmpty()) {
            return empty2x2Geo;
        }

        Envelope left = new Envelope(startX, startX + stepX, startY, endY);
        Envelope right = new Envelope(startX + stepX, endX, startY, endY);

        Envelope bottomLeft = new Envelope(startX, startX + stepX, startY, startY + stepY);
        Envelope bottomRight = new Envelope(startX + stepX, endX, startY, startY + stepY);
        Envelope topLeft = new Envelope(startX, startX + stepX, startY + stepY, endY);
        Envelope topRight = new Envelope(startX + stepX, endX, startY + stepY, endY);

        try {
            Geometry geoLeft = geo.intersection(factory.createPolygon(new Coordinate[]{
                    new Coordinate(left.getMinX(), left.getMinY()),
                    new Coordinate(left.getMinX(), left.getMaxY()),
                    new Coordinate(left.getMaxX(), left.getMaxY()),
                    new Coordinate(left.getMaxX(), left.getMinY()),
                    new Coordinate(left.getMinX(), left.getMinY())
            }));
            Geometry geoRight = geo.intersection(factory.createPolygon(new Coordinate[]{
                    new Coordinate(right.getMinX(), right.getMinY()),
                    new Coordinate(right.getMinX(), right.getMaxY()),
                    new Coordinate(right.getMaxX(), right.getMaxY()),
                    new Coordinate(right.getMaxX(), right.getMinY()),
                    new Coordinate(right.getMinX(), right.getMinY())
            }));
            Geometry geoBottomLeft = geoLeft.intersection(factory.createPolygon(new Coordinate[]{
                    new Coordinate(bottomLeft.getMinX(), bottomLeft.getMinY()),
                    new Coordinate(bottomLeft.getMinX(), bottomLeft.getMaxY()),
                    new Coordinate(bottomLeft.getMaxX(), bottomLeft.getMaxY()),
                    new Coordinate(bottomLeft.getMaxX(), bottomLeft.getMinY()),
                    new Coordinate(bottomLeft.getMinX(), bottomLeft.getMinY())
            }));
            Geometry geoTopLeft = geoLeft.intersection(factory.createPolygon(new Coordinate[]{
                    new Coordinate(topLeft.getMinX(), topLeft.getMinY()),
                    new Coordinate(topLeft.getMinX(), topLeft.getMaxY()),
                    new Coordinate(topLeft.getMaxX(), topLeft.getMaxY()),
                    new Coordinate(topLeft.getMaxX(), topLeft.getMinY()),
                    new Coordinate(topLeft.getMinX(), topLeft.getMinY())
            }));
            Geometry geoBottomRight = geoRight.intersection(factory.createPolygon(new Coordinate[]{
                    new Coordinate(bottomRight.getMinX(), bottomRight.getMinY()),
                    new Coordinate(bottomRight.getMinX(), bottomRight.getMaxY()),
                    new Coordinate(bottomRight.getMaxX(), bottomRight.getMaxY()),
                    new Coordinate(bottomRight.getMaxX(), bottomRight.getMinY()),
                    new Coordinate(bottomRight.getMinX(), bottomRight.getMinY())
            }));
            Geometry geoTopRight = geoRight.intersection(factory.createPolygon(new Coordinate[]{
                    new Coordinate(topRight.getMinX(), topRight.getMinY()),
                    new Coordinate(topRight.getMinX(), topRight.getMaxY()),
                    new Coordinate(topRight.getMaxX(), topRight.getMaxY()),
                    new Coordinate(topRight.getMaxX(), topRight.getMinY()),
                    new Coordinate(topRight.getMinX(), topRight.getMinY())
            }));
            Geometry[][] split = new Geometry[][]{
                    new Geometry[]{
                            geoBottomLeft,
                            geoTopLeft
                    },
                    new Geometry[]{
                            geoBottomRight,
                            geoTopRight
                    }
            };
            return split;
        }catch (TopologyException e){
            //TODO some geos have non-noded intersections between them and the left and right part of their envelope
            return empty2x2Geo;
        }
    }
}
