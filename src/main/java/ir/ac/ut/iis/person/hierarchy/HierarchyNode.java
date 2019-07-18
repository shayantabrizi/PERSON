/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import com.google.common.collect.Iterables;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.Main;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.base.Threads;
import ir.ac.ut.iis.person.myretrieval.CachedIndexSearcher;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 *
 * @author shayan
 */
public class HierarchyNode {

    private int id;
    private short level;
    private final HierarchyNode parent;
    private final ObjectOpenHashSet<GraphNode> users = new ObjectOpenHashSet<>(1, .75f);
    private Iterable<GraphNode> getUsersIterable = null;
    private final Int2ObjectOpenHashMap<HierarchyNode> children = new Int2ObjectOpenHashMap<>(1, .75f);
    private String path;
    private IndexWriter indexer;
    private IndexSearcher searcher;
    private SimpleFSDirectory fs;
    private IndexReader indexReader;
    private Term cache = null;
    private long valueCache = 0;
    private double totalFreq = -1d;
    private final Map<PPRCalculator, float[]> selfPPR = new HashMap<>();
    private int usersNum = 0;
    private static PreparedStatement selectSelfStatement = null;
    private static final Map<Integer, PreparedStatement> selectUserStatements = new HashMap<>();
    private final Hierarchy hier;
    private volatile boolean isBeingProcessed = false;

    private short flag;

    public HierarchyNode(Hierarchy hier, HierarchyNode parent) {
        this.parent = parent;
        this.hier = hier;
    }

    public boolean isEqualToOrAncestorOf(HierarchyNode h) {
        if (h.equals(this)) {
            return true;
        }
        while (h.getParent() != null) {
            h = h.getParent();
            if (h.equals(this)) {
                return true;
            }
        }
        return false;
    }

    public Iterable<GraphNode> getUsers() {
        if (getUsersIterable == null) {
            getUsersIterable = users;
            for (HierarchyNode c : children.values()) {
                getUsersIterable = Iterables.concat(getUsersIterable, c.getUsers());
            }
        }
        return getUsersIterable;
    }

    public int usersNum() {
        if (usersNum == 0) {
            usersNum = users.size();
            for (HierarchyNode c : children.values()) {
                usersNum += c.usersNum();
            }
        }
        return usersNum;
    }

    public IndexSearcher getSearcher() {
        if (searcher == null && path != null) {
            initializeNode();
        }
        return searcher;
    }

