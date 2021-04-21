package edu.ntnu.app.periodica;

import java.util.*;
import java.util.stream.IntStream;

public class ReferenceSpot {
    private static int LONGITUDE = 0;
    private static int LATITUDE = 1;
    private static int GRANULARITY = 1000;
    private final List<int[]> rows;

    private ReferenceSpot(List<int[]> rows) {
        this.rows = rows;
    }

    public static ReferenceSpot[] findReferenceSpots() {
        Float[][] points = PeriodicaDocs.getXYValues();

        float xStart = Float.POSITIVE_INFINITY;
        float xEnd = Float.NEGATIVE_INFINITY;
        float yStart = Float.POSITIVE_INFINITY;
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
        float gamma = calculateGamma(points);
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                densities[i][j] = calcDensityEstimate(xStart + ((float)i)/GRANULARITY, yStart + ((float)j)/GRANULARITY, points, gamma);
            }
        }

        // find top 15% density values
        // TODO: Use quickselect to do this?
        int nVals = (int) (0.15 * points.length);
        PriorityQueue<Float> maxHeap = new PriorityQueue<>();
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                maxHeap.add(densities[i][j]);
                if (maxHeap.size() > nVals) maxHeap.poll();
            }
        }
        float lowestVal = maxHeap.peek();
        // Find contours

        Map<Integer, List<int[]>> clusterCols = new HashMap<>();
        for (int i = 0; i < x; i++) {
            int latestJ = -2;
            clusterCols.put(i, new ArrayList<>());
            List<int[]> tmp = clusterCols.get(i);
            for (int j = 0; j < y; j++) {
                if (densities[i][j] > lowestVal) {
                    if (latestJ == j - 1) {
                        // add to same cluster as previous
                        tmp.get(tmp.size()-1)[2] = j;
                    } else {
                        // create new cluster
                        tmp.add(new int[]{i, j, j});
                    }
                    latestJ = j;
                }
            }
        }

        List<List<int[]>> clusters = new ArrayList<>();
        clusterCols.forEach((rowIndex, cols) -> {
            List<int[]> nextRowCols = clusterCols.get(rowIndex + 1);
            List<int[]> cluster;
            if (nextRowCols != null) {
                // Check if they align vertically as well
                for (int[] col : cols) {
                    cluster = new ArrayList<>();
                    cluster.add(col);
                    for (int[] nextRowCol : nextRowCols) {
                        int colStart = col[1]; // for readability
                        int colEnd = col[2];
                        int nextRowColStart = nextRowCol[1];
                        int nextRowColEnd = nextRowCol[2];
                        if (!(colEnd < nextRowColStart || colStart > nextRowColEnd)) {
                            // overlap, add to cluster and remove from list to avoid looping over it again next iteration
                            // But then we won't merge more than two rows...
                            // Move into function and then call recursively. Do that tomorrow.
                            cluster.add(nextRowCol);
                            nextRowCols.remove(nextRowCol);
                        }
                    }
                }
            } else {
                // cols cannot be merged more, add as singleton clusters
                for (int[] col : cols) {
                    cluster = new ArrayList<>();
                    cluster.add(col);
                    clusters.add(cluster);
                }
            }
        });

        return clusters.stream().map(c -> new ReferenceSpot(c)).toArray(ReferenceSpot[]::new);
    }

    private static float calcDensityEstimate(float longC, float latC, Float[][] points, float gamma) {
        int n = points.length;
        return (float) (1 / (n * sqr(gamma)) *
                IntStream.range(0, n)
                        .mapToDouble(i -> 1 / (2 * Math.PI) * Math.exp(-calcSquaredDist(longC, latC, points[i]) / 2 * sqr(gamma)))
                        .sum());
    }

    // Calculate the squared distance from the middle of a cell to a point.
    private static float calcSquaredDist(float longC, float latC, Float[] point) {
        return sqr(longC + 0.5f - point[LONGITUDE]) + sqr(latC + 0.5f - point[LATITUDE]);
    }

    private static float calculateGamma(Float[][] points) {
        // Calculate standard deviation
        int n = points.length;
        float meanX = 0f;
        float meanY = 0f;
        for (int i = 0; i < n; i++) {
            meanX += points[i][LONGITUDE];
            meanY += points[i][LATITUDE];
        }
        meanX = meanX / n;
        meanY = meanY / n;

        Float sigmaX = 0f;
        Float sigmaY = 0f;
        for (int i = 0; i < n; i++) {
            sigmaX += sqr(points[i][LONGITUDE] - meanX);
            sigmaY += sqr(points[i][LATITUDE] - meanY);
        }
        sigmaX = (float) Math.sqrt(sigmaX / n);
        sigmaY = (float) Math.sqrt(sigmaY / n);
        return (float) (0.5 * Math.sqrt(sqr(sigmaX) + sqr(sigmaY)) * Math.pow(n, -1 / 6));
    }

    private static Float sqr(float v) {
        return v * v;
    }
}
