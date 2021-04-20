package edu.ntnu.app.autoperiod;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {

        Main m = new Main();
        Float[] data;
        try {
            data = m.readFile("../sinewave.txt");
        } catch (IOException | URISyntaxException | NullPointerException e) {
            System.out.println("File read failed, fallback to dummy data.");
            System.out.println(e);
            data = new Float[1000];
            Arrays.fill(data, 0);
            for (int i = 0; i < 1000; i += 220) {
                data[i] = 1f;
                data[i + 1] = 1f;
            }
        }

        Timeseries ts = new Timeseries(data, 500);
        Float[] periods = FindPeriodsInTimeseries.execute(ts);
        System.out.print("p=[");
        for (double d : periods) {
            System.out.print(d);
            System.out.print(",");
        }
        System.out.println("]");
    }

    public Float[] readFile(String pathName) throws IOException, URISyntaxException, NullPointerException {
        Path path = Paths.get(pathName);
        Stream<String> lines = Files.lines(path);
        Float[] data = lines.map(d -> Float.parseFloat(d)).toArray(Float[]::new);
        lines.close();
        return data;
    }


}
