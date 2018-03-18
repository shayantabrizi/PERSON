/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.base.Statistic;
import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;

/**
 *
 * @author shayan
 */
public class MyCustomScoreProvider extends CustomScoreProvider {

    private static final Statistic subQueryScore = new Statistic("subQueryScore");
    private static final Statistic valSrcScore = new Statistic("valSrcScore");
    private static final Statistic allSubQueryScore = new Statistic("allSubQueryScore");
    private static final Statistic allValSrcScore = new Statistic("allValSrcScore");
    private static final Statistic.Statistics statistics = new Statistic.Statistics();

    static {
        statistics.addStatistic(subQueryScore);
        statistics.addStatistic(valSrcScore);
//        statistics.addStatistic(allSubQueryScore);
//        statistics.addStatistic(allValSrcScore);
    }

    public static void resetStatistics() {
        subQueryScore.initialize();
        valSrcScore.initialize();
    }

    public static void printStatistics() {
        statistics.printStatistics();
    }

    private final float personalizationWeight;
    private final float textualWeight;

    public MyCustomScoreProvider(LeafReaderContext context, float personalizationWeight, float textualWeight) {
        super(context);
        this.personalizationWeight = personalizationWeight;
        this.textualWeight = textualWeight;
    }

    @Override
    public float customScore(int doc, float subQueryScore, float valSrcScore)
            throws IOException {
//        System.out.println(subQueryScore + " " + valSrcScore);

//        try {
//            Document document = Main.hiers[0].getRootNode().getIndexReader().document(doc);
//            final int year = Integer.parseInt(document.get("year"));
//            System.out.println(year + " " + subQueryScore + " " + valSrcScore);
//            if (year > 2000) {
//                subQueryScoreSum = 1000;
//                valSrcScoreSum = 1000;
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(RandomSearcher.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException();
//        }
//        final double pow = Math.pow(Math.E, subQueryScore);
        MyCustomScoreProvider.subQueryScore.add(subQueryScore);
        MyCustomScoreProvider.valSrcScore.add(valSrcScore);
        MyCustomScoreProvider.allSubQueryScore.add(subQueryScore);
        MyCustomScoreProvider.allValSrcScore.add(valSrcScore);
//        System.out.println(doc + " " + subQueryScore + " " + valSrcScore);
        return (subQueryScore * textualWeight + valSrcScore * personalizationWeight);
    }

}
