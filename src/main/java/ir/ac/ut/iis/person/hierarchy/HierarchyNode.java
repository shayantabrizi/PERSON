/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import com.google.common.collect.Iterables;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.base.IgnoreQueryEx;
import ir.ac.ut.iis.person.base.Threads;
import ir.ac.ut.iis.person.myretrieval.CachedIndexSearcher;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

    public float[] selfPPR(double pageRankAlpha) {
        return selfPPR(new UniformPPR(id, getUsers(), usersNum(), pageRankAlpha), pageRankAlpha);
    }
//    public static int c1 = 0;
//    public static int c2 = 0;
//    public static int c3 = 0;
//    public static int c4 = 0;
//    public static int c5 = 0;
//    public static int c6 = 0;

    public float[] selfPPR(PPRCalculator pprCalculator, double pageRankAlpha) {
        if (pprCalculator == null) {
            return selfPPR(pageRankAlpha);
        }
        float[] res = null;
        if (isBeingProcessed == false) {
            float[] get = selfPPR.get(pprCalculator);
            if (get != null) {
//                c1++;
                res = get;
            }
        }

        if (res == null) {
            boolean check = false;
            Threads.enterSafeArea();
            synchronized (hier) {
                float[] get = selfPPR.get(pprCalculator);
                if (get != null) {
//                    c2++;
                    res = get;
                } else {
                    check = true;
                    isBeingProcessed = true;
                    int numOfWeights = hier.getNumberOfWeights();
                    if (parent == null) {
                        res = new float[numOfWeights];
                        selfPPR.put(pprCalculator, res);
                        for (int i = 0; i < numOfWeights; i++) {
                            res[i] = 1.f;
                        }
//            System.out.println("SelfPPR: " + getId() + ": " + selfPPR[0]);
                    }

                    if (res == null) {
                        if (Configs.useCachedPPRs) {
                            /*
            selfPPR = getSelfPPRFromDB(getId(), numOfWeights);
            if (selfPPR != null) {
//                System.out.println("SelfPPR: " + getId() + ": " + selfPPR[0]);
                return selfPPR;
            }
                             */
                        }
                        res = new float[numOfWeights];
                        selfPPR.put(pprCalculator, res);

                        for (GraphNode u : getUsers()) {
                            float[] userPPR = userPPR(pprCalculator, pageRankAlpha, u, false);
                            for (int i = 0; i < numOfWeights; i++) {
                                res[i] += userPPR[i];
                            }
                        }
//                        c3++;
                    }
                }
            }
            if (check) {
                synchronized (hier) {
                    isBeingProcessed = false;
                }
            }
            Threads.enterUnsafeArea();
        }
//        System.out.println("SelfPPR: " + getId() + ": " + selfPPR[0]);
        return res;
    }

    public static float[] getSelfPPRFromDB(short hierarchyNodeId, int numOfWeights, float pageRankAlpha) throws RuntimeException {
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
        selectSelfStatement = conn.prepareStatement("select PPR from `PPR_self_" + numOfWeights + "_" + pageRankAlpha + Configs.databaseTablesPostfix + "` where hierarchyNode_id=?");
    }

    public float[] userPPR(GraphNode node, double pageRankAlpha) {
        return userPPR(new UniformPPR(id, getUsers(), usersNum(), pageRankAlpha), pageRankAlpha, node);
    }

    public float[] userPPR(PPRCalculator pprCalculator, double pageRankAlpha, GraphNode node) {
        return userPPR(pprCalculator, pageRankAlpha, node, true);
    }

    public float[] userPPR(PPRCalculator pprCalculator, double pageRankAlpha, GraphNode node, boolean safetyChecking) {
        if (pprCalculator == null) {
            return userPPR(node, pageRankAlpha);
        }
        if (parent == null) {
            System.out.println("null parent occured");
//            System.out.println("");
            throw new IgnoreQueryEx();
        }
        float[] PPR = null;
        if (safetyChecking) {
            if (isBeingProcessed == false) {
                float[] get = node.getMeasure(pprCalculator);
                if (get != null) {
//                    c4++;
                    PPR = get;
                }
            }
            if (PPR == null) {
                boolean check = false;
                Threads.enterSafeArea();
                synchronized (hier) {
                    float[] get = node.getMeasure(pprCalculator);
                    if (get != null) {
//                        c5++;
                        PPR = get;
                    } else {
                        check = true;
                        isBeingProcessed = true;
                        PPR = pprCalculator.calc(hier.getNumberOfWeights(), node, parent.getUsers(), parent.usersNum(), parent.level);
//                        c6++;
                    }
                }
                if (check) {
                    synchronized (hier) {
                        isBeingProcessed = false;
                    }
                }
                Threads.enterUnsafeArea();
            }
        } else {
            PPR = pprCalculator.calc(hier.getNumberOfWeights(), node, parent.getUsers(), parent.usersNum(), parent.level);
        }
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

    public static float[] getUserPPRFromDB(short hierarchyNodeId, int numOfWeights, GraphNode node, double pageRankAlpha) throws RuntimeException {
        /*        try {
            PreparedStatement selectUserStatement = selectUserStatements.get(hierarchyNodeId);
            if (selectUserStatement == null) {
                if (selectSelfStatement == null) {
                    loadSelectSelfStatement(numOfWeights);
                }
                Connection connection = selectSelfStatement.getConnection();
                ResultSet tables = connection.getMetaData().getTables(null, null, Configs.database_name + ".PPR_user_" + numOfWeights + "_" + pageRankAlpha + "_" + hierarchyNodeId, null);
                if (tables.next()) {
                    selectUserStatement = connection.prepareStatement("select PPR from `PPR_user_" + numOfWeights + "_" + pageRankAlpha + "_" + hierarchyNodeId + Configs.databaseTablesPostfix + "` where node_id=?");
                    selectUserStatements.put(hierarchyNodeId, selectUserStatement);
                }
            }
            if (selectUserStatement != null) {
                selectUserStatement.setString(1, node.getId().getId());
                ResultSet executeQuery = selectUserStatement.executeQuery();
                if (executeQuery.next()) {
                    final float[] aFloat = new float[numOfWeights];
                    String[] split = executeQuery.getString("PPR").split(",");
                    for (int i = 0; i < split.length; i++) {
                        aFloat[i] = Float.parseFloat(split[i]);
                    }
                    node.addPPR(hierarchyNodeId, aFloat);
                    return aFloat;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
         */ return null;
    }

    public void optimize() {
        users.trim();
        children.trim();

        for (HierarchyNode c : children.values()) {
            c.optimize();
        }
    }

    public void pruneMeasures(int ratio) {
        if (parent != null && this.usersNum() * ratio < parent.usersNum() && !selfPPR.isEmpty()) {
            selfPPR.clear();
            System.out.println("Removing measures for: " + this.getId());
            for (GraphNode g : parent.getUsers()) {
                for (Iterator<Map.Entry<MeasureCalculator, float[]>> it = g.getMeasure().entrySet().iterator(); it.hasNext();) {
                    Map.Entry<MeasureCalculator, float[]> ii = it.next();
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
}
