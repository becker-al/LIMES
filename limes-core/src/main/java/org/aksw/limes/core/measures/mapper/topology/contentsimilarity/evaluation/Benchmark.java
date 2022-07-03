package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.APRF;
import org.aksw.limes.core.exceptions.InvalidThresholdException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.pointsets.PropertyFetcher;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.fullr.FullRTreeWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing.RTreeIndexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing.RTreeIndexingWithoutComputeCheck;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing.RadonIndexingMBB;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.FARelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.FDRelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.FMRelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.MbbOptimalRelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.wrapper.AbstractWrapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Benchmark {

    private static final String baseDirectory = "P:\\Cloud\\Studium\\22_Bachelorarbeit\\GeoConverter\\";

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
            EQUALS, DISJOINT, INTERSECTS, TOUCHES, WITHIN,
            CONTAINS,
            OVERLAPS, COVERS, COVEREDBY
    };


    public static void main(String[] args) throws ParseException, IOException {
        //String sourceName = "NUTS";
        //String targetName = "CLC_Subset_111";
        int numThreads = 1;

        test(baseDirectory, "NUTS", "NUTS", numThreads);
        test(baseDirectory, "NUTS", "CLC_Subset_111", numThreads);
        test(baseDirectory, "CLC_Subset_111", "CLC_Subset_111", numThreads);
        //test("NUTS", "NUTS", numThreads);
    }

    public static void test(String baseDirectory, String sourceName, String targetName, int numThreads) throws ParseException, IOException {
        ACache sourceWithoutSimplification = PolygonSimplification.cacheWithoutSimplification(baseDirectory + sourceName + ".nt");
        ACache targetWithoutSimplification = PolygonSimplification.cacheWithoutSimplification(baseDirectory + targetName + ".nt");

        Map<String, GeoMapper> geoMapperMap = new LinkedHashMap<>();
        geoMapperMap.put("RADON", new RadonWrapper());
        geoMapperMap.put("RADON_MBB", new RadonOnlyMBBWrapper());

        //geoMapperMap.put("RADON", new RadonOnlyMBBWrapper());

        geoMapperMap.put("FA_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new FARelater()));
        geoMapperMap.put("FD_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new FDRelater()));
        geoMapperMap.put("FM_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new FMRelater()));
        geoMapperMap.put("Optimal_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new MbbOptimalRelater()));

        geoMapperMap.put("FA_RTREE_WITHOUTCHECK_MBB", new AbstractWrapper(new RTreeIndexingWithoutComputeCheck(), new FARelater()));
        geoMapperMap.put("FD_RTREE_WITHOUTCHECK_MBB", new AbstractWrapper(new RTreeIndexingWithoutComputeCheck(), new FDRelater()));
        geoMapperMap.put("FM_RTREE_WITHOUTCHECK_MBB", new AbstractWrapper(new RTreeIndexingWithoutComputeCheck(), new FMRelater()));
        geoMapperMap.put("Optimal_RTREE_WITHOUTCHECK_MBB", new AbstractWrapper(new RTreeIndexingWithoutComputeCheck(), new MbbOptimalRelater()));


        geoMapperMap.put("FA_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new FARelater()));
        geoMapperMap.put("FD_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new FDRelater()));
        geoMapperMap.put("FM_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new FMRelater()));
        geoMapperMap.put("Optimal_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new MbbOptimalRelater()));
        geoMapperMap.put("RTREE_FULL", new FullRTreeWrapper());

        List<String> results = new ArrayList<>();
        for (String relation : RELATIONS) {
            testForRelation(sourceWithoutSimplification, targetWithoutSimplification, relation, results, geoMapperMap, numThreads);
        }

        log(results, sourceName, targetName, numThreads);
    }

    private static void testForRelation(ACache sourceWithoutSimplification, ACache targetWithoutSimplification, String relation, List<String> results, Map<String, GeoMapper> geoMapperMap, int numThreads) {
        String expression = "top_" + relation + "(x.asWKT, y.asWKT)";
        if (relation.equals(COVEREDBY)) {
            expression = "top_" + "covered_by" + "(x.asWKT, y.asWKT)";
        }
        Map<String, Geometry> sourceMap = createSourceMap(sourceWithoutSimplification, expression, 1.0);
        Map<String, Geometry> targetMap = createTargetMap(targetWithoutSimplification, expression, 1.0);

        results.add(relation + ",Indexing,Matcher,F,Time,Precision,Recall,TruePositive,FalsePositive,TrueNegative,FalseNegative,,Positive,Negative"); //FScore, TruePositive,FalsePositive,TrueNegative,FalseNegative

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


            results.add(String.join(",", "", geoMapperEntry.getValue().getIndexingName(), geoMapperEntry.getValue().getMatcherName(), f + "", time + "", precision + "", recall + "", tp + "", fp + "", tn + "", fn + "", "", (tp + fp) + "", (tn + fn) + ""));
        }
        results.add("");
    }

    public static void log(List<String> results, String sourceName, String targetName, int numThreads) throws IOException {
        FileWriter writer = new FileWriter("results_" + sourceName + "_" + targetName + "_" + numThreads + ".csv");
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

    public static Map<String, Geometry> createSourceMap(ACache source, String expression, double threshold) {
        if (threshold <= 0) {
            throw new InvalidThresholdException(threshold);
        }
        List<String> properties = PropertyFetcher.getProperties(expression, threshold);
        Map<String, Geometry> sourceMap = getGeometryMapFromCache(source, properties.get(0));
        return sourceMap;
    }

    public static Map<String, Geometry> createTargetMap(ACache target, String expression, double threshold) {
        if (threshold <= 0) {
            throw new InvalidThresholdException(threshold);
        }
        List<String> properties = PropertyFetcher.getProperties(expression, threshold);
        Map<String, Geometry> targetMap = getGeometryMapFromCache(target, properties.get(1));
        return targetMap;
    }

    public static double calculatePrecision(double tp, double fp, double tn, double fn){
        if (tp + fp == 0){
            return 0;
        }
        return tp / (tp + fp);
    }

    public static double calculateRecall(double tp, double fp, double tn, double fn){
        if (tp + fn == 0){
            return 0;
        }
        return tp / (tp + fn);
    }

    public static double calculateFScore(double tp, double fp, double tn, double fn){
        double beta = 1.0D;

        double p = calculatePrecision(tp, fp, tn, fn);
        double r = calculateRecall(tp, fp, tn, fn);
        double beta2 = Math.pow(beta, 2);

        if (p + r > 0d)
            return (1 + beta2) * p * r / ((beta2 * p) + r);
        else
            return 0d;

    }

}
