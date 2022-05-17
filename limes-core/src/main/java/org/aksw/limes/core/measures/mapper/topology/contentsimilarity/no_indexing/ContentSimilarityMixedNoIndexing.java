package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.no_indexing;

import org.aksw.limes.core.exceptions.InvalidThresholdException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.pointsets.PropertyFetcher;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.ContentMeasure;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.aksw.limes.core.measures.mapper.topology.RADON.CROSSES;


public class ContentSimilarityMixedNoIndexing {

    public static class Matcher implements Runnable {

        public static int maxSize = 1000;
        private String relation;
        private final List<Map<String, Set<String>>> result;
        private List<Map.Entry<String, Geometry>> scheduled;

        public Matcher(String relation, List<Map<String, Set<String>>> result) {
            this.relation = relation;
            this.result = result;
            this.scheduled = new ArrayList<>();
        }

        @Override
        public void run() {
            Map<String, Set<String>> temp = new HashMap<>();
            for (int i = 0; i < scheduled.size(); i += 2) {
                Map.Entry<String, Geometry> s = scheduled.get(i);
                Map.Entry<String, Geometry> t = scheduled.get(i + 1);
                if (relate(s.getValue(), t.getValue(), relation)) {
                    if (!temp.containsKey(s.getKey())) {
                        temp.put(s.getKey(), new HashSet<>());
                    }
                    temp.get(s.getKey()).add(t.getKey());
                }
            }
            synchronized (result) {
                result.add(temp);
            }
        }

        public void schedule(Map.Entry<String, Geometry> s, Map.Entry<String, Geometry> t) {
            scheduled.add(s);
            scheduled.add(t);
        }

        public int size() {
            return scheduled.size();
        }

        public static boolean relate(Geometry s, Geometry t, String relation) {
            Envelope mbrA = s.getEnvelopeInternal();
            Envelope mbrB = t.getEnvelopeInternal();
            double X = ContentMeasure.fM(mbrA, mbrB);
            double Y = ContentMeasure.fM(mbrB, mbrA);
            double Z = X + Y;

            switch (relation) {
                case EQUALS:
                    if (X == -1 && Y == -1 && Z == -2) {
                        return true;
                    } else {
                        return false;
                    }
                case DISJOINT:
                    if (X > 1 && Y > 1 && Z > 2) {
                        return true;
                    } else {
                        return false;
                    }
                case INTERSECTS:
                    if (!(X > 1 && Y > 1 && Z > 2)) {
                        return true;
                    } else {
                        return false;
                    }
                case TOUCHES: //meet
                    if (X == 1 && Y == 1 && Z == 2) {
                        return true;
                    } else {
                        return false;
                    }
                case WITHIN://inside
                    if (X < -1 && Math.abs(Y) < 1 && (-2 < Z && Z < 0)) {
                        return true;
                    } else {
                        return false;
                    }
                case CONTAINS:
                    if (Math.abs(X) < 1 && Y < -1 && (-2 < Z && Z < 0)) {
                        return true;
                    } else {
                        return false;
                    }
                case COVERS:
                    if (Math.abs(X) < 1 && Y == -1 && (-2 < Z && Z < 0)) {
                        return true;
                    } else {
                        return false;
                    }
                case COVEREDBY:
                    if (X == -1 && Math.abs(Y) < 1 && (-2 < Z && Z < 0)) {
                        return true;
                    } else {
                        return false;
                    }
                case OVERLAPS:
                    if (Math.abs(X) < 1 && Math.abs(Y) < 1 && Math.abs(Z) < 2) {
                        return true;
                    } else {
                        return false;
                    }
                default:
                    return false;
            }


        }
    }

    public static class Merger implements Runnable {

        private AMapping m;
        private List<Map<String, Set<String>>> localResults = new ArrayList<>();

        public Merger(List<Map<String, Set<String>>> results, AMapping m) {
            this.m = m;
            // copy over entries to local list
            synchronized (results) {
                for (Iterator<Map<String, Set<String>>> iterator = results.listIterator(); iterator.hasNext(); ) {
                    localResults.add(iterator.next());
                    iterator.remove();
                }
            }
        }

