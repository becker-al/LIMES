package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RTree {

    public static class Entry {
        private String uri;
        private Envelope envelope;
        private Geometry geometry;

        public Entry(String uri, Envelope envelope, Geometry geometry) {
            this.uri = uri;
            this.envelope = envelope;
            this.geometry = geometry;
        }

        public String getUri() {
            return uri;
        }

        public Envelope getEnvelope() {
            return envelope;
        }

        public Geometry getGeometry() {
            return geometry;
        }
    }


    private boolean leaf;
    private Envelope boundary;
    private List<RTree> children;
    private List<Entry> contents;

    private static int capacity = 4;

    public static RTree createLeaf(List<Entry> contents) {
        RTree tree = new RTree();
        tree.leaf = true;
        tree.contents = contents;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Entry content : contents) {
            if (content.envelope.getMinX() < minX) {
                minX = content.envelope.getMinX();
            }
            if (content.envelope.getMinY() < minY) {
                minY = content.envelope.getMinY();
            }
            if (content.envelope.getMaxX() > maxX) {
                maxX = content.envelope.getMaxX();
            }
            if (content.envelope.getMaxY() > maxY) {
                maxY = content.envelope.getMaxY();
            }
        }
        tree.boundary = new Envelope(minX, maxX, minY, maxY);
        return tree;
    }

    public static RTree createParent(List<RTree> children) {
        RTree tree = new RTree();
        tree.leaf = false;
        tree.children = new ArrayList<>(children.size());
        for (RTree child : children) {
            if (child.boundary.getMinX() != Double.POSITIVE_INFINITY) {
                tree.children.add(child);
            }else{
                throw new RuntimeException("");
            }
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (RTree content : tree.children) {
            if (content.boundary.getMinX() < minX) {
                minX = content.boundary.getMinX();
            }
            if (content.boundary.getMinY() < minY) {
                minY = content.boundary.getMinY();
            }
            if (content.boundary.getMaxX() > maxX) {
                maxX = content.boundary.getMaxX();
            }
            if (content.boundary.getMaxY() > maxY) {
                maxY = content.boundary.getMaxY();
            }
        }
        tree.boundary = new Envelope(minX, maxX, minY, maxY);
        return tree;
    }

    public Envelope getBoundary() {
        return boundary;
    }

    public void setBoundary(Envelope boundary) {
        this.boundary = boundary;
    }

    public List<RTree> getChildren() {
        return children;
    }

    public void setChildren(List<RTree> children) {
        this.children = children;
    }

    public List<Entry> getContents() {
        return contents;
    }

    public void setContents(List<Entry> contents) {
        this.contents = contents;
    }

    public static RTree buildSTR(List<Entry> entries) {
        int requiredNodeAmount = (int) Math.ceil((0.0 + entries.size()) / capacity);
        int sliceAmount = (int) Math.ceil(Math.sqrt(requiredNodeAmount));
        int entriesPerSlice = (int) Math.ceil((0.0 + entries.size()) / sliceAmount);
        int nodesPerSlice = (int) Math.ceil((0.0 +entriesPerSlice) / capacity);

        List<RTree> nodes = new ArrayList<>(requiredNodeAmount);

        entries.sort(Comparator.comparingDouble(o -> o.envelope.getMinX() + o.envelope.getMaxX()));
        for (int i = 0; i < sliceAmount; i++) {
            List<Entry> sliceEntries = entries.subList(i * entriesPerSlice, Math.min((i + 1) * entriesPerSlice, entries.size()));
            sliceEntries.sort(Comparator.comparingDouble(o -> o.envelope.getMinY() + o.envelope.getMaxY()));
            for (int j = 0; j < nodesPerSlice; j++) {
                List<Entry> nodeEntries = sliceEntries.subList(Math.min(j * capacity, sliceEntries.size()), Math.min((j + 1) * capacity, sliceEntries.size()));
                if(!nodeEntries.isEmpty()){
                    nodes.add(createLeaf(nodeEntries));
                }
            }
        }
        return buildSTRRec(nodes);
    }

    private static RTree buildSTRRec(List<RTree> entries) {
        int requiredNodeAmount = (int) Math.ceil((0.0 + entries.size()) / capacity);

        if(requiredNodeAmount == 0){
            return createLeaf(new ArrayList<>()); //Empty RTree
        }
        if(requiredNodeAmount == 1){
            return createParent(entries); //Create the root
        }

        int sliceAmount = (int) Math.ceil(Math.sqrt(requiredNodeAmount));
        int entriesPerSlice = (int) Math.ceil((0.0 + entries.size()) / sliceAmount);
        int nodesPerSlice = (int) Math.ceil((0.0 +entriesPerSlice) / capacity);

        List<RTree> nodes = new ArrayList<>(requiredNodeAmount);

        entries.sort(Comparator.comparingDouble(o -> o.boundary.getMinX() + o.boundary.getMaxX()));
        for (int i = 0; i < sliceAmount; i++) {
            List<RTree> sliceEntries = entries.subList(i * entriesPerSlice, Math.min((i + 1) * entriesPerSlice, entries.size()));
            sliceEntries.sort(Comparator.comparingDouble(o -> o.boundary.getMinY() + o.boundary.getMaxY()));
            for (int j = 0; j < nodesPerSlice; j++) {
                List<RTree> nodeEntries = sliceEntries.subList(Math.min(j * capacity, sliceEntries.size()), Math.min((j + 1) * capacity, sliceEntries.size()));
                if(!nodeEntries.isEmpty()){
                    nodes.add(createParent(nodeEntries));
                }
            }
        }
        return buildSTRRec(nodes);
    }

    /*

        public static RTree buildSTR(List<Entry> entries) {
            int requiredNodeAmount = (int) Math.ceil((0.0 + entries.size()) / capacity);
            int sliceAmount = (int) Math.ceil(Math.sqrt(requiredNodeAmount));
            int entriesPerSlice = (int) Math.ceil((0.0 + entries.size()) / sliceAmount);
            int nodesPerSlice = entriesPerSlice / capacity;

            RTree[][] grid = new RTree[sliceAmount][nodesPerSlice];

            entries.sort((o1, o2) -> { //Sort by envelope mid x
                double o1x = o1.envelope.getMaxX() - o1.envelope.getMinX();
                double o2x = o2.envelope.getMaxX() - o2.envelope.getMinX();
                return Double.compare(o1x, o2x);
            });
            for (int i = 0; i < sliceAmount; i++) {
                List<Entry> sliceEntries = entries.subList(i * entriesPerSlice, Math.min((i + 1) * entriesPerSlice, entries.size()));
                sliceEntries.sort((o1, o2) -> { //Sort by envelope mid x
                    double o1y = o1.envelope.getMaxY() - o1.envelope.getMinY();
                    double o2y = o2.envelope.getMaxY() - o2.envelope.getMinY();
                    return Double.compare(o1y, o2y);
                });
                for (int j = 0; j < nodesPerSlice; j++) {
                    List<Entry> nodeEntries = sliceEntries.subList(Math.min(j * capacity, sliceEntries.size()), Math.min((j + 1) * capacity, sliceEntries.size()));
                    grid[i][j] = createLeaf(nodeEntries);
                }
            }

            return buildFromGrid(grid, 0, 0, sliceAmount - 1, nodesPerSlice - 1);
        }
        //maxX and maxY inclusive
        private static RTree buildFromGrid(RTree[][] grid, int minX, int minY, int maxX, int maxY) {
            if (minX == maxX && minY == maxY) {
                RTree tree = grid[minX][minY];
                return tree;
            }
            if (minX == maxX) {
                int size = maxY - minY;
                if (size == 1) {
                    List<RTree> list = List.of(grid[minX][minY], grid[minX][maxY]);
                    return createParent(list);
                } else {
                    throw new RuntimeException("This is impossible");
                }
            }
            if (minY == maxY) {
                int size = maxX - minX;
                if (size == 1) {
                    List<RTree> list = List.of(grid[minX][minY], grid[maxX][minY]);
                    return createParent(list);
                } else {
                    throw new RuntimeException("This is impossible");
                }
            }
            //At least a 2x2 grid
            int sizeX = maxX - minX;
            int sizeY = maxY - minY;
            if (sizeX == 1 && sizeY == 1) {
                List<RTree> list = List.of(grid[minX][minY], grid[minX][maxY], grid[maxX][minY], grid[maxX][maxY]);
                return createParent(list);
            }
            //Bigger than 2x2
            int middleX = (maxX + minX) / 2;
            int middleY = (maxY + minY) / 2;
            return createParent(List.of(
                    buildFromGrid(grid, minX, minY, middleX, middleY),
                    buildFromGrid(grid, middleX + 1, minY, maxX, middleY),
                    buildFromGrid(grid, minX, middleY + 1, middleX, maxY),
                    buildFromGrid(grid, middleX + 1, middleY + 1, maxX, maxY)
            ));
        }
    */
    public List<Entry> search(Envelope envelope) {
        ArrayList<Entry> result = new ArrayList<>();
        search(envelope, result);
        return result;
    }

    private void search(Envelope envelope, List<Entry> result) {
        if (leaf) {
            //for (int i = 0; i < contents.size(); i++) {
            //    Entry entry = contents.get(i);
            //    if (entry.envelope.intersects(envelope)) {
            //        result.add(entry);
            //    }
            //}
            for (Entry content : contents) {
                if (content.envelope.intersects(envelope)) {
                    result.add(content);
                }
            }
        } else {
            //for (int i = 0; i < children.size(); i++) {
            //    RTree child = children.get(i);
            //    if(child.boundary.intersects(envelope)){
            //        child.search(envelope, result);
            //    }
            //}
            for (RTree child : children) {
                if (child.boundary.intersects(envelope)) {
                    child.search(envelope, result);
                }
            }
        }
    }


}

