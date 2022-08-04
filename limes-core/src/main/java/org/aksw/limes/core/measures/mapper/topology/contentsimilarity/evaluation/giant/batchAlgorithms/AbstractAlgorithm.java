package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.batchAlgorithms;

import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.datamodel.RelatedGeometries;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAlgorithm {

    protected double thetaX;
    protected double thetaY;

    protected long indexingTime;
    protected long verificationTime;
    protected RelatedGeometries relations;


    protected final Map<String, Geometry> sourceData;
    protected final Map<String, Geometry> targetData;

    protected final Map<Integer, Geometry> sourceDataById;
    protected final Map<Integer, String> sourceUriIds;


    protected final String relation;
    protected final int numThreads;


    public AbstractAlgorithm(Map<String, Geometry> sourceData, Map<String, Geometry> targetData,
                             String relation, int numThreads) {
        this.sourceData = sourceData;
        this.targetData = targetData;
        this.relation = relation;
        this.numThreads = numThreads;

        sourceDataById = new HashMap<>();
        sourceUriIds = new HashMap<>();

        relations = new RelatedGeometries(sourceData.size() * targetData.size());
    }

    public void applyProcessing() {
        setThetas();
    }

    public void printResults() {
        System.out.println("Indexing time\t:\t" + indexingTime);
        System.out.println("Verification time\t:\t" + verificationTime);
        relations.print();
    }

    protected abstract void setThetas();
}