    public IndexReader getIndexReader() {
        if (indexReader == null && path != null) {
            initializeNode();
        }
        return indexReader;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public short getLevel() {
        return level;
    }

    public void addUser(GraphNode u) {
        users.add(u);
    }

    public HierarchyNode getParent() {
        return parent;
    }

    public void setValues(HierarchyNode node) {
        this.indexReader = node.getIndexReader();
        this.searcher = node.getSearcher();
        this.path = node.getPath();
        this.level = node.getLevel();
    }

    public void setLevel(short level) {
        this.level = level;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public short getFlag() {
        return flag;
    }

    public void setFlag(short flag) {
        this.flag = flag;
    }

    public HierarchyNode getChild(GraphNode user) {
        HierarchyNode lastNode = null;
        HierarchyNode currentNode = user.getHierarchyNode();

        while (!currentNode.equals(this)) {
            lastNode = currentNode;
            currentNode = currentNode.getParent();
        }
        return lastNode;
    }

    private void initializeNode() {
        File file = new File(path + File.separator + Configs.indexName);
        if (file.isDirectory()) {
            try {
                this.fs = new SimpleFSDirectory(new File(path + File.separator + Configs.indexName).toPath());
                this.indexReader = DirectoryReader.open(fs);
                if (Configs.useSearchCaching) {
                    this.searcher = new CachedIndexSearcher(new IndexSearcher(indexReader), 15);
                } else {
                    this.searcher = new IndexSearcher(indexReader);
                }
                this.searcher.setSimilarity(new LMDirichletSimilarity());
            } catch (IOException e) {
                Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, e);
            }
        } else {
            System.out.println("n");
            throw new RuntimeException();
        }
    }

    public double getFreq(Term term) throws IOException {
        if (cache != null) {
            if (term.equals(cache)) {
                return valueCache;
            }
        }
        cache = term;
        if (this.getSearcher() == null) {
            System.out.println("h");
        }
        if (this.getSearcher().getIndexReader() == null) {
            System.out.println("n");
        }
        long rValue = this.getSearcher().getIndexReader().totalTermFreq(term);
        valueCache = rValue;
        return rValue;
    }

    public double getTotalFreq() throws IOException {
        if (this.getSearcher() == null) {
            System.out.println("");
        }
        if (totalFreq == -1d) {
            totalFreq = Double.valueOf(this.getSearcher().collectionStatistics("content").sumTotalTermFreq());
        }
        return totalFreq;
    }

    public Int2ObjectOpenHashMap<HierarchyNode> getChildren() {
        return children;
    }

    public void close() throws IOException {
        for (HierarchyNode n : children.values()) {
            n.close();
        }
        if (indexReader != null) {
            indexReader.close();
        }
        if (indexer != null) {
            indexer.close();
        }
        if (fs != null) {
            fs.close();
        }
        if (searcher != null) {
            searcher.getIndexReader().close();
        }
    }

    public IndexWriter getIndexer() {
        return indexer;
    }

    public void setIndexer(IndexWriter indexer) {
        this.indexer = indexer;
    }

    public int getNumberOfWeights() {
        return hier.getNumberOfWeights();
    }

    public float selfPPR(double pageRankAlpha) {
        return selfPPR(new UniformPPR(id, getUsers(), usersNum(), pageRankAlpha), pageRankAlpha);
    }
//    public static int c1 = 0;
//    public static int c2 = 0;
//    public static int c3 = 0;
//    public static int c4 = 0;
//    public static int c5 = 0;
//    public static int c6 = 0;

    public float selfPPR(PPRCalculator pprCalculator, double pageRankAlpha) {
        if (pprCalculator == null) {
            return selfPPR(pageRankAlpha);
        }
        float[] get = selfPPR.get(pprCalculator);
        float[] res = null;
        if (get != null) {
//                    c2++;
            res = get;
        } else {
            int numOfWeights = hier.getNumberOfWeights();
            if (parent == null) {
                res = new float[numOfWeights];
                selfPPR.put(pprCalculator, res);
                selfPPRCount++;
                for (int i = 0; i < numOfWeights; i++) {
                    res[i] = 1.f;
                }
//            System.out.println("SelfPPR: " + getId() + ": " + selfPPR[0]);
            }

            if (res == null) {
//                if (true) {
//                    throw new RuntimeException();
//                }
                res = new float[numOfWeights];
                selfPPR.put(pprCalculator, res);
                selfPPRCount++;

                for (GraphNode u : getUsers()) {
                    float userPPR = userPPR(pprCalculator, pageRankAlpha, u, false);
                    for (int i = 0; i < numOfWeights; i++) {
                        res[i] += userPPR;
                    }
                }
//                        c3++;
            }
        }
//        System.out.println("SelfPPR: " + getId() + ": " + selfPPR[0]);
        return res[0];
    }

    public static float[] getSelfPPRFromDB(int hierarchyNodeId, int numOfWeights, double pageRankAlpha) throws RuntimeException {
        try {
            if (selectSelfStatement == null) {
                loadSelectSelfStatement(numOfWeights, pageRankAlpha);
            }
            selectSelfStatement.setString(1, String.valueOf(hierarchyNodeId));
            ResultSet executeQuery = selectSelfStatement.executeQuery();
            if (executeQuery.next()) {
                String[] split = executeQuery.getString("PPR").split(",");
                float[] aFloat = new float[split.length];
                for (int i = 0; i < split.length; i++) {
                    aFloat[i] = Float.parseFloat(split[i]);
                }
                return aFloat;
            }
        } catch (SQLException ex) {
            Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return null;
    }

    protected static void loadSelectSelfStatement(int numOfWeights, double pageRankAlpha) throws SQLException {
        Connection conn = MySQLConnector.connect(Configs.database_name);
        selectSelfStatement = conn.prepareStatement("select PPR from `PPR_user` where hn_id=? and node_id=?");
    }

    public float userPPR(GraphNode node, double pageRankAlpha) {
        return userPPR(new UniformPPR(id, getUsers(), usersNum(), pageRankAlpha), pageRankAlpha, node);
    }

    public float userPPR(PPRCalculator pprCalculator, double pageRankAlpha, GraphNode node) {
        return userPPR(pprCalculator, pageRankAlpha, node, true);
    }

    public float userPPR(PPRCalculator pprCalculator, double pageRankAlpha, GraphNode node, boolean safetyChecking) {
        if (pprCalculator == null) {
            return userPPR(node, pageRankAlpha);
        }
        if (parent == null) {
            System.out.println("null parent occured");
//            System.out.println("");
            throw new IgnoreQueryEx();
        }
        float PPR = -1;
        PPR = pprCalculator.calc(hier.getNumberOfWeights(), node, parent.getUsers(), parent.usersNum(), parent.level);
//        System.out.println(PPR[0]);
        return PPR;
    }

    public float[] userUserPPR(GraphNode node, GraphNode topic) {
        throw new UnsupportedOperationException();
//        final HashSet<GraphNode> topicSet = new HashSet<>();
//        topicSet.add(topic);
//        UniformPPR uniformVector = new UniformPPR(, topicSet, 1);
//        return uniformVector.PPR(hier.getNumberOfWeights(), node, getUsers(), usersNum(), level);
    }

    public static float getUserPPRFromDB(int hierarchyNodeId, int numOfWeights, GraphNode node, double pageRankAlpha) throws RuntimeException {
//        if (hierarchyNodeId >= 0)
//            return -1;
        try {
//            PreparedStatement selectUserStatement = selectUserStatements.get(hierarchyNodeId);
//            if (selectUserStatement == null) {
            if (selectSelfStatement == null) {
                loadSelectSelfStatement(numOfWeights, pageRankAlpha);
            }
//                Connection connection = selectSelfStatement.getConnection();
//                ResultSet tables = connection.getMetaData().getTables(Configs.database_name, null, "`PPR_user_" + numOfWeights + "_" + pageRankAlpha + "_" + hierarchyNodeId + Configs.databaseTablesPostfix + "`", null);
//                if (tables.next()) {
//                    selectUserStatement = connection.prepareStatement("select PPR from `PPR_user_" + numOfWeights + "_" + pageRankAlpha + "_" + hierarchyNodeId + Configs.databaseTablesPostfix + "` where node_id=?");
//                    selectUserStatements.put(hierarchyNodeId, selectUserStatement);
//                }

            selectSelfStatement.setInt(1, hierarchyNodeId);
            selectSelfStatement.setInt(2, node.getId().getId());
            ResultSet executeQuery = selectSelfStatement.executeQuery();
            if (executeQuery.next()) {
                float split = executeQuery.getFloat("PPR");
//                    node.addPPR(hierarchyNodeId, aFloat);
                return split;
            }
        } catch (SQLException ex) {
            Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    public void optimize() {
        users.trim();
        children.trim();

        for (HierarchyNode c : children.values()) {
            c.optimize();
        }
    }

    public void pruneMeasures(int ratio) {
        if (Configs.pruneThreshold == -1) {
            return;
        }
        if (parent != null && this.usersNum() * ratio < parent.usersNum() && !selfPPR.isEmpty()) {
            selfPPR.clear();
            System.out.println("Removing measures for: " + this.getId());
            for (GraphNode g : parent.getUsers()) {
                for (ObjectIterator<Map.Entry<MeasureCalculator, Float>> it = g.getMeasure().entrySet().iterator(); it.hasNext();) {
                    Map.Entry<MeasureCalculator, Float> ii = it.next();
                    if (ii.getKey().getSeedsId() == this.getId()) {
                        it.remove();
                    }
                }
            }
        }
        for (HierarchyNode c : children.values()) {
            c.pruneMeasures(ratio);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HierarchyNode other = (HierarchyNode) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
    public static int selfPPRCount = 0;
    public static int userPPRCount = 0;
    public static OutputStreamWriter pprOSW = null;

    public void loadPPRs(String filename, int threshold) {
        PPRLoader.loadAndExecute(filename, hier, false,
                //                hn -> hn.getParent().usersNum() < threshold * hn.usersNum(),
                hn -> true,
                (hn, ss) -> {
                    GraphNode user = hier.getUserNode(Integer.parseInt(ss[0]));
                    try {
                        user.addMeasure(new UniformPPR(hn.getId(), hn.getUsers(), hn.usersNum(), Configs.pagerankAlpha), Float.parseFloat(ss[1]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                t -> {
                },
                (hn, s) -> {
                },
                () -> {
                });
        hier.getRootNode().getUsers().forEach((user) -> {
            user.getMeasure().trim();
        });
    }

    public void loadSelfPPRs(String filename) {
        try ( Scanner sc = new Scanner(new FileInputStream(filename))) {
            while (sc.hasNextLine()) {
                String[] split = sc.nextLine().split(" ");
                String[] ids = split[0].split(":");
                HierarchyNode hn = hier.getRootNode();
                for (String id : ids) {
                    if (id.equals("99")) {
                        System.out.println("");
                    }
                    hn = hn.getChildren().get(Integer.parseInt(id));
                }
                if (hn == null) {
                    System.out.println("");
                }
                if (hn.getId() >= 0) {
                    hn.selfPPR.put(new UniformPPR(hn.getId(), hn.getUsers(), hn.usersNum(), Configs.pagerankAlpha), new float[]{(float) Float.parseFloat(split[1])});
                } else {
                    hn.getUsers().iterator().next().addMeasure(new UniformPPR(hn.getId(), hn.getUsers(), hn.usersNum(), Configs.pagerankAlpha), Float.parseFloat(split[1]));
                }

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void precompute(int childThreshold, int parentThreshold, double pageRankAlpha) {
        if (usersNum() < childThreshold || (!(parent == null) && parent.usersNum() < parentThreshold)) {
            return;
        }
        boolean check = false;
        for (GraphNode u : getUsers()) {
            if (ir.ac.ut.iis.person.paper.PapersRetriever.checkBalanceness(u)) {
                check = true;
                break;
            }
        }
        if (check) {
            selfPPR(pageRankAlpha);
            if (pprOSW != null && parent != null) {
                HierarchyNode node = this;
                List<String> list = new ArrayList<>();
                while (node.getId() != -2147483648) {
                    list.add(0, String.valueOf(node.getId()));
                    node = node.getParent();
                }
                StringBuilder sb = new StringBuilder();
                list.forEach(s -> {
                    sb.append(s).append(":");
                });
                try {
                    pprOSW.write(sb.toString());
                    pprOSW.write(" " + parent.usersNum() + "\n");
                } catch (IOException ex) {
                    Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                parent.getUsers().forEach(user -> {
                    try {
                        pprOSW.write(user.getId().getId() + " " + user.getMeasure().values().iterator().next() + "\n");
                    } catch (IOException ex) {
                        Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    user.resetMeasure();
                });
            }

            for (HierarchyNode c : getChildren().values()) {
                c.precompute(childThreshold, parentThreshold, pageRankAlpha);
            }
        }
    }

    public void injectDummyValues(int maxBranching) {
        int okCount = 0;
        if (id ==-1)
            System.out.println("");
        if (id < -1 && id != Integer.MIN_VALUE)
            return;
        for (HierarchyNode c : getChildren().values()) {
            boolean check = false;
            for (GraphNode u : c.getUsers()) {
                if (ir.ac.ut.iis.person.paper.PapersRetriever.checkBalanceness(u)) {
                    check = true;
                    break;
                }
            }
            if (check) {
                okCount++;
            }
        }
        final int finalOk = okCount;
        getUsers().forEach(u -> {
            for (int i = 0; i < Math.min(maxBranching, usersNum()) - finalOk; i++) {
                while (true) {
                    int rand = Main.random(1000000000) + 1000000;
                    UniformPPR uniformPPR = new UniformPPR(rand, null, rand, 0);
                    if (u.getMeasure(uniformPPR) == -1) {
                        u.addMeasure(uniformPPR, 0.f);
                        break;
                    }
                }
            }
        });
        for (HierarchyNode c : getChildren().values()) {
            c.injectDummyValues(maxBranching);
        }
    }

    public void precomputePPRs(double pageRankAlpha, List<GraphNode> nodes) {
        nodes.forEach(node -> {
            UniformPPR uniformPPR = new UniformPPR(node.getId().getId(), java.util.Collections.singleton(node), 1, pageRankAlpha);
            uniformPPR.calc(1, node, this.getUsers(), this.usersNum(), (short) 0);
            try {
                pprOSW.write(node.getId().getId());
                pprOSW.write(" " + hier.getRootNode().usersNum() + "\n");
            } catch (IOException ex) {
                Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.getUsers().forEach(user -> {
                try {
                    pprOSW.write(user.getId().getId() + " " + user.getMeasure().values().iterator().next() + "\n");
                } catch (IOException ex) {
                    Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                user.resetMeasure();
            });
        });
    }
}
