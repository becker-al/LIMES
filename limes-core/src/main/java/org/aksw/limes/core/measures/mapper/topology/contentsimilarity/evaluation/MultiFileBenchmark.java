package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.APRF;
import org.aksw.limes.core.exceptions.InvalidThresholdException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.pointsets.PropertyFetcher;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.fullr.FullRTreeWrapper;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing.RTreeIndexing;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.indexing.RadonIndexingMBB;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.FARelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.FDRelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.FMRelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.relater.MbbOptimalRelater;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.flexible.wrapper.AbstractWrapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MultiFileBenchmark {

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
        int numThreads = 14;

        test(baseDirectory, "NUTS", "CLC_Split_10000_10", numThreads);
        //test("NUTS", "NUTS", numThreads);
    }

    public static void test(String baseDirectory, String sourceNamesStart, String targetNamesStart, int numThreads) throws ParseException, IOException {

        Map<String, GeoMapper> geoMapperMap = new LinkedHashMap<>();

        geoMapperMap.put("RADON", new RadonWrapper());
        geoMapperMap.put("RADON_MBB", new RadonOnlyMBBWrapper());

        //geoMapperMap.put("RADON", new RadonOnlyMBBWrapper());

        geoMapperMap.put("FA_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new FARelater()));
        geoMapperMap.put("FD_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new FDRelater()));
        geoMapperMap.put("FM_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new FMRelater()));
        geoMapperMap.put("Optimal_Radon_MBB", new AbstractWrapper(new RadonIndexingMBB(), new MbbOptimalRelater()));

        geoMapperMap.put("FA_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new FARelater()));
        geoMapperMap.put("FD_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new FDRelater()));
        geoMapperMap.put("FM_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new FMRelater()));
        geoMapperMap.put("Optimal_RTREE_CUSTOM_MBB", new AbstractWrapper(new RTreeIndexing(), new MbbOptimalRelater()));
        geoMapperMap.put("RTREE_FULL", new FullRTreeWrapper());
        List<String> results = new ArrayList<>();
        for (String relation : RELATIONS) {
            testForRelation(baseDirectory, sourceNamesStart, targetNamesStart, relation, results, geoMapperMap, numThreads);
        }

        log(results, sourceNamesStart, targetNamesStart, numThreads);
    }

    public static void testForRelation(String baseDirectory, String sourceNamesStart, String targetNamesStart, String relation, List<String> results, Map<String, GeoMapper> geoMapperMap, int numThreads) throws ParseException {
        System.out.println(relation);
        String expression = "top_" + relation + "(x.asWKT, y.asWKT)";
        if (relation.equals(COVEREDBY)) {
            expression = "top_" + "covered_by" + "(x.asWKT, y.asWKT)";
        }
        results.add(relation + ",Indexing,Matcher,F,Time,Precision,Recall,TruePositive,FalsePositive,TrueNegative,FalseNegative,,Positive,Negative"); //FScore, TruePositive,FalsePositive,TrueNegative,FalseNegative
        List<String> sourceFileNames = Arrays.stream(new File(baseDirectory).listFiles()).filter(file -> file.getName().toLowerCase().startsWith(sourceNamesStart.toLowerCase())).map(File::getAbsolutePath).sorted().collect(Collectors.toList());
        HashMap<String, Long> totalTime = new HashMap<>();
        HashMap<String, Integer> totalTP = new HashMap<>();
        HashMap<String, Integer> totalFP = new HashMap<>();
        HashMap<String, Integer> totalTN = new HashMap<>();
        HashMap<String, Integer> totalFN = new HashMap<>();

        for (Map.Entry<String, GeoMapper> entry : geoMapperMap.entrySet()) {
            totalTime.put(entry.getKey(), 0L);
            totalTP.put(entry.getKey(), 0);
            totalFP.put(entry.getKey(), 0);
            totalTN.put(entry.getKey(), 0);
            totalFN.put(entry.getKey(), 0);
        }

        for (String sourceFileName : sourceFileNames) {
            System.out.println("SourceFile: " + sourceFileName);
            ACache sourceWithoutSimplification = PolygonSimplification.cacheWithoutSimplification(sourceFileName);
            Map<String, Geometry> sourceMap = createSourceMap(sourceWithoutSimplification, expression, 1.0);
            List<String> targetFileNames = Arrays.stream(new File(baseDirectory).listFiles()).filter(file -> file.getName().toLowerCase().startsWith(targetNamesStart.toLowerCase())).map(File::getAbsolutePath).sorted().collect(Collectors.toList());
            for (String targetFileName : targetFileNames) {
                System.out.println("TargetFile: " + targetFileName);
                ACache targetWithoutSimplification = PolygonSimplification.cacheWithoutSimplification(targetFileName);
                Map<String, Geometry> targetMap = createTargetMap(targetWithoutSimplification, expression, 1.0);

                AMapping radon = null;
                GoldStandard goldStandard = null;
                for (Map.Entry<String, GeoMapper> geoMapperEntry : geoMapperMap.entrySet()) {
                    System.gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    long start = System.currentTimeMillis();
                    AMapping mapping = geoMapperEntry.getValue().getMapping(sourceMap, targetMap, relation, numThreads);
                    long end = System.currentTimeMillis();
                    long time = (end - start);

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
                    totalTime.put(geoMapperEntry.getKey(), totalTime.get(geoMapperEntry.getKey()) + time);
                    totalTP.put(geoMapperEntry.getKey(), (int) (Math.ceil(totalTP.get(geoMapperEntry.getKey())) + tp));
                    totalFP.put(geoMapperEntry.getKey(), (int) (Math.ceil(totalFP.get(geoMapperEntry.getKey())) + fp));
                    totalTN.put(geoMapperEntry.getKey(), (int) (Math.ceil(totalTN.get(geoMapperEntry.getKey())) + tn));
                    totalFN.put(geoMapperEntry.getKey(), (int) (Math.ceil(totalFN.get(geoMapperEntry.getKey())) + fn));
                }

            }
        }

        for (Map.Entry<String, GeoMapper> geoMapperEntry : geoMapperMap.entrySet()) {
            System.out.println(geoMapperEntry.getKey());

            int tp = totalTP.get(geoMapperEntry.getKey());
            int fp = totalFP.get(geoMapperEntry.getKey());
            int tn = totalTN.get(geoMapperEntry.getKey());
            int fn = totalFN.get(geoMapperEntry.getKey());

            System.out.println("Time: " + totalTime.get(geoMapperEntry.getKey()));
            double precision = calculatePrecision(tp, fp, tn, fn);
            System.out.println("Precision:" + precision);
            double recall = calculateRecall(tp, fp, tn, fn);
            System.out.println("Recall:" + recall);
            double f = calculateFScore(tp, fp, tn, fn);
            System.out.println("F:" + f);
            results.add(String.join(",", "", geoMapperEntry.getValue().getIndexingName(), geoMapperEntry.getValue().getMatcherName(), f + "", totalTime.get(geoMapperEntry.getKey()) + "", precision + "", recall + "", tp + "", fp + "", tn + "", fn + "", "", (tp + fp) + "", (tn + fn) + ""));

        }

        results.add("");
    }

    public static void log(List<String> results, String sourceName, String targetName, int numThreads) throws IOException {
        FileWriter writer = new FileWriter("multi_results_" + sourceName + "_" + targetName + "_" + numThreads + ".csv");
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
