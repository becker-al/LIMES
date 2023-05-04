package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.APRF;
import org.aksw.limes.core.exceptions.InvalidThresholdException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.pointsets.PropertyFetcher;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.cobalt_split.EqualSplitter;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.cobalt_split.FittingSplitter;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.cobalt_split.CobaltSplit;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.AbstractRTreeIndexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.AbstractRTreeMatchingIndexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.RadonIndexingMBB;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.HilbertRTree;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeOMT;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSTR;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.indexing.rtrees.RTreeSmallestX;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.FDMatcher;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.matcher.Matcher;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper.CombinedFullGeoMapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper.CombinedGeoMapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms.wrapper.RTreeIndexingFull;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.batchAlgorithms.GiantMBBWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.batchAlgorithms.GiantWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.radon.RadonOnlyMBBWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.radon.RadonSimplifiedWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.radon.RadonWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.rstar.RStarTreeIndexing;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Benchmark {

    public static final String EQUALS = "equals";
    public static final String DISJOINT = "disjoint";
    public static final String INTERSECTS = "intersects";
    public static final String TOUCHES = "touches";
    //public static final String CROSSES = "crosses"; //Crosses is not defined by godoy
    public static final String WITHIN = "within";
    public static final String CONTAINS = "contains";
    public static final String OVERLAPS = "overlaps";
    public static final String COVERS = "covers";
    public static final String COVEREDBY = "coveredby";

    public static final String[] RELATIONS = new String[]{
            //EQUALS,
            //INTERSECTS, TOUCHES
            //CONTAINS, COVERS, COVEREDBY,
            //OVERLAPS
            //EQUALS, INTERSECTS, CONTAINS, OVERLAPS, TOUCHES
            EQUALS, INTERSECTS, CONTAINS, WITHIN, OVERLAPS, TOUCHES // <----------
            //EQUALS
            //CONTAINS
            //TOUCHES
            //WITHIN
            //OVERLAPS
            //EQUALS, INTERSECTS, TOUCHES, WITHIN
    };

    public static void main(String[] args) throws ParseException, IOException {
    }

    public static void test(String baseDirectory, String sourceName, String targetName, int numThreads, String outputFile) throws ParseException, IOException {
        ACache sourceWithoutSimplification = PolygonReader.cachePolygons(baseDirectory + sourceName + ".nt");
        ACache targetWithoutSimplification = PolygonReader.cachePolygons(baseDirectory + targetName + ".nt");

        Map<String, GeoMapper> geoMapperMap = new LinkedHashMap<>();
        geoMapperMap.put("RADON", new RadonWrapper());



        geoMapperMap.put("RADON_MBB", new RadonOnlyMBBWrapper());
        geoMapperMap.put("GIANT", new GiantWrapper());
        geoMapperMap.put("GIANT_MBB", new GiantMBBWrapper());

        double[] simplValues = new double[]{0.05, 0.1, 0.2};
        for (double value : simplValues) {
            geoMapperMap.put("RADON_SIMPLIFIED_" + value, new RadonSimplifiedWrapper(value));
        }


        int[] capacities = new int[]{4, 8, 16, 32, 64, 128, 256};

        Map<String, Matcher> cobaltMatcher = new TreeMap<>();


        for (Map.Entry<String, Matcher> matcherEntry : cobaltMatcher.entrySet()) {
            geoMapperMap.put("RADON-" + matcherEntry.getKey(), new CombinedGeoMapper(new RadonIndexingMBB(), matcherEntry.getValue()));
        }
        for (int capacity : capacities) {
            for (Map.Entry<String, Matcher> matcherEntry : cobaltMatcher.entrySet()) {
                geoMapperMap.put("RTreeSmallestX-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeIndexing(() -> new RTreeSmallestX(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("RTreeSTR-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeIndexing(() -> new RTreeSTR(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("RTreeOMT-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeIndexing(() -> new RTreeOMT(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("HilbertPack-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeIndexing(() -> new HilbertRTree(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("MatchRTreeSmallestX-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeMatchingIndexing(() -> new RTreeSmallestX(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("MatchRTreeSTR-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeMatchingIndexing(() -> new RTreeSTR(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("MatchRTreeOTM-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeMatchingIndexing(() -> new RTreeOMT(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("MatchHilbertPack-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new AbstractRTreeMatchingIndexing(() -> new HilbertRTree(capacity)), matcherEntry.getValue()));
                geoMapperMap.put("RStarTree-" + capacity + "-" + matcherEntry.getKey(), new CombinedGeoMapper(new RStarTreeIndexing(capacity), matcherEntry.getValue()));
            }
        }




        for (Map.Entry<String, Matcher> matcherEntry : cobaltMatcher.entrySet()) {
            for (int i = 0; i <= 4; i++) {
                geoMapperMap.put("FittingSplitter-" + i + "-" + matcherEntry.getKey(), new CombinedFullGeoMapper(new RTreeIndexingFull(), new CobaltSplit(i, new FittingSplitter(), matcherEntry.getValue())));
            }
            for (int i = 0; i <= 4; i++) {
                geoMapperMap.put("EqualSplitter-" + i + "-" + matcherEntry.getKey(), new CombinedFullGeoMapper(new RTreeIndexingFull(), new CobaltSplit(i, new EqualSplitter(), matcherEntry.getValue())));
            }
        }


        List<String> results = new ArrayList<>();
        for (String relation : RELATIONS) {
            testForRelation(sourceWithoutSimplification, targetWithoutSimplification, relation, results, geoMapperMap, numThreads);
        }

        log(results, sourceName, targetName, numThreads, outputFile);
    }

    private static void testForRelation(ACache sourceWithoutSimplification, ACache targetWithoutSimplification, String relation, List<String> results, Map<String, GeoMapper> geoMapperMap, int numThreads) {
        String expression = "top_" + relation + "(x.asWKT, y.asWKT)";
        if (relation.equals(COVEREDBY)) {
            expression = "top_" + "covered_by" + "(x.asWKT, y.asWKT)";
        }
        Map<String, Geometry> sourceMap = createSourceMap(sourceWithoutSimplification, expression, 1.0);
        Map<String, Geometry> targetMap = createTargetMap(targetWithoutSimplification, expression, 1.0);

        results.add(relation + ",AlgoName,F,Time,Precision,Recall,TruePositive,FalsePositive,TrueNegative,FalseNegative,,Positive,Negative"); //FScore, TruePositive,FalsePositive,TrueNegative,FalseNegative

        AMapping radon = null;
        GoldStandard goldStandard = null;

        for (Map.Entry<String, GeoMapper> geoMapperEntry : geoMapperMap.entrySet()) {
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("------------------");
            System.out.println(geoMapperEntry.getKey() + " | " + relation);
            long start = System.currentTimeMillis();
            AMapping mapping = null;
            for (int i = 0; i < 1; i++) {
                mapping = geoMapperEntry.getValue().getMapping(sourceMap, targetMap, relation, numThreads);
            }
            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.println("Time: " + time);

            if (radon == null) {
                if (geoMapperEntry.getKey().equalsIgnoreCase("RADON")) {
                    radon = mapping;
                    goldStandard = new GoldStandard(radon);
                    goldStandard.sourceUris = sourceWithoutSimplification.getAllUris();
                    goldStandard.targetUris = targetWithoutSimplification.getAllUris();
                } else {
                    throw new RuntimeException("Radon has to be the first mapper in the GeoMapperMap");
                }
            }


            double tp = APRF.trueFalsePositive(mapping, radon, true);
            double fp = APRF.trueFalsePositive(mapping, radon, false);
            double tn = APRF.trueNegative(mapping, goldStandard);
            double fn = APRF.falseNegative(mapping, radon);

            double precision = calculatePrecision(tp, fp, tn, fn);
            System.out.println("Precision:" + precision);
            double recall = calculateRecall(tp, fp, tn, fn);
            System.out.println("Recall:" + recall);
            double f = calculateFScore(tp, fp, tn, fn);
            System.out.println("F:" + f);
            System.out.println("TP: " + tp);
            System.out.println("FP: " + fp);

            results.add(String.join(",", "", geoMapperEntry.getKey(), f + "", time + "", precision + "", recall + "", tp + "", fp + "", tn + "", fn + "", "", (tp + fp) + "", (tn + fn) + ""));
        }
        results.add("");
    }

    public static void log(List<String> results, String sourceName, String targetName, int numThreads, String outputFile) throws IOException {
        File file = new File(outputFile);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileWriter writer = new FileWriter(new File(outputFile, "results_" + sourceName + "_" + targetName + "_" + numThreads + ".csv"));
        for (String str : results) {
            writer.write(str + System.lineSeparator());
        }
        writer.close();
    }


    public static Map<String, Geometry> getGeometryMapFromCache(ACache c, String property) {
        WKTReader wktReader = new WKTReader();
        Map<String, Geometry> gMap = new HashMap<>();
        for (String uri : c.getAllUris()) {
            Set<String> values = c.getInstance(uri).getProperty(property);
            if (values.size() > 0) {
                String wkt = values.iterator().next();
                try {
                    gMap.put(uri, wktReader.read(wkt));
                } catch (ParseException e) {
                    System.out.println("Skipping malformed geometry at " + uri + "...");
                }
            }
        }
        return gMap;
    }

    //Code taken from RADON
    public static Map<String, Geometry> createSourceMap(ACache source, String expression, double threshold) {
        if (threshold <= 0) {
            throw new InvalidThresholdException(threshold);
        }
        List<String> properties = PropertyFetcher.getProperties(expression, threshold);
        Map<String, Geometry> sourceMap = getGeometryMapFromCache(source, properties.get(0));
        return sourceMap;
    }

    //Code taken from RADON
    public static Map<String, Geometry> createTargetMap(ACache target, String expression, double threshold) {
        if (threshold <= 0) {
            throw new InvalidThresholdException(threshold);
        }
        List<String> properties = PropertyFetcher.getProperties(expression, threshold);
        Map<String, Geometry> targetMap = getGeometryMapFromCache(target, properties.get(1));
        return targetMap;
    }

    public static double calculatePrecision(double tp, double fp, double tn, double fn) {
        if (tp + fp == 0) {
            return 0;
        }
        return tp / (tp + fp);
    }

    public static double calculateRecall(double tp, double fp, double tn, double fn) {
        if (tp + fn == 0) {
            return 0;
        }
        return tp / (tp + fn);
    }

    public static double calculateFScore(double tp, double fp, double tn, double fn) {
        double p = calculatePrecision(tp, fp, tn, fn);
        double r = calculateRecall(tp, fp, tn, fn);
        if (p + r > 0d) {
            return (1 + 1.0D) * p * r / (p + r);
        } else {
            return 0d;
        }
    }

}
