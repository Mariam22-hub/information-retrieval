/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package invertedIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author ehab
 */
public class Test {

    public static void main(String args[]) throws IOException {
        Index5 biwordIndex = new Index5();
        Index5 positionalIndex = new Index5();
        String files = "tmp11/rl/collection/";

        File file = new File(files);

        String[] fileList = file.list();

        fileList = biwordIndex.sort(fileList);

        biwordIndex.N = fileList.length;
        positionalIndex.N = fileList.length;

        for (int i = 0; i < fileList.length; i++) {
            fileList[i] = files + fileList[i];
        }
        biwordIndex.buildIndex(fileList, "bi".toLowerCase());
        positionalIndex.buildIndex(fileList, "positional".toLowerCase());
        biwordIndex.store("index");
        positionalIndex.store("positionalIndex");
        System.out.println("Positional index");
        System.out.println("--------------------------------------------");
        positionalIndex.printDictionary();
        System.out.println("--------------------------------------------");
        System.out.println("Biword index");
        System.out.println("--------------------------------------------");
        biwordIndex.printDictionary();

        String test3 = "Introduce have \"Difference learning\"";
        String test4 = "its learning process is natural to sequential decision-making";
        System.out.println("Biword Model result = \n" + biwordIndex.findPhraseQuery(test3));
        System.out.println("Positional Model result = \n" + positionalIndex.findPositionalIndex(test4));

        String phrase = "";

        do {
            System.out.println("Print search phrase: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            phrase = in.readLine();
            if(phrase.isEmpty())
                break;
            System.out.println("Select (from 1 to 2) the model to use: ");
            System.out.println("1. Biword");
            System.out.println("2. Positional");
            String algorithm = in.readLine();
            if (algorithm.equals("1")) {
                System.out.println("Model result = \n" + biwordIndex.findPhraseQuery(phrase));
            } else {
                System.out.println("Model result = \n" + positionalIndex.findPositionalIndex(phrase));
            }
        } while (true);

    }
}
