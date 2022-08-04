package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.batchAlgorithms;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.datamodel.LightIndex;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class GIAnt extends AbstractAlgorithm {

    private final AMapping m = MappingFactory.createDefaultMapping();

    public GIAnt(Map<String, Geometry> sourceData, Map<String, Geometry> targetData,
                 String relation, int numThreads) {
        super(sourceData, targetData, relation, numThreads);
    }

    @Override
    public void applyProcessing() {
        long time1 = System.currentTimeMillis();

        super.applyProcessing();
        final LightIndex spatialIndex = indexSource();

        long time2 = System.currentTimeMillis();
        try {
            matchTargetData(spatialIndex);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        long time3 = System.currentTimeMillis();
        indexingTime = time2 - time1;
        verificationTime = time3 - time2;
    }

    private void addToIndex(int geometryId, Envelope envelope, LightIndex spatialIndex) {
        int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
        int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
        int minX = (int) Math.floor(envelope.getMinX() / thetaX);
        int minY = (int) Math.floor(envelope.getMinY() / thetaY);
        for (int latIndex = minX; latIndex <= maxX; latIndex++) {
            for (int longIndex = minY; longIndex <= maxY; longIndex++) {
                spatialIndex.add(latIndex, longIndex, geometryId);
            }
        }
    }

    private LightIndex indexSource() {
        final LightIndex spatialIndex = new LightIndex();
        for (Map.Entry<String, Geometry> entry : sourceData.entrySet()) {
        }

        int i = 0;
        for (Map.Entry<String, Geometry> entry : sourceData.entrySet()) {
            addToIndex(i, entry.getValue().getEnvelopeInternal(), spatialIndex);
            sourceDataById.put(i, entry.getValue());
            sourceUriIds.put(i, entry.getKey());
            i++;
        }
        return spatialIndex;
    }

    public AMapping getM() {
        return m;
    }

    private void matchTargetData(LightIndex spatialIndex) throws IOException {
        if (numThreads == 1) {
            for (Map.Entry<String, Geometry> entry : targetData.entrySet()) {
                Geometry geometry = entry.getValue();
                final TIntSet candidateMatches = new TIntHashSet();
                final Envelope envelope = geometry.getEnvelopeInternal();

                int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
                int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
                int minX = (int) Math.floor(envelope.getMinX() / thetaX);
                int minY = (int) Math.floor(envelope.getMinY() / thetaY);
                for (int latIndex = minX; latIndex <= maxX; latIndex++) {
                    for (int longIndex = minY; longIndex <= maxY; longIndex++) {
                        final TIntList partialCandidates = spatialIndex.getSquare(latIndex, longIndex);
                        if (partialCandidates != null) {
                            candidateMatches.addAll(partialCandidates);
                        }
                    }
                }

                final TIntIterator intIterator = candidateMatches.iterator();
                while (intIterator.hasNext()) {
                    int candidateMatchId = intIterator.next();
                    Geometry sourceGeom = sourceDataById.get(candidateMatchId);
                    Envelope abb = sourceGeom.getEnvelopeInternal();
                    if (abb.intersects(geometry.getEnvelopeInternal())) {
                        boolean compute = (relation.equals(RADON.COVERS) && abb.covers(envelope))
                                || (relation.equals(RADON.COVEREDBY) && envelope.covers(abb))
                                || (relation.equals(RADON.CONTAINS) && abb.contains(envelope))
                                || (relation.equals(RADON.WITHIN) && envelope.contains(abb))
                                || (relation.equals(RADON.EQUALS) && abb.equals(envelope))
                                || relation.equals(RADON.INTERSECTS) || relation.equals(RADON.TOUCHES)
                                || relation.equals(RADON.OVERLAPS);
                        if (compute) {
                            if (relations.verifyRelation(sourceGeom, geometry, relation)) {
                                //ADD TO
                                m.add(sourceUriIds.get(candidateMatchId), entry.getKey(), 1.0);
                            }
                        }
                    }
                }
            }
        } else {
            ExecutorService exec = Executors.newFixedThreadPool(numThreads);
            for (Map.Entry<String, Geometry> entry : targetData.entrySet()) {
                Geometry geometry = entry.getValue();
                final TIntSet candidateMatches = new TIntHashSet();
                final Envelope envelope = geometry.getEnvelopeInternal();

                int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
                int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
                int minX = (int) Math.floor(envelope.getMinX() / thetaX);
                int minY = (int) Math.floor(envelope.getMinY() / thetaY);
                for (int latIndex = minX; latIndex <= maxX; latIndex++) {
                    for (int longIndex = minY; longIndex <= maxY; longIndex++) {
                        final TIntList partialCandidates = spatialIndex.getSquare(latIndex, longIndex);
                        if (partialCandidates != null) {
                            candidateMatches.addAll(partialCandidates);
                        }
                    }
                }

                final TIntIterator intIterator = candidateMatches.iterator();
                while (intIterator.hasNext()) {
                    int candidateMatchId = intIterator.next();
                    Geometry sourceGeom = sourceDataById.get(candidateMatchId);
                    Envelope abb = sourceGeom.getEnvelopeInternal();
                    if (abb.intersects(geometry.getEnvelopeInternal())) {
                        boolean compute = (relation.equals(RADON.COVERS) && abb.covers(envelope))
                                || (relation.equals(RADON.COVEREDBY) && envelope.covers(abb))
                                || (relation.equals(RADON.CONTAINS) && abb.contains(envelope))
                                || (relation.equals(RADON.WITHIN) && envelope.contains(abb))
                                || (relation.equals(RADON.EQUALS) && abb.equals(envelope))
                                || relation.equals(RADON.INTERSECTS) || relation.equals(RADON.TOUCHES)
                                || relation.equals(RADON.OVERLAPS);
                        if (compute) {
                            exec.submit(() -> {

                                if (relations.verifyRelation(sourceGeom, geometry, relation)) {
                                    //ADD TO
                                    synchronized (m) {
                                        m.add(sourceUriIds.get(candidateMatchId), entry.getKey(), 1.0);
                                    }
                                }
                            });
                        }
                    }
                }
            }
            exec.shutdown();
            try {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    protected void setThetas() {
        thetaX = 0;
        thetaY = 0;
        for (Map.Entry<String, Geometry> entry : sourceData.entrySet()) {
            final Envelope en = entry.getValue().getEnvelopeInternal();
            thetaX += en.getWidth();
            thetaY += en.getHeight();
        }
        thetaX /= sourceData.size();
        thetaY /= sourceData.size();
    }
}
