/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.citeseerx;

import ir.ac.ut.iis.person.paper.DisambiguatedAuthorFactory;
import ir.ac.ut.iis.person.paper.PapersPreprocessor;
import ir.ac.ut.iis.retrieval_tools.crawler.IterableToFiles;
import ir.ac.ut.iis.retrieval_tools.crawler.IterateOnFiles;
import java.io.File;
import java.util.List;

/**
 *
 * @author Shayan
 */
public class CiteseerxPreprocessor implements IterableToFiles<List> {

    PapersPreprocessor pp = new PapersPreprocessor(new DisambiguatedAuthorFactory());

    @Override
    public boolean doAction(final File fileEntry, final List records) {
        for (Object o : records) {
            pp.processPaper(o);
        }
        return true;
    }

    public static void main(String[] args) {
        CiteseerxPreprocessor citeseerxPreprocessor = new CiteseerxPreprocessor();
        IterateOnFiles iof = new IterateOnFiles(citeseerxPreprocessor, new CiteseerxParser());
        iof.iterate(new File("/home/shayan/Desktop/Taval/citeseerx-oai"));
        citeseerxPreprocessor.pp.postProcess();
        PapersPreprocessor.convertAuthorsFileToGraphs("authors_giant.txt", "authors_graph_giant.csv");
        PapersPreprocessor.convertPapersFileToGraph("papers_giant.txt", "papers_graph_giant.csv");
    }

}
