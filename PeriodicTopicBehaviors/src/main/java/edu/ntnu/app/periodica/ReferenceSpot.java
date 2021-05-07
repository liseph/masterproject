package edu.ntnu.app.periodica;

import edu.ntnu.app.Location;

import java.util.*;
import java.util.stream.IntStream;

public class ReferenceSpot {
    private static final int LONGITUDE = 0;
    private static final int LATITUDE = 1;
    private static final int GRANULARITY = 1; // The higher the value, the more cells
    private static final double DENSITYTHRESHOLD = 0.15;
    private static int idCount = 1;
    private static double xStart;
    private static double yStart;

    private final int id;
    private final List<Cell> cells;
    private int counter = 0;

    private ReferenceSpot(int i, int j) {
        this.id = idCount++;
        cells = new ArrayList<>();
        cells.add(new Cell(i, j));
    }

    // Called to create a reference spot signifying all areas not covered by the reference spots.
    private ReferenceSpot() {
        this.id = 0;
        cells = null;
    }

    public static ReferenceSpot[] findReferenceSpots() {
        double[][] points = PeriodicaDocs.getXYValues();
        int n = points.length;

        xStart = Double.POSITIVE_INFINITY;
        double xEnd = Double.NEGATIVE_INFINITY;
        yStart = Double.POSITIVE_INFINITY;
        double yEnd = Double.NEGATIVE_INFINITY;
        for (double[] point : points) {
            double lo = point[LONGITUDE];
            double la = point[LATITUDE];
            if (lo < xStart) xStart = lo;
            else if (lo > xEnd) xEnd = lo;
            if (la < yStart) yStart = la;
            else if (la > yEnd) yEnd = la;
        }

        int x = (int) (GRANULARITY * (xEnd - xStart));
        int y = (int) (GRANULARITY * (yEnd - yStart));

        Double[][] densities = new Double[x][y];
        double gamma = calculateGamma(points, n);
        int nVals = (int) (DENSITYTHRESHOLD * x * y); // Find top 15% density threshold
        PriorityQueue<Double> maxHeap = new PriorityQueue<>(); // TODO: Use quickselect to do this?
        IntStream.range(0, x).forEach(i -> {
            densities[i] = IntStream
                    .range(0, y)
                    .mapToDouble(j -> calcDensityEstimate(xStart + ((double) i) / GRANULARITY, yStart + ((double) j) / GRANULARITY, points, n, gamma))
                    .boxed()
                    .toArray(Double[]::new);
            Collections.addAll(maxHeap, densities[i]);
            while (maxHeap.size() > nVals) maxHeap.poll();
        });

        double lowestVal = maxHeap.peek();
        // Find clusters of values with density above threshold. Once we find one cell, we create a cluster and
        // find all cells that belong to that cluster.
        List<ReferenceSpot> spots = new ArrayList<>();
        long start = System.nanoTime();
        IntStream.range(0, x).forEach(i -> {
            IntStream.range(0, y).forEach(j -> {
                if (densities[i][j] > lowestVal && spots.stream().noneMatch(spot -> spot.containsCell(i, j))) {
                    // Find cluster
                    ReferenceSpot spot = new ReferenceSpot(i, j);
                    Cell c = spot.getNext();
                    while (c != null) {
                        int tmpi = c.i;
                        int tmpj = c.j;
                        if (tmpj > 0 && densities[tmpi][tmpj - 1] > lowestVal && spots.stream().noneMatch(s -> s.containsCell(tmpi, tmpj - 1)))
                            spot.addCellIfNotExists(tmpi, tmpj - 1);
                        if (tmpj < y - 1 && densities[tmpi][tmpj + 1] > lowestVal && spots.stream().noneMatch(s -> s.containsCell(tmpi, tmpj + 1)))
                            spot.addCellIfNotExists(tmpi, tmpj + 1);
                        if (tmpi < x - 1 && densities[tmpi + 1][tmpj] > lowestVal && spots.stream().noneMatch(s -> s.containsCell(tmpi + 1, tmpj)))
                            spot.addCellIfNotExists(tmpi + 1, tmpj);
                        c = spot.getNext();
                    }
                    spots.add(spot);
                }
            });
        });
        long tot = System.nanoTime() - start;
        // Add a reference spot that indicates all areas outside the reference spots, will have id=0.
        spots.add(0, new ReferenceSpot());
        return spots.toArray(ReferenceSpot[]::new);
    }

    private static double calcDensityEstimate(double longC, double latC, double[][] points, int n, double gamma) {
        double C1 = 1.0 / (n * sqr(gamma));
        double C2 = 1.0 / (2 * Math.PI);
        double C3 = 2 * sqr(gamma);
        double func = IntStream.range(0, n)
                .mapToDouble(i -> C2 * Math.exp(-calcSquaredDist(longC, latC, points[i]) / C3))
                .sum();
        return C1 * func;
    }

    // Calculate the squared distance between a cell and a point
    private static double calcSquaredDist(double longC, double latC, double[] point) {
        // Add 0.5 to calculate the distance from the middle of the cell
        return sqr(0.5 + (int) longC - point[LONGITUDE]) + sqr(0.5 + (int) latC - point[LATITUDE]);
    }

    private static double calculateGamma(double[][] points, int n) {
        // Calculate standard deviation
        double meanX = 0;
        double meanY = 0;
        for (int i = 0; i < n; i++) {
            meanX += points[i][LONGITUDE];
            meanY += points[i][LATITUDE];
        }
        meanX = meanX / n;
        meanY = meanY / n;
        double sigmaX = 0;
        double sigmaY = 0;
        for (int i = 0; i < n; i++) {
            sigmaX += sqr(points[i][LONGITUDE] - meanX);
            sigmaY += sqr(points[i][LATITUDE] - meanY);
        }
        sigmaX = Math.sqrt(sigmaX / n);
        sigmaY = Math.sqrt(sigmaY / n);
        return 0.5 * Math.sqrt(sqr(sigmaX) + sqr(sigmaY)) * Math.pow(n, -1.0 / 6);
    }

    private static double sqr(double v) {
        return v * v;
    }

    public int getId() {
        return id;
    }

    private void addCellIfNotExists(int i, int j) {
        Cell c = new Cell(i, j);
        if (!cells.contains(c)) {
            cells.add(c);
        }
    }

    private boolean containsCell(int i, int j) {
        return cells.contains(new Cell(i, j));
    }

    // Returns whether a point is contained in this reference point. For id=0, this is always true as this method is
    // called for id=0 after it has been called for all other reference points.
    public boolean containsPoint(double longitude, double latitude) {
        if (id != 0) {
            int lo = (int) ((longitude - xStart) / GRANULARITY);
            int la = (int) ((latitude - yStart) / GRANULARITY);
            return containsCell(lo, la);
        }
        return true;
    }

    private Cell getNext() {
        if (counter < cells.size())
            return cells.get(counter++);
        counter = 0;
        return null;
    }

    // TODO: Finish this function to be able to debug if the ref spots are correct.
    public List<Location> getLocationsInRefSpot() {
        List<Location> result = new ArrayList<>();
        if (id != 0) {
            for (Location l : PeriodicaDocs.getLocations()) {
                if (containsPoint(l.getLongitude(), l.getLatitude())) {
                    result.add(l);
                }
            }
        }
        return result;
    }

    class Cell {
        final int i;
        final int j;

        public Cell(int i, int j) {
            this.i = i;
            this.j = j;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cell cell = (Cell) o;
            return i == cell.i && j == cell.j;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, j);
        }

        @Override
        public String toString() {
            return "{i=" + i +
                    ", j=" + j +
                    '}';
        }
    }
}

