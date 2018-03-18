/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.myretrieval;

import ir.ac.ut.iis.person.query.Query;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

/**
 *
 * @author GOMROK IRAN
 */
public class MyDummySimilarity extends LMDirichletSimilarity {

    private final MyQuery mq;

    public MyDummySimilarity(float dirichletMu, MyQuery mq) {
        super(dirichletMu);
        this.mq = mq;
    }

    public MyQuery initializeQuery(String field, String query, Query q, org.apache.lucene.search.Query ignoreQuery) {
        mq.reuseQuery(field, query, q, ignoreQuery);
        return mq;
    }

}
