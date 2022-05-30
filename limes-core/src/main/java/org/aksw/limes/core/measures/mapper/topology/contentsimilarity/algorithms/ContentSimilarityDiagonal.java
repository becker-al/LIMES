package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.algorithms;

import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.topology.contentsimilarity.ContentMeasure;
import org.locationtech.jts.geom.Envelope;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ContentSimilarityDiagonal {


    public static class GridSizeHeuristics {

        public final static String AVG = "avg";
        public final static String MIN = "min";
        public final static String MAX = "max";
        public final static String MED = "median";
        public static boolean swap = false;

        public static double[] decideForTheta(GridSizeHeuristics s, GridSizeHeuristics t, String measure) {
            double[] stats;
            switch (measure) {
                case MAX:
                    stats = new double[]{s.maxX, s.maxY, t.maxX, t.maxY};
                    break;
                case AVG:
                    stats = new double[]{s.avgX, s.avgY, t.avgX, t.avgY};
                    break;
                case MED:
                    stats = new double[]{s.medX, s.medY, t.medX, t.medY};
                    break;
                case MIN:
                default:
                    stats = new double[]{s.minX, s.minY, t.minX, t.minY};
            }
            double estAreaS = stats[0] * stats[1] * s.size;
            double estAreaT = stats[2] * stats[3] * t.size;
            // we want to swap towards the smallest area coverage to optimize the
            // number of comparisons
            swap = estAreaS > estAreaT;
            return new double[]{(2.0d) / (stats[0] + stats[2]), (2.0d) / (stats[1] + stats[3])};
        }

        private double size;
        private double minX;
        private double maxX;
        private double avgX;
        private double medX;
        private double minY;
        private double maxY;
        private double avgY;
        private double medY;

        public GridSizeHeuristics(Collection<Envelope> input) {
            double[] x = new double[input.size()];
            double[] y = new double[input.size()];
            int i = 0;
            for (Envelope e : input) {
                y[i] = e.getHeight();
                x[i] = e.getWidth();
                i++;
            }
            this.size = input.size();
            Arrays.sort(x);
            this.minX = x[0];
            this.maxX = x[x.length - 1];
            this.avgX = Arrays.stream(x).average().getAsDouble();
            this.medX = x.length % 2 == 0 ? (x[x.length / 2 - 1] + x[x.length / 2]) / 2.0d : x[x.length / 2];
            Arrays.sort(y);
            this.minY = y[0];
            this.maxY = y[y.length - 1];
            this.avgY = Arrays.stream(y).average().getAsDouble();
            this.medY = y.length % 2 == 0 ? (y[y.length / 2 - 1] + y[y.length / 2]) / 2.0d : y[y.length / 2];
        }

        public double getSize() {
            return size;
        }

        public double getMinX() {
            return minX;
        }

        public double getMaxX() {
            return maxX;
        }

        public double getAvgX() {
            return avgX;
        }

        public double getMedX() {
            return medX;
        }

        public double getMinY() {
            return minY;
        }

        public double getMaxY() {
            return maxY;
        }

        public double getAvgY() {
            return avgY;
        }

        public double getMedY() {
            return medY;
        }

        public String toString() {
            DecimalFormat df = new DecimalFormat("0.0000");
            return "[MIN(" + df.format(minX) + ";" + df.format(minY) + ");MAX(" + df.format(maxX) + ";"
                    + df.format(maxY) + ";AVG(" + df.format(avgX) + ";" + df.format(avgY) + ");MED(" + df.format(medX)
                    + ";" + df.format(medY) + ")]";
        }

    }

    public static class MBBIndex {

        public int lat1, lat2, lon1, lon2;
        public Envelope polygon;
        private String uri;
        private String origin_uri;

        public MBBIndex(int lat1, int lon1, int lat2, int lon2, Envelope polygon, String uri) {
            this.lat1 = lat1;
            this.lat2 = lat2;
            this.lon1 = lon1;
            this.lon2 = lon2;
            this.polygon = polygon;
            this.uri = uri;
            this.origin_uri = uri;
        }

        public MBBIndex(int lat1, int lon1, int lat2, int lon2, Envelope polygon, String uri, String origin_uri) {
            this.lat1 = lat1;
            this.lat2 = lat2;
            this.lon1 = lon1;
            this.lon2 = lon2;
            this.polygon = polygon;
            this.uri = uri;
            this.origin_uri = origin_uri;
        }

        public boolean contains(MBBIndex i) {
            return this.lat1 <= i.lat1 && this.lon1 <= i.lon1 && this.lon2 >= i.lon2 && this.lat2 >= i.lat2;
        }

        public boolean covers(MBBIndex i) {
            return this.lat1 <= i.lat1 && this.lon1 <= i.lon1 && this.lon2 >= i.lon2 && this.lat2 >= i.lat2;
        }

        public boolean intersects(MBBIndex i) {
            return !this.disjoint(i);
        }

        public boolean disjoint(MBBIndex i) {
            return this.lat2 < i.lat1 || this.lat1 > i.lat2 || this.lon2 < i.lon1 || this.lon1 > i.lon2;
        }

        public boolean equals(Object o) {
            if (!(o instanceof MBBIndex)) {
                return false;
            }
            MBBIndex i = ((MBBIndex) o);
            return lat1 == i.lat1 && lat2 == i.lat2 && lon1 == i.lon1 && lon2 == i.lon2;
        }

    }

    public static class SquareIndex {

        public HashMap<Integer, HashMap<Integer, List<MBBIndex>>> map = new HashMap<>();

        public SquareIndex() {

        }

        public SquareIndex(int capacity) {
            this.map = new HashMap<>(capacity);
        }

        public void add(int i, int j, MBBIndex m) {
            if (!map.containsKey(i)) {
                map.put(i, new HashMap<>());
            }
            if (!map.get(i).containsKey(j)) {
                map.get(i).put(j, new ArrayList<>());
            }
            map.get(i).get(j).add(m);
        }

        public List<MBBIndex> getSquare(int i, int j) {
            if (!map.containsKey(i) || !map.get(i).containsKey(j))
                return null;
            else
                return map.get(i).get(j);
        }
    }

    public static class Matcher implements Runnable {

        public static int maxSize = 1000;
        private String relation;
        private final List<Map<String, Set<String>>> result;
        private List<MBBIndex> scheduled;

        public Matcher(String relation, List<Map<String, Set<String>>> result) {
            this.relation = relation;
            this.result = result;
            this.scheduled = new ArrayList<>();
        }

        @Override
        public void run() {
            Map<String, Set<String>> temp = new HashMap<>();
            for (int i = 0; i < scheduled.size(); i += 2) {
                MBBIndex s = scheduled.get(i);
                MBBIndex t = scheduled.get(i + 1);
                if (relate(s.polygon, t.polygon, relation)) {
                    if (!temp.containsKey(s.origin_uri)) {
                        temp.put(s.origin_uri, new HashSet<>());
                    }
                    temp.get(s.origin_uri).add(t.origin_uri);
                }
            }
            synchronized (result) {
                result.add(temp);
            }
        }

        public void schedule(MBBIndex s, MBBIndex t) {
            scheduled.add(s);
            scheduled.add(t);
        }

        public int size() {
            return scheduled.size();
        }

        public static boolean relate(Envelope mbrA, Envelope mbrB, String relation) {
            double X = ContentMeasure.fD(mbrA, mbrB);
            double Y = ContentMeasure.fD(mbrB, mbrA);
            double Z = X + Y;

            return relate(X, Y, Z, relation);
        }

        public static boolean relate(double X, double Y, double Z, String relation) {
            switch (relation) {
                case EQUALS:
                    if (X == 1 && Y == 1 && Z == 2) {
                        return true;
                    } else {
                        return false;
                    }
                case DISJOINT:
                    if (0 < X && X < 1 && 0 < Y && Y < 1 && 0 < Z && Z < 2) {
                        return true;
                    } else {
                        return false;
                    }
                case INTERSECTS:
                    if (!(0 < X && X < 1 && 0 < Y && Y < 1 && 0 < Z && Z < 2)) {
                        return true;
                    } else {
                        return false;
                    }
                case TOUCHES: //meet
                    if (0 < X && X < 1 && 0 < Y && Y < 1 && 1 <= Z && Z <= 2) {
                        return true;
                    } else {
                        return false;
                    }
                case WITHIN://inside
                case COVEREDBY:
                    if (0 < X && X < 1 && Y == 1 && 1 < Z && Z < 2) {
                        return true;
                    } else {
                        return relate(X, Y, Z, EQUALS);
                    }
                case CONTAINS:
                case COVERS:
                    if (X == 1 && 0 < Y && Y < 1 && 1 < Z && Z < 2) {
                        return true;
                    } else {
                        return relate(X, Y, Z, EQUALS);
                    }
                case OVERLAPS:
                    if (0 < X && X < 1 && 0 < Y && Y < 1 && 1 < Z && Z < 2) {
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
                        if (GridSizeHeuristics.swap)
                            m.add(t, s, 1.0d);
                        else
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

    public static AMapping getMapping(Map<String, Envelope> sourceData, Map<String, Envelope> targetData,
                                      String relation, int numThreads) {
        double thetaX, thetaY;

        // Relation thats actually used for computation.
        // Might differ from input relation when swapping occurs or the input
        // relation is 'disjoint'.
        String rel = relation;

        // When relation for AMapping M is 'disjoint' we compute AMapping M'
        // relation 'intersects'
        // and return M = (S x T) \ M'
        boolean disjointStrategy = rel.equals(DISJOINT);
        if (disjointStrategy)
            rel = INTERSECTS;

        GridSizeHeuristics heuristicsS = new GridSizeHeuristics(sourceData.values());
        GridSizeHeuristics heuristicsT = new GridSizeHeuristics(targetData.values());
        double[] theta = GridSizeHeuristics.decideForTheta(heuristicsS, heuristicsT, heuristicStatMeasure);
        thetaX = theta[0];
        thetaY = theta[1];
        // swap smaller dataset to source
        // if swap is necessary is decided in Stats.decideForTheta([...])!
        Map<String, Envelope> swap;
        boolean swapped = GridSizeHeuristics.swap;
        if (swapped) {
            swap = sourceData;
            sourceData = targetData;
            targetData = swap;
            switch (rel) {
                case WITHIN:
                    rel = CONTAINS;
                    break;
                case CONTAINS:
                    rel = WITHIN;
                    break;
                case COVERS:
                    rel = COVEREDBY;
                    break;
                case COVEREDBY:
                    rel = COVERS;
                    break;
            }
        }

        // set up indexes
        SquareIndex sourceIndex = index(sourceData, null, thetaX, thetaY);
        SquareIndex targetIndex = index(targetData, sourceIndex, thetaX, thetaY);

        // execute matching
        ExecutorService matchExec = Executors.newFixedThreadPool(numThreads);
        ExecutorService mergerExec = Executors.newFixedThreadPool(1);
        AMapping m = MappingFactory.createDefaultMapping();
        List<Map<String, Set<String>>> results = Collections.synchronizedList(new ArrayList<>());
        Map<String, Set<String>> computed = new HashMap<>();
        Matcher matcher = new Matcher(rel, results);

        for (Integer lat : sourceIndex.map.keySet()) {
            for (Integer lon : sourceIndex.map.get(lat).keySet()) {
                List<MBBIndex> source = sourceIndex.getSquare(lat, lon);
                //System.out.println("RAD source: "+source.toString());
                List<MBBIndex> target = targetIndex.getSquare(lat, lon);
                //System.out.println("RAD target: "+target.toString());
                if (target != null && target.size() > 0) {
                    for (MBBIndex a : source) {
                        //	System.out.println("RADON jumped there 12");
                        if (!computed.containsKey(a.uri))
                            computed.put(a.uri, new HashSet<>());
                        for (MBBIndex b : target) {
                            if (!computed.get(a.uri).contains(b.uri)) {
                                computed.get(a.uri).add(b.uri);
                                //System.out.println(" the new relation is: "+rel);
                                if (numThreads == 1) {

                                    if (Matcher.relate(a.polygon, b.polygon, rel)) {
                                        if (swapped)
                                            m.add(b.origin_uri, a.origin_uri, 1.0);
                                        else
                                            m.add(a.origin_uri, b.origin_uri, 1.0);
                                    }
                                } else {
                                    matcher.schedule(a, b);
                                    if (matcher.size() == Matcher.maxSize) {
                                        matchExec.execute(matcher);
                                        matcher = new Matcher(rel, results);
                                        if (results.size() > 0) {
                                            mergerExec.execute(new Merger(results, m));
                                        }
                                    }
                                }
                            }
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
                        mergerExec.execute(new Merger(results, m));
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (results.size() > 0) {
                mergerExec.execute(new Merger(results, m));
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
        //System.out.println("RADON jumped there 1");
        // Compute M = (S x T) \ M' for disjoint relation
        if (disjointStrategy) {
            AMapping disjoint = MappingFactory.createDefaultMapping();
            for (String s : sourceData.keySet()) {
                for (String t : targetData.keySet()) {
                    if (swapped) {
                        if (!m.contains(t, s)) {
                            disjoint.add(t, s, 1.0d);
                        }
                    } else {
                        if (!m.contains(s, t)) {
                            disjoint.add(s, t, 1.0d);
                        }
                    }
                }
            }
            m = disjoint;
        }
        //System.out.println("the size of RADON "+ m.getSize());
        return m;
    }

    public static SquareIndex index(Map<String, Envelope> input, SquareIndex extIndex, double thetaX, double thetaY) {
        SquareIndex result = new SquareIndex();

        for (String p : input.keySet()) {
            Envelope g = input.get(p);

            int minLatIndex = (int) Math.floor(g.getMinY() * thetaY);
            int maxLatIndex = (int) Math.ceil(g.getMaxY() * thetaY);
            int minLongIndex = (int) Math.floor(g.getMinX() * thetaX);
            int maxLongIndex = (int) Math.ceil(g.getMaxX() * thetaX);

            // Check for passing over 180th meridian. In case its shorter to
            // pass over it, we assume that is what is
            // meant by the user and we split the geometry into one part east
            // and one part west of 180th meridian.

            if (minLongIndex < (int) Math.floor(-90d * thetaX) && maxLongIndex > (int) Math.ceil(90d * thetaX)) {
                MBBIndex westernPart = new MBBIndex(minLatIndex, (int) Math.floor(-180d * thetaX), maxLatIndex,
                        minLongIndex, g, p + "<}W", p);
                addToIndex(westernPart, result, extIndex);
                MBBIndex easternPart = new MBBIndex(minLatIndex, maxLongIndex, maxLatIndex,
                        (int) Math.ceil(180 * thetaX), g, p + "<}E", p);
                addToIndex(easternPart, result, extIndex);
            } else {
                MBBIndex mbbIndex = new MBBIndex(minLatIndex, minLongIndex, maxLatIndex, maxLongIndex, g, p);
                addToIndex(mbbIndex, result, extIndex);
            }

        }
        return result;
    }

    private static void addToIndex(MBBIndex mbbIndex, SquareIndex result, SquareIndex extIndex) {
        if (extIndex == null) {
            for (int latIndex = mbbIndex.lat1; latIndex <= mbbIndex.lat2; latIndex++) {
                for (int longIndex = mbbIndex.lon1; longIndex <= mbbIndex.lon2; longIndex++) {
                    result.add(latIndex, longIndex, mbbIndex);
                }
            }
        } else {
            for (int latIndex = mbbIndex.lat1; latIndex <= mbbIndex.lat2; latIndex++) {
                for (int longIndex = mbbIndex.lon1; longIndex <= mbbIndex.lon2; longIndex++) {
                    if (extIndex.getSquare(latIndex, longIndex) != null)
                        result.add(latIndex, longIndex, mbbIndex);
                }
            }
        }
    }
}
