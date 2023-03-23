package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.AbstractRTree;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

public interface RTree {

    public static class Entry {
        protected String uri;
        protected Envelope envelope;
        protected Geometry geometry;

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

    List<Entry> search(Envelope envelope);

    List<Entry> searchExcept(Envelope envelope);

    void build(List<Entry> entries);

    public boolean isLeaf();

    public Envelope getBoundary();

    public List<AbstractRTree> getChildren();

    public Entry getContent();

}
