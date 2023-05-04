package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.RTree;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRTree implements RTree {

    protected boolean leaf;
    protected Envelope boundary;
    protected List<AbstractRTree> children;
    protected Entry content;
    protected int capacity = 4;

    public AbstractRTree() {
        leaf = true;
    }

    public AbstractRTree(int capacity) {
        this.capacity = capacity;
        leaf = true;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public Envelope getBoundary() {
        return boundary;
    }

    public List<AbstractRTree> getChildren() {
        return children;
    }

    public Entry getContent() {
        return content;
    }

    protected void recalculateBoundary() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (AbstractRTree content : this.children) {
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
        this.boundary = new Envelope(minX, maxX, minY, maxY);
    }

    @Override
    public List<Entry> search(Envelope envelope) {
        ArrayList<Entry> result = new ArrayList<>();
        search(envelope, result);
        return result;
    }

    private void search(Envelope envelope, List<Entry> result) {
        if (leaf) {
            result.add(content);
        } else {
            for (AbstractRTree child : children) {
                if (child.boundary.intersects(envelope)) {
                    child.search(envelope, result);
                }
            }
        }
    }

    @Override
    public List<Entry> searchExcept(Envelope envelope) {
        ArrayList<Entry> result = new ArrayList<>();
        searchExcept(envelope, result);
        return result;
    }

    private void searchExcept(Envelope envelope, List<Entry> result) {
        if (leaf) {
            result.add(content);
        } else {
            for (AbstractRTree child : children) {
                if (child.boundary.intersects(envelope)) {
                    child.searchExcept(envelope, result);
                } else {
                    child.addRecursive(result);
                }
            }
        }
    }

    private void addRecursive(List<Entry> result) {
        if (leaf) {
            result.add(content);
        } else {
            for (AbstractRTree child : children) {
                child.addRecursive(result);
            }
        }
    }

    protected static AbstractRTree createStaticParent(List<AbstractRTree> children, int capacity) {
        AbstractRTree tree = new StaticRTree(capacity);
        tree.leaf = false;
        tree.children = new ArrayList<>(children.size());
        for (AbstractRTree child : children) {
            if (child.boundary.getMinX() != Double.POSITIVE_INFINITY) {
                tree.children.add(child);
            } else {
                throw new RuntimeException("");
            }
        }
        tree.recalculateBoundary();
        return tree;
    }

    protected static AbstractRTree createStaticLeaf(Entry content) {
        AbstractRTree tree = new StaticRTree(1);
        tree.leaf = true;
        tree.content = content;
        tree.boundary = content.getEnvelope();
        return tree;
    }

    protected void takeValues(AbstractRTree other) {
        leaf = other.leaf;
        boundary = other.boundary;
        children = other.children;
        content = other.content;
    }


}
