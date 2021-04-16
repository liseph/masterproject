package edu.ntnu.app.psta;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        Docs.initialize("../dataset1000.txt");

        PstaPattern pattern = PSTA.execute( 2);
        pattern.writeToFile();
    }
}