        @Override
        public void run() {
            // merge back to m
            for (Map<String, Set<String>> result : localResults) {
                for (String s : result.keySet()) {
                    for (String t : result.get(s)) {
                        m.add(s, t, 1.0d);
                    }
                }
            }
        }
    }

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
    // best measure according to our evaluation in the RADON paper
    public static String heuristicStatMeasure = "avg";

    private static final Logger logger = Logger.getLogger(ContentSimilarityAreaNoIndexing.class);

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
                    logger.warn("Skipping malformed geometry at " + uri + "...");
                }
            }
        }
        return gMap;
    }

    public static AMapping getMapping(ACache source, ACache target, String sourceVar, String targetVar,
                                      String expression, double threshold, String relation, int numThreads) {
        if (threshold <= 0) {
            throw new InvalidThresholdException(threshold);
        }
        //System.out.println("RADON is here");
        List<String> properties = PropertyFetcher.getProperties(expression, threshold);

        Map<String, Geometry> sourceMap = getGeometryMapFromCache(source, properties.get(0));

        Map<String, Geometry> targetMap = getGeometryMapFromCache(target, properties.get(1));
        //	System.out.println("RADON is still here "+targetMap.toString());
        return getMapping(sourceMap, targetMap, relation, numThreads);
    }

		/*public static AMapping getMapping(Set<Polygon> sourceData, Set<Polygon> targetData, String relation) {
			Map<String, Geometry> source, target;
			source = new HashMap<>();
			target = new HashMap<>();
			for (Polygon polygon : sourceData) {
				try {
					source.put(polygon.uri, polygon.getGeometry());
				} catch (ParseException e) {
					//logger.warn("Skipping malformed geometry at " + polygon.uri + "...");
				}
			}
			for (Polygon polygon : targetData) {
				try {
					target.put(polygon.uri, polygon.getGeometry());
				} catch (ParseException e) {
					//logger.warn("Skipping malformed geometry at " + polygon.uri + "...");
				}
			}
			return getMapping(source, target, relation);
		}*/

    public static AMapping getMapping(Map<String, Geometry> sourceData, Map<String, Geometry> targetData,
                                      String relation, int numThreads) {
        String rel = relation;

        // execute matching
        ExecutorService matchExec = Executors.newFixedThreadPool(numThreads);
        ExecutorService mergerExec = Executors.newFixedThreadPool(1);
        AMapping m = MappingFactory.createDefaultMapping();
        List<Map<String, Set<String>>> results = Collections.synchronizedList(new ArrayList<>());
        Map<String, Set<String>> computed = new HashMap<>();
        ContentSimilarityAreaNoIndexing.Matcher matcher = new ContentSimilarityAreaNoIndexing.Matcher(rel, results);

        for (Map.Entry<String, Geometry> sourceEntry : sourceData.entrySet()) {
            for (Map.Entry<String, Geometry> targetEntry : targetData.entrySet()) {
                if (numThreads == 1) {
                    if (ContentSimilarityAreaNoIndexing.Matcher.relate(sourceEntry.getValue(), targetEntry.getValue(), rel)) {
                        m.add(sourceEntry.getKey(), targetEntry.getKey(), 1.0);
                    }
                } else {
                    matcher.schedule(sourceEntry, targetEntry);
                    if (matcher.size() == ContentSimilarityAreaNoIndexing.Matcher.maxSize) {
                        matchExec.execute(matcher);
                        matcher = new ContentSimilarityAreaNoIndexing.Matcher(rel, results);
                        if (results.size() > 0) {
                            mergerExec.execute(new ContentSimilarityAreaNoIndexing.Merger(results, m));
                        }
                    }
                }
            }
        }

        if (numThreads > 1) {
            if (matcher.size() > 0) {
                matchExec.execute(matcher);
            }
            matchExec.shutdown();
            while (!matchExec.isTerminated()) {
                try {
                    if (results.size() > 0) {
                        mergerExec.execute(new ContentSimilarityAreaNoIndexing.Merger(results, m));
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (results.size() > 0) {
                mergerExec.execute(new ContentSimilarityAreaNoIndexing.Merger(results, m));
            }
            mergerExec.shutdown();
            while (!mergerExec.isTerminated()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return m;
    }

}
