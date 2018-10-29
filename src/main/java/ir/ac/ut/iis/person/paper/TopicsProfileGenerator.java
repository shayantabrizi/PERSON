/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.datasets.citeseerx.PapersExtractor;
import ir.ac.ut.iis.person.hierarchy.GraphNode;
import ir.ac.ut.iis.person.hierarchy.IUser;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RList;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author shayan
 */
public class TopicsProfileGenerator implements Closeable {

    protected final Connection conn;
    private final boolean useDirichletEstimation;
    private Rengine re;

    public TopicsProfileGenerator(String databaseName, boolean useDirichletEstimation) {
        this.conn = MySQLConnector.connect(databaseName);
        this.useDirichletEstimation = useDirichletEstimation;
        if (useDirichletEstimation) {
            re = new Rengine(new String[]{"--vanilla"}, false, null);
            if (!re.waitForR()) {
                throw new RuntimeException();
            }
        }
    }

    public void generateTopicsProfile(Set<GraphNode> users, IndexSearcher searcher, Map<String, float[]> topics) {
        System.out.println("Generating topic profiles started");
        Map<Integer, UserData> map = new HashMap<>();
        int length = topics.values().iterator().next().length;
        for (GraphNode u : users) {
            map.put(u.getId().getId(), new UserData(u.getId()));
            u.getId().setTopics(new float[length]);
        }

        TopDocs search;
        for (Map.Entry<String, float[]> d : topics.entrySet()) {
            try {
                search = searcher.search(new TermQuery(new Term("id", String.valueOf(d.getKey()))), 1);
                if (search != null && search.totalHits == 0) {
                    throw new RuntimeException();
                }

                String[] split = searcher.getIndexReader().document(search.scoreDocs[0].doc).get("authors").split(" ");
                for (String authorId : split) {
                    UserData get = map.get(Integer.parseInt(authorId));
                    get.docCount++;
                    float[] value = d.getValue();
                    if (useDirichletEstimation) {
                        get.docs.add(value);
                    }
                    float[] userTopics = get.u.getTopics();
                    for (int i = 0; i < length; i++) {
                        userTopics[i] += value[i];
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(TopicsProfileGenerator.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }
        PreparedStatement pstmt;
        try {
            pstmt = createTopicsProfilesTable(conn, "", true);
            for (UserData u : map.values()) {
                pstmt.setInt(1, u.u.getId());
                pstmt.setInt(3, u.docCount);

                float[] userTopics = u.u.getTopics();
                for (int i = 0; i < length; i++) {
                    userTopics[i] /= u.docCount;
                }
                if (useDirichletEstimation) {
                    float[] userTopics2 = estimateTopicsByDirichlet(u.docs);
                    if (userTopics2 != null) {
                        userTopics = userTopics2;
                    }
//                    System.out.println("T0: " + u.docCount);
//                    System.out.println("T1: " + Arrays.toString(userTopics));
//                    System.out.println("T2: " + Arrays.toString(userTopics2));
                }
                byte[] toByteArray = toByteArray(userTopics);
                pstmt.setBytes(2, toByteArray);

                pstmt.execute();
            }
            System.out.println("Commiting started");
            conn.commit();
            System.out.println("Commiting finished");
        } catch (SQLException ex) {
            Logger.getLogger(TopicsProfileGenerator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        System.out.println("Generating topic profiles finished");
    }

    public static byte[] toByteArray(float[] topics) throws RuntimeException {
        final byte[] toByteArray;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
            out.writeObject(topics);
            toByteArray = byteArrayOutputStream.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return toByteArray;
    }

    public static PreparedStatement createTopicsProfilesTable(Connection conn, String suffix, boolean hasDocCount) {
        try {
            PreparedStatement pstmt;
            conn.setAutoCommit(false);
            conn.prepareStatement("DROP TABLE IF EXISTS `TopicsProfiles" + suffix + Configs.profileTopicsDBTable + "`").executeUpdate();
            conn.prepareStatement("CREATE TABLE `TopicsProfiles" + suffix + Configs.profileTopicsDBTable + "` (\n"
                    + "  `userId` int(11) NOT NULL,\n"
                    + "  `topics` blob,\n"
                    + (hasDocCount ? "  `docCount` int(11) DEFAULT NULL,\n" : "")
                    + "  PRIMARY KEY (`userId`)\n"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;").executeUpdate();
            pstmt = conn.prepareStatement("insert into TopicsProfiles" + suffix + Configs.profileTopicsDBTable + " values (?," + (hasDocCount ? "?," : "") + "?)");
            return pstmt;
        } catch (SQLException ex) {
            Logger.getLogger(TopicsProfileGenerator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    @Override
    public void close() throws IOException {
//        System.out.println("CNT: " + cnt1 + " " + cnt2 + " " + cnt3 + " " + cnt4 + " ");
        try {
            conn.close();
            if (re != null) {
                re.end();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TopicsProfileGenerator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public static REXP toRmatrix(Rengine r, double[][] matrix, String assign) {
        REXP resultat = null;
        if (matrix.length > 0) {
            r.assign(assign, matrix[0]);
            resultat = r.eval(assign + " <- matrix( " + assign + " ,nr=1)");
        } else {
            return null;
        }
        for (int i = 1; i < matrix.length; i++) {
            r.assign("intermediaire", matrix[i]);
            resultat = r.eval(assign + " <- rbind(" + assign + ",matrix(intermediaire,nr=1))");
        }
        return resultat;
    }

    private float[] estimateTopicsByDirichlet(Set<float[]> map) {
//        boolean check = true;
        if (map.size() == 1) {
            return map.iterator().next();
        }
//        for (float[] a : map) {
//            if (!Arrays.equals(a, next)) {
//                check = false;
//                break;
//            }
//        }
//        if (check) {
//            if (map.size() == 1) {
//                cnt1++;
//            } else {
//                cnt2++;
//            }
//            return next;
//        }
        final int length = map.iterator().next().length;
        double[][] arr = new double[map.size()][length];
        int i = 0;
        for (float[] m : map) {
            for (int j = 0; j < length; j++) {
                arr[i][j] = m[j];
            }
            i++;
        }
        REXP toRmatrix = toRmatrix(re, arr, "test");

        REXP x = re.eval("sirt::dirichlet.mle(test)");
        if (x == null) {
//            cnt3++;
            return null;
        }
        final RList asList = x.asList();
//        if (cnt1 < 100) {
//            System.out.println(asList.at(1).asDouble());
//        }
        float[] asFloatArray = Floats.toArray(Doubles.asList(asList.at(2).asDoubleArray()));
//        cnt4++;
        return asFloatArray;
    }

    public static void main() {
        try (TopicsProfileGenerator topicsProfileGenerator = new TopicsProfileGenerator("", true)) {
            Set<float[]> map = new HashSet<>();
            map.add(new float[]{.1f, .2f, .7f});
            map.add(new float[]{.2f, .3f, .5f});
            topicsProfileGenerator.estimateTopicsByDirichlet(map);
        } catch (IOException ex) {
            Logger.getLogger(TopicsProfileGenerator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static class UserData {

        IUser u;
        int docCount = 0;
        Set<float[]> docs = new HashSet<>();

        private UserData(IUser u) {
            this.u = u;
        }
    }
}
