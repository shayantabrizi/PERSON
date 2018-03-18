/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.datasets.citeseerx.PapersExtractor;
import ir.ac.ut.iis.person.hierarchy.GraphNode;
import ir.ac.ut.iis.person.hierarchy.User;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author shayan
 */
public class TopicsProfileGenerator implements Closeable {

    protected final Connection conn;

    public TopicsProfileGenerator(String databaseName) {
        this.conn = MySQLConnector.connect(databaseName);
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
                    float[] userTopics = get.u.getTopics();
                    float[] value = d.getValue();
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

                for (int i = 0; i < length; i++) {
                    float[] userTopics = u.u.getTopics();
                    userTopics[i] /= u.docCount;
                }
                byte[] toByteArray = toByteArray(u.u.getTopics());
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
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(TopicsProfileGenerator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static class UserData {

        User u;
        int docCount = 0;

        private UserData(User u) {
            this.u = u;
        }
    }
}
