/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.topics;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author shayan
 */
public class InstanceClassifier {

    protected cc.mallet.topics.TopicInferencer inferencer;
    private final SerialPipes serialPipes;

    public InstanceClassifier() {
        this(true);
    }

    public InstanceClassifier(boolean readInferencer) {
        if (readInferencer) {
            inferencer = readInferencer();
        }
        serialPipes = readPipes();
    }

    private cc.mallet.topics.TopicInferencer readInferencer() throws RuntimeException {
        try {
            return TopicInferencer.read(new File(Configs.datasetRoot + "topics/" + Configs.topicsName + "/inferencer.mallet"));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private SerialPipes readPipes() throws RuntimeException {
        Alphabet alphabet;
        try (InputStream file = new FileInputStream(Configs.datasetRoot + "topics/" + Configs.topicsName + "/alphabet.txt");
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream(buffer);) {
            alphabet = (Alphabet) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequenceRemoveStopwords(new File(this.getClass().getResource("/stoplists/en.txt").getFile()), "UTF-8", false, false, false));
        pipeList.add(new TokenSequence2FeatureSequence(alphabet));
        return new SerialPipes(pipeList);
    }

    public static void main(String[] args) {
        ParallelTopicModel lda = readModel(Configs.datasetRoot + "topics/" + Configs.topicsName + "/model.mallet");
        try (OutputStream file = new FileOutputStream(Configs.datasetRoot + "topics/" + Configs.topicsName + "/alphabet.txt");
                OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer);) {
            output.writeObject(lda.getAlphabet());
        } catch (IOException ex) {
            Logger.getLogger(InstanceClassifier.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public static ParallelTopicModel readModel(String fileName) throws RuntimeException {
        ParallelTopicModel lda;
        try {
            lda = ParallelTopicModel.read(new File(fileName));
        } catch (Exception ex) {
            Logger.getLogger(InstanceClassifier.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return lda;
    }

    public float[] getQueryTopics(String query) {
        List<String> tokenizeString = PapersRetriever.tokenizeString(query);
        StringBuilder sb = new StringBuilder();
        for (String s : tokenizeString) {
            sb.append(s).append(" ");
        }

        Instance ins = new Instance(query, null, "test", null);
        InstanceList instanceList = new InstanceList(serialPipes);
        instanceList.addThruPipe(ins);
        final double[] sampledDistribution = inferencer.getSampledDistribution(instanceList.get(0), 100, 10, 10);
        float[] floatArray = new float[sampledDistribution.length];
        for (int i = 0; i < sampledDistribution.length; i++) {
            floatArray[i] = (float) sampledDistribution[i];
        }

        return floatArray;
    }

    public static List<Integer> getTopClasses(float[] queryTopics, int numOfClasses) {
        final Integer[] idx = new Integer[queryTopics.length];
        for (int i = 0; i < queryTopics.length; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return Double.compare(queryTopics[o2], queryTopics[o1]);
            }
        });

        List<Integer> indices = new LinkedList<>();
        for (int i = 0; i < numOfClasses; i++) {
            indices.add(idx[i]);
        }
        return indices;
    }

    public List<Integer> getTopClasses(String query, int numberOfTopics) {
        return getTopClasses(getQueryTopics(query), numberOfTopics);
    }

    public Integer getTopClass(String query) {
        return getTopClasses(getQueryTopics(query), 1).iterator().next();
    }

    public void setPriors(float[] priorTopics) {
    }

}
