package edu.ntnu.app.periodica;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Location;

import java.io.IOException;
import java.util.Arrays;

public class PeriodicaDocs extends Docs {

    public static void initialize(String pathName) throws IOException {
        Docs.initialize(pathName);
    }

    public static Float[][] getXYValues() {
        Location[] locations = Docs.getLocations();
        return Arrays.stream(docs)
                .map(d -> d.getLocationId())
                .map(locId -> locations[locId])
                .map(loc -> new Float[]{loc.getLongitude(), loc.getLatitude()})
                .toArray(Float[][]::new);
    }
}
