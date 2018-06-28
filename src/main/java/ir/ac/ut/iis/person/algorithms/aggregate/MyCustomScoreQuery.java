/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.search.Query;

/**
 *
 * @author shayan
 */
public class MyCustomScoreQuery extends org.apache.lucene.queries.CustomScoreQuery {

    private final float personalizationWeight;
    private final float textualWeight;
    private final boolean powTextual;
    private final float normalizationFactor;

    // powTextual = true means convert textual_weight to e^textual_weight before combining it with personalization weight
    // normalizationFactor=0f means do not normalize
    public MyCustomScoreQuery(Query subQuery, FunctionQuery fq, float personalizationWeight, float textualWeight, boolean powTextual, float normalizationFactor) {
        super(subQuery, fq);
        this.personalizationWeight = personalizationWeight;
        this.textualWeight = textualWeight;
        this.powTextual = powTextual;
        this.normalizationFactor = normalizationFactor;
    }

    @Override
    protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context) throws IOException {
        return new MyCustomScoreProvider(context, personalizationWeight, textualWeight, powTextual, normalizationFactor);
    }

    @Override
    public CustomScoreQuery clone() {
        return this;
    }

}
