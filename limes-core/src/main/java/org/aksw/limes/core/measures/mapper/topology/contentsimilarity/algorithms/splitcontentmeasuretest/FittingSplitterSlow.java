package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.splitcontentmeasuretest;

import org.locationtech.jts.geom.*;

public class FittingSplitterSlow implements Splitter{

    private GeometryFactory factory = new GeometryFactory();

    private Geometry emptyGeo;
    private Geometry[][] empty2x2Geo;

    public FittingSplitterSlow() {
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
            Geometry[][] temp = new Geometry[splitGeo.length * 2][];
            for (int i = 0; i < splitGeo.length; i++) {
                temp[i * 2] = new Geometry[splitGeo[i].length * 2];
                temp[i * 2 + 1] = new Geometry[splitGeo[i].length * 2];

                for (int j = 0; j < splitGeo[i].length; j++) {
                    Geometry[][] partSplit = getSplitGeo(splitGeo[i][j]);
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
    private Geometry[][] getSplitGeo(Geometry geo) {
        if (geo.isEmpty()) {
            return empty2x2Geo;
        }
        Envelope envelope = geo.getEnvelopeInternal();
        double midX = envelope.getMinX() + (envelope.getMaxX() - envelope.getMinX()) / 2;
        double midY = envelope.getMinY() + (envelope.getMaxY() - envelope.getMinY()) / 2;

        Envelope left = new Envelope(envelope.getMinX(), midX, envelope.getMinY(), envelope.getMaxY());
        Envelope right = new Envelope(midX, envelope.getMaxX(), envelope.getMinY(), envelope.getMaxY());
        
        Envelope bottomLeft = new Envelope(envelope.getMinX(), midX, envelope.getMinY(), midY);
        Envelope bottomRight = new Envelope(midX, envelope.getMaxX(), envelope.getMinY(), midY);
        Envelope topLeft = new Envelope(envelope.getMinX(), midX, midY, envelope.getMaxY());
        Envelope topRight = new Envelope(midX, envelope.getMaxX(), midY, envelope.getMaxY());

        try{
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
