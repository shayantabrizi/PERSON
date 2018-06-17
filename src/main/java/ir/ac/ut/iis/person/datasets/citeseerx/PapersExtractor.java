/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.citeseerx;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.algorithms.aggregate.AggregateSearcher;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import ir.ac.ut.iis.person.paper.Paper;
import ir.ac.ut.iis.person.paper.TopicsReader;
import ir.ac.ut.iis.retrieval_tools.citeseerx.PapersReader;
import ir.ac.ut.iis.retrieval_tools.papers.Author;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;

/**
 *
 * @author Shayan
 */
public class PapersExtractor {

    Connection connection;

    private Connection getDatabaseConnection() {
        if (connection == null) {
            connection = MySQLConnector.connect(Configs.database_name);
        }
        return connection;
    }

    public void doParseAndIndex(Hierarchy root, String topic, boolean makeRootIndex, Map<String, float[]> topics, Map<String, TopicsReader.DocTopics> topicAssignments) {
        PapersReader papersReader = new PapersReader(new Indexer(root, topic, makeRootIndex, topics, topicAssignments));
        papersReader.run(Configs.datasetRoot + "papers_giant.txt");

        try {
            if (makeRootIndex) {
                root.getRootNode().getIndexer().commit();
            } else {
                doCommit(root.getRootNode());
            }
            System.out.println(" commited ...");
        } catch (IOException ex) {
            Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    /*
     * IMPORTANT: use these queries to enrich table UserPapers after filling it.
     * create table temp select uid1, count(*) from coauthors group by uid1;
     * update UserPapers set NumberOfAuthorsCoauthors = (select cnt from temp where uid1=uid);
     */
    public void createSocialTextualSQLScripts(String papersFileName, String coauthorsFileName) {
        final Map<String, Integer> docIdMap = generateDocIdMap(DatasetMain.getInstance().getIndexReader());
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(papersFileName))) {
            writer.write("insert into UserPapers (uid, paperId, numOfAuthors) values ");
            PapersReader papersReader = new PapersReader(new ir.ac.ut.iis.retrieval_tools.domain.MyIterable<ir.ac.ut.iis.retrieval_tools.papers.BasePaper>() {

                @Override
                public boolean doAction(ir.ac.ut.iis.retrieval_tools.papers.BasePaper d) {
                    try {
                        List<Author> authors = d.getAuthors();
                        Integer dID = docIdMap.get(String.valueOf(d.getId()));
                        if (dID == null) {
                            throw new RuntimeException();
                        }
                        for (Author a : authors) {
                            writer.write("(" + a.getId() + "," + dID + "," + authors.size() + "),\n");
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return true;
                }
            });
            papersReader.run(Configs.datasetRoot + "papers_giant.txt");
            writer.write(";");
        } catch (IOException ex) {
            Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(coauthorsFileName))) {
            writer.write("insert into coauthors (uid1, uid2, weight) values ");
            try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(Configs.datasetRoot + "authors_giant_graph.csv")))) {
                while (sc.hasNextLine()) {
                    String[] nextLine = sc.nextLine().split(",");
                    writer.write("(" + nextLine[0] + "," + nextLine[1] + "," + nextLine[2] + "),"
                            + "(" + nextLine[1] + "," + nextLine[0] + "," + nextLine[2] + "),\n");
                }
            }
            writer.write(";");
        } catch (IOException ex) {
            Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static Map<String, Integer> generateDocIdMap(IndexReader rootReader) {
        Map<String, Integer> docIdMap = new HashMap<>();
        Bits liveDocs = MultiFields.getLiveDocs(rootReader);
        try {
            for (int i = 0; i < rootReader.maxDoc(); i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }

                Document doc = rootReader.document(i);
                docIdMap.put(doc.get("id"), i);

            }
        } catch (IOException ex) {
            Logger.getLogger(AggregateSearcher.class
                    .getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return docIdMap;
    }

    protected void doCommit(HierarchyNode root) throws IOException {
        root.getIndexer().commit();
        //        root.getIndexer().waitForMerges();
        for (HierarchyNode value : root.getChildren().values()) {
            doCommit(value);
        }
    }

    private class Indexer implements ir.ac.ut.iis.retrieval_tools.domain.MyIterable<ir.ac.ut.iis.retrieval_tools.papers.BasePaper> {

        private final Hierarchy root;
        private int count = 0;
        private final String topic;
        private Map<Integer, List<String>> readWeights;
        private final boolean makeRootIndex;
        private final Map<String, float[]> topics;
        private final Map<String, TopicsReader.DocTopics> docTopics;

        private Indexer(Hierarchy root, String topic, boolean makeRootIndex, Map<String, float[]> topics, Map<String, TopicsReader.DocTopics> docTopics) {
            this.root = root;
            this.topic = topic;
            if (topic != null) {
                readWeights = ir.ac.ut.iis.person.datasets.citeseerx.old.GraphExtractor.readWeights(Configs.datasetRoot + "graph-weights.txt", 10);
            }
            this.makeRootIndex = makeRootIndex;
            this.topics = topics;
            this.docTopics = docTopics;
        }

        @Override
        public boolean doAction(ir.ac.ut.iis.retrieval_tools.papers.BasePaper p) {
            if (count % 10_000 == 0) {
                System.out.println(count + " " + new Date());
            }

            count++;
            Paper cite = new Paper(
                    p.getId(),
                    p.getDocId(),
                    p.getAbs(),
                    p.getTitle(),
                    p.getDate().getYear() + 1_900,
                    p.isIsMerged());
            boolean check = true;
            if (readWeights != null) {
                check = false;
                List<String> get = readWeights.get(cite.getId());
                if (get != null && get.contains(topic)) {
                    check = true;
                }
            }
            if (check) {
                cite.setCreators(p.getAuthors());
                cite.setReferences(p.getUnprocessableRefs());
                try {
                    doIndex(cite, root);
                } catch (IOException ex) {
                    Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            if (count % 600_0000 == 0) {
                try {
                    doCommit(root.getRootNode());
                    System.out.println(" commited ...");
                } catch (IOException ex) {
                    Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            return true;
        }

        protected void doIndex(Paper node, Hierarchy hier) throws IOException {
            Document doc;
            if (makeRootIndex) {
                doc = makeCompleteDocument(node);
                hier.getRootNode().getIndexer().addDocument(doc);
                return;
            } else {
                doc = makeDocument(node);
            }
            List<HierarchyNode> list = hier.getHierarchyNodesParentFirst(node.getCreators().get(0).getId());

            if (list.isEmpty()) {
//                System.out.println(node.intID + " " + node.creators.get(0).getId() + " " + currentNode.path + " not exists");
//            throw new RuntimeException();
                return;
            }

            for (HierarchyNode n : list) {
                if (n.getIndexer() == null) {
                    System.out.println("salam");
                }
                n.getIndexer().addDocument(doc);
            }
        }

        public Document makeDocument(Paper node) {
            org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
            doc.add(new StringField("id", String.valueOf(node.getId()), Field.Store.YES));
            doc.add(new TextField("content", node.getText(), Field.Store.NO));
            return doc;
        }

        public Document makeCompleteDocument(Paper node) {
            org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
            doc.add(new StringField("id", String.valueOf(node.getId()), Field.Store.YES));
            doc.add(new StringField("isMerged", String.valueOf(node.isIsMerged() ? 1 : 0), Field.Store.YES));
            doc.add(new TextField("title", node.getTitle(), Field.Store.YES));
            doc.add(new TextField("abstract", node.getPaperAbstract(), Field.Store.YES));
            FieldType type = new FieldType();
            type.setStored(true);
            type.setStoreTermVectors(true);
            type.setTokenized(true);
            type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
            Field field = new Field("content", node.getText(), type);
            doc.add(field);
            doc.add(new IntPoint("year", node.getYear()));
            doc.add(new StoredField("year", node.getYear()));

            StringBuilder refs = new StringBuilder();
            for (String c : node.getReferences()) {
                refs.append(c).append(" ");
            }
            StringBuilder authors = new StringBuilder();
            for (Author c : node.getCreators()) {
                authors.append(c.getId()).append(" ");
            }
            doc.add(new TextField("refs", refs.toString(), Field.Store.YES));
            doc.add(new TextField("authors", authors.toString(), Field.Store.YES));

            if (topics != null) {
                byte[] topicsArray;
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
                    out.writeObject(topics.get(String.valueOf(node.getId())));
                    topicsArray = byteArrayOutputStream.toByteArray();
                } catch (IOException ex) {
                    Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
                doc.add(new StoredField("topics", topicsArray));
                if (Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP2) || Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP3)) {
                    PreparedStatement pstmt;
                    try {
                        pstmt = getDatabaseConnection().prepareStatement("select * from TopicsProfiles" + Configs.profileTopicsDBTable + " where userId=?");
                        pstmt.setInt(1, node.getCreators().iterator().next().getId());
                        ResultSet executeQuery = pstmt.executeQuery();
                        executeQuery.next();
                        byte[] bytes = executeQuery.getBytes("topics");
                        doc.add(new StoredField("authorTopics", bytes));
                        if (Configs.runStage.equals(Configs.RunStage.CREATE_INDEXES_WITH_TOPICS_STEP3)) {
                            pstmt = getDatabaseConnection().prepareStatement("select * from TopicsProfiles_reg" + Configs.profileTopicsDBTable + " where userId=?");
                            pstmt.setInt(1, node.getCreators().iterator().next().getId());
                            ResultSet eq = pstmt.executeQuery();
                            eq.next();
                            byte[] b = eq.getBytes("topics");
                            doc.add(new StoredField("authorTopics_reg", b));
                            pstmt = getDatabaseConnection().prepareStatement("select * from TopicsProfiles_reg_topic7" + Configs.profileTopicsDBTable + " where userId=?");
                            pstmt.setInt(1, node.getCreators().iterator().next().getId());
                            eq = pstmt.executeQuery();
                            eq.next();
                            b = eq.getBytes("topics");
                            doc.add(new StoredField("authorTopics_reg_topic7", b));
                        }
                        doc.add(new StoredField("authorPapersCount", executeQuery.getInt("docCount")));
                    } catch (SQLException ex) {
                        Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException();
                    }
                }
            }

            if (docTopics != null) {
                byte[] topicsArray;
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
                    out.writeObject(docTopics.get(String.valueOf(node.getId())));
                    topicsArray = byteArrayOutputStream.toByteArray();
                } catch (IOException ex) {
                    Logger.getLogger(PapersExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
                doc.add(new StoredField("topicAssignments", topicsArray));
            }

            return doc;
        }

    }
}
