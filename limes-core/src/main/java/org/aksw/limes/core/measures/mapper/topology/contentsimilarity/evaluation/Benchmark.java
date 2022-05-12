package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.APRF;
import org.aksw.limes.core.evaluation.qualititativeMeasures.FMeasure;
import org.aksw.limes.core.exceptions.InvalidThresholdException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.pointsets.PropertyFetcher;
import org.aksw.limes.core.measures.mapper.topology.RADON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Benchmark {

    private static final String nuts = "P:\\Cloud\\Studium\\22_Bachelorarbeit\\GeoConverter\\NUTS.nt";
    private static final String clc = "P:\\Cloud\\Studium\\22_Bachelorarbeit\\GeoConverter\\CLC.nt";

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
            EQUALS, DISJOINT, INTERSECTS, TOUCHES, WITHIN, CONTAINS, OVERLAPS, COVERS, COVEREDBY
    };

    public static void main(String[] args) throws ParseException, IOException {
        ACache sourceWithoutSimplification = PolygonSimplification.cacheWithoutSimplification(nuts);
        ACache targetWithoutSimplification = PolygonSimplification.cacheWithoutSimplification(nuts);

        Map<String, GeoMapper> geoMapperMap = new HashMap<>();
        geoMapperMap.put("FA", new FAWrapper());
        geoMapperMap.put("FD", new FDWrapper());
        geoMapperMap.put("FM", new FMWrapper());
        geoMapperMap.put("RADON", new RadonWrapper());

        List<String> results = new ArrayList<>();
        for (String relation : RELATIONS) {
            testForRelation(sourceWithoutSimplification, targetWithoutSimplification, relation, results, geoMapperMap);
        }

        log(results);
    }

    private static void testForRelation(ACache sourceWithoutSimplification, ACache targetWithoutSimplification, String relation, List<String> results, Map<String, GeoMapper> geoMapperMap) {
        String expression = "top_" + relation + "(x.asWKT, y.asWKT)";
        if (relation == COVEREDBY) {
            expression = "top_" + "covered_by" + "(x.asWKT, y.asWKT)";
        }
        Map<String, Geometry> sourceMap = createSourceMap(sourceWithoutSimplification, expression, 1.0);
        Map<String, Geometry> targetMap = createTargetMap(targetWithoutSimplification, expression, 1.0);

        results.add(relation + ",Algo,Time,Precision,Recall,F,TP,FP,TN,FN"); //FScore, TruePositive,FalsePositive,TrueNegative,FalseNegative
        FMeasure fMeasure = new FMeasure();

        AMapping radon = RADON.getMapping(sourceMap, targetMap, relation);
        GoldStandard goldStandard = new GoldStandard(radon);
        goldStandard.sourceUris = sourceWithoutSimplification.getAllUris();
        goldStandard.targetUris = targetWithoutSimplification.getAllUris();

        for (Map.Entry<String, GeoMapper> geoMapperEntry : geoMapperMap.entrySet()) {
            System.out.println("------------------");
            System.out.println(geoMapperEntry.getKey());
            long start = System.currentTimeMillis();
            AMapping mapping = geoMapperEntry.getValue().getMapping(sourceMap, targetMap, relation);
            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.println("Time: " + time);

            double precision = fMeasure.precision(mapping, goldStandard);
            System.out.println("Precision:" + precision);
            double recall = fMeasure.recall(mapping, goldStandard);
            System.out.println("Recall:" + recall);
            double f = fMeasure.calculate(mapping, goldStandard);
            System.out.println("F:" + f);


            double tp = APRF.trueFalsePositive(mapping, radon, true);
            double fp = APRF.trueFalsePositive(mapping, radon, false);
            double tn = APRF.trueNegative(mapping, goldStandard);
            double fn = APRF.falseNegative(mapping, radon);


            results.add(String.join(",", "", geoMapperEntry.getKey(), time + "", precision + "", recall + "", f+ "", tp+"", fp+"", tn+"", fn+""));
        }
        results.add("");
    }

    public static void log(List<String> results) throws IOException {
        FileWriter writer = new FileWriter("results.csv");
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

}
