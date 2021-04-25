package edu.ntnu.app.periodica;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.stream.IntStream;

public class ReferenceSpot {
    private static final int LONGITUDE = 0;
    private static final int LATITUDE = 1;
    private static final int GRANULARITY = 1;
    private static final double DENSITYTHRESHOLD = 0.15;
    private static int idCount = 1;
    private static float xStart;
    private static float yStart;

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
        Float[][] points = PeriodicaDocs.getXYValues();
        int n = points.length;

        xStart = Float.POSITIVE_INFINITY;
        float xEnd = Float.NEGATIVE_INFINITY;
        yStart = Float.POSITIVE_INFINITY;
        float yEnd = Float.NEGATIVE_INFINITY;
        for (Float[] point : points) {
            float lo = point[LONGITUDE];
            float la = point[LATITUDE];
            if (lo < xStart) xStart = lo;
            else if (lo > xEnd) xEnd = lo;
            if (la < yStart) yStart = la;
            else if (la > yEnd) yEnd = la;
        }

        int x = (int) (GRANULARITY * (xEnd - xStart));
        int y = (int) (GRANULARITY * (yEnd - yStart));

        float[][] densities = new float[x][y];
        float gamma = calculateGamma(points, n);
        int nVals = (int) (DENSITYTHRESHOLD * n); // Find top 15% density threshold
        PriorityQueue<Float> maxHeap = new PriorityQueue<>(); // TODO: Use quickselect to do this?
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                densities[i][j] = calcDensityEstimate(xStart + ((float) i) / GRANULARITY, yStart + ((float) j) / GRANULARITY, points, n, gamma);
                maxHeap.add(densities[i][j]);
                if (maxHeap.size() > nVals) maxHeap.poll();
            }
        }

        float lowestVal = maxHeap.peek();
        // Find clusters of values with density above threshold. Once we find one cell, we create a cluster and find
        // find all cells that belong to that cluster.
        List<ReferenceSpot> spots = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                int finalI = i;
                int finalJ = j;
                if (densities[i][j] > lowestVal && spots.stream().noneMatch(spot -> spot.containsCell(finalI, finalJ))) {
                    // Find cluster
                    ReferenceSpot spot = new ReferenceSpot(i, j);
                    int tmpi;
                    int tmpj;
                    Cell c = spot.getNext();
                    while (c != null) {
                        tmpi = c.i;
                        tmpj = c.j;
                        if (densities[tmpi][tmpj - 1] > lowestVal) spot.addCellIfNotExists(tmpi, tmpj - 1);
                        if (densities[tmpi][tmpj + 1] > lowestVal) spot.addCellIfNotExists(tmpi, tmpj + 1);
                        if (densities[tmpi + 1][tmpj] > lowestVal) spot.addCellIfNotExists(tmpi + 1, tmpj);
                        c = spot.getNext();
                    }
                    spots.add(spot);
                }
            }
        }
        // Add a reference spot that indicates all areas outside the reference spots, will have id=0.
        spots.add(new ReferenceSpot());
        return spots.toArray(ReferenceSpot[]::new);
    }

    private static float calcDensityEstimate(float longC, float latC, Float[][] points, int n, float gamma) {
        float C1 = 1 / (n * sqr(gamma));
        float C2 = (float) (1 / (2 * Math.PI));
        float func = IntStream.range(0, n)
                .mapToObj(i -> C2 * (float) Math.exp(-calcSquaredDist(longC, latC, points[i]) / (2 * sqr(gamma))))
                .reduce(0f, Float::sum);
        return C1 * func;
    }

    // Calculate the squared distance from the middle of a cell to a point.
    private static float calcSquaredDist(float longC, float latC, Float[] point) {
        return sqr(longC + 0.5f - point[LONGITUDE]) + sqr(latC + 0.5f - point[LATITUDE]);
    }

    private static float calculateGamma(Float[][] points, int n) {
        // Calculate standard deviation
        float meanX = 0f;
        float meanY = 0f;
        for (int i = 0; i < n; i++) {
            meanX += points[i][LONGITUDE];
            meanY += points[i][LATITUDE];
        }
        meanX = meanX / n;
        meanY = meanY / n;
        float sigmaX = 0f;
        float sigmaY = 0f;
        for (int i = 0; i < n; i++) {
            sigmaX += sqr(points[i][LONGITUDE] - meanX);
            sigmaY += sqr(points[i][LATITUDE] - meanY);
        }
        sigmaX = (float) Math.sqrt(sigmaX / n);
        sigmaY = (float) Math.sqrt(sigmaY / n);
        return (float) (0.5 * Math.sqrt(sqr(sigmaX) + sqr(sigmaY)) * Math.pow(n, -1f / 6));
    }

    private static Float sqr(float v) {
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
    public boolean containsPoint(float longitude, float latitude) {
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
