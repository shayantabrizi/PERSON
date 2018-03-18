/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

import ir.ac.ut.iis.person.base.Instance.Instances;
import ir.ac.ut.iis.person.query.Query;
import ir.ac.ut.iis.person.query.Query.Result;
import ir.ac.ut.iis.person.query.QueryConverter;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author shayan
 */
public abstract class Searcher {

    private final String name;
    private final QueryConverter queryConverter;

    public Searcher(String name, QueryConverter queryConverter) {
        this.name = name;
        this.queryConverter = queryConverter;
    }

    public String getName() {
        String paramsString = getParamsString();
        return name + (paramsString == null ? "" : "{" + getParamsString() + "}");
    }

    protected String getParamsString() {
        return null;
    }

    protected List<Result> convertTopDocsToResultList(TopDocs td, Instances ins, String queryId) {
        List<Result> list = new LinkedList<>();
        for (ScoreDoc d : td.scoreDocs) {
            if (ins != null) {
                final Instance instance = ins.getInstance(d.doc, queryId);
                if (instance == null) {
                    System.out.println("");
                }
                list.add(new Result(d.doc, instance));
            } else {
                list.add(new Result(d.doc));
            }
        }
        return list;

    }

    public QueryConverter getQueryConverter() {
        return queryConverter;
    }

    public abstract List<Query.Result> search(Query q, int numOfResults);

    public abstract String explain(Query q, int docId);

}
