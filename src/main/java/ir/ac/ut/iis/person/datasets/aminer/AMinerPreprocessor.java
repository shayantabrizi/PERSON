/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.aminer;

import ir.ac.ut.iis.retrieval_tools.papers.PreprocessedPaper;
import ir.ac.ut.iis.person.paper.BasicAuthorFactory;
import ir.ac.ut.iis.person.paper.DisambiguatedAuthorFactory;
import ir.ac.ut.iis.person.paper.PapersPreprocessor;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class AMinerPreprocessor {

    PapersPreprocessor pp = new PapersPreprocessor(new BasicAuthorFactory());

    public static void main(String[] args) {
        String inputFile = "authors_giant.txt";
        String outputFile = "authors_giant_graph.csv";
        AMinerPreprocessor aMinerPreprocessor = new AMinerPreprocessor();
//        convertDataset("/media/veracrypt5/Archieve/Research/Datasets/Papers Datasets/AMiner/citation-network2.txt", aMinerPreprocessor);
        PapersPreprocessor.convertAuthorsFileToGraphs(inputFile, outputFile);
//        PapersPreprocessor.convertPapersFileToGraph("papers_giant.txt", "papers_giant_graph.csv");
    }

    private static void convertDataset(String fileName, AMinerPreprocessor aMinerPreprocessor) throws RuntimeException {
        int papersWithIgnoredAuthors = 0;
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(fileName)))) {
            sc.useDelimiter("\n");
            PreprocessedPaper pp = null;
            boolean ppCheck = true;
            while (sc.hasNext()) {
                String nextLine = sc.next();
                if (pp == null) {
                    ppCheck = true;
                    pp = new PreprocessedPaper();
                }
                nextLine = nextLine.replaceAll("[\n\r\uDB4A\uDCA9\u0085]", "");
                if (nextLine.startsWith("#*")) {
                    pp.setTitle(nextLine.substring(2));
                } else if (nextLine.startsWith("#@")) {
                    String s = nextLine.substring(2);
                    if (s.isEmpty()) {
                        ppCheck = false;
                        continue;
                    }
                    if (s.length() < 3) {
                        papersWithIgnoredAuthors++;
                        ppCheck = false;
                        continue;
                    }
                    if (s.contains("ARRAY(0x") || s.equals("Team Project Sequoia 2000") || s.equals(sc)) {
                        ppCheck = false;
                        continue;
                    }
                    String[] split = s.split(",");
                    if (split.length == 0) {
                        throw new RuntimeException();
                    }
                    for (String a : split) {
                        if (!a.contains(" ") || a.matches(".* \\w(\\.)?") || DisambiguatedAuthorFactory.clean(a) == null) {
                            papersWithIgnoredAuthors++;
                            ppCheck = false;
                            break;
                        }
                        if (a.equals("8088 Family")) {
                            continue;
                        }
                        pp.addCreator(a);
                    }
                } else if (nextLine.startsWith("#t")) {
                    if (Integer.parseInt(nextLine.substring(2)) <= 2002) {
                        ppCheck = false;
                        continue;
                    }
                    pp.addDate(nextLine.substring(2));
                } else if (nextLine.startsWith("#c")) {
                } else if (nextLine.startsWith("#index")) {
                    pp.setDocId(nextLine.substring(6));
                } else if (nextLine.startsWith("#%")) {
                    pp.addRelation(nextLine.substring(2));
                } else if (nextLine.startsWith("#!")) {
                    pp.setAbs(nextLine.substring(2));
                } else if (nextLine.isEmpty()) {
                    if (pp.getAbs() == null) {
                        ppCheck = false;
                    }
                    if (ppCheck) {
                        aMinerPreprocessor.pp.processPaper(pp);
                    }
                    pp = null;
                }
            }
            if (pp != null && ppCheck) {
                aMinerPreprocessor.pp.processPaper(pp);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AMinerPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        System.out.println("papersWithIgnoredAuthors: " + papersWithIgnoredAuthors);
        aMinerPreprocessor.pp.postProcess();
    }

}
