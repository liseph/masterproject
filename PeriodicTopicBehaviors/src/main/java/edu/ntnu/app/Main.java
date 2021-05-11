package edu.ntnu.app;

public class Main {

    public static void main(String[] args) {

        try {
            int algorithm = Integer.parseInt(args[0]);
            String in = args[1];
            String out = args[2];

            switch (algorithm) {
                case 0: {
                    System.out.println("Selected GeoLPTA.");
                    edu.ntnu.app.lpta.Main.execute(in, out);
                }
                case 1: {
                    System.out.println("Selected TopicPeriodica.");
                    edu.ntnu.app.periodica.Main.execute(in, out);
                }
                case 0: {
                    System.out.println("Selected PSTA+.");
                    edu.ntnu.app.psta.Main.execute(in, out);
                }
            }


        } catch (Exception e) {
            System.out.println("Something went wrong...");
            e.printStackTrace();
        }

    }
}
