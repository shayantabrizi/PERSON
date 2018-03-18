package ir.ac.ut.iis.person.myretrieval;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

/**
 * Expert: A <code>Scorer</code> for documents matching a <code>Term</code>.
 */
public class TermScorer extends Scorer {

    private final PostingsEnum postingsEnum;
    private final Similarity.SimScorer docScorer;
    private final float collectionProbability;
    private final NumericDocValues ndv;
    private final Term term;
    private final float boost;

    /**
     * Construct a <code>TermScorer</code>.
     *
     * @param weight The weight of the <code>Term</code> in the query.
     * @param td An iterator over the documents matching the <code>Term</code>.
     * @param docScorer The <code>Similarity.SimScorer</code> implementation to
     * be used for score computations.
     */
    TermScorer(Weight weight, PostingsEnum td, Similarity.SimScorer docScorer, float collectionProbability, NumericDocValues ndv, Term term, float boost) {
        super(weight);
        this.docScorer = docScorer;
        this.postingsEnum = td;
        this.collectionProbability = collectionProbability;
        this.ndv = ndv;
        this.term = term;
        this.boost = boost;
    }

    @Override
    public int docID() {
        if (postingsEnum == null) {
            return DocIdSetIterator.NO_MORE_DOCS;
        }
        return postingsEnum.docID();
    }

    public int freq() throws IOException {
        if (postingsEnum == null) {
            return 0;
        }
        return postingsEnum.freq();
    }

    public int getDocLen() {
        if (postingsEnum == null) {
            throw new RuntimeException();
        }
        try {
            ndv.advance(postingsEnum.docID());
            if (ndv.docID() != postingsEnum.docID()) {
                throw new RuntimeException();
            }

            return (int) ndv.longValue();
        } catch (IOException ex) {
            Logger.getLogger(TermScorer.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public Term getTerm() {
        return term;
    }

    public float getBoost() {
        return boost;
    }

    public float getCollectionProbability() {
        return collectionProbability;
    }

    @Override
    public DocIdSetIterator iterator() {
        return postingsEnum;
    }

    @Override
    public float score() throws IOException {
        if (postingsEnum == null) {
            throw new RuntimeException();
        }
        assert docID() != DocIdSetIterator.NO_MORE_DOCS;
        return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
    }

    /**
     * Returns a string representation of this <code>TermScorer</code>.
     */
    @Override
    public String toString() {
        return "scorer(" + weight + ")[" + super.toString() + "]";
    }
}
