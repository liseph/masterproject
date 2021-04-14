package edu.ntnu.app.psta;

public class Main {

    public static void main(String[] args) {

        Document d1 = new Document(0,0, 0, new String[]{"hei"});
        Document d2 = new Document(0,1, 1, new String[]{"hei", "hopp"});
        Document d3 = new Document(1,0, 1, new String[]{"sann"});
        Document d4 = new Document(1,1, 2, new String[]{"jeg", "er", "kul"});
        Document d5 = new Document(2,0, 3, new String[]{"hei", "kul"});

        Documents docs = new Documents(new Document[]{d1, d2, d3, d4, d5});

        PstaPattern pattern = PSTA.execute(docs, 2);
    }
}
