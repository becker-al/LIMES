package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.splitcontentmeasuretest;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public interface Splitter {

    Envelope[][] getSplit(Geometry geo, int times);

}
