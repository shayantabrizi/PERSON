/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 * With Love To Lucene
 *
 * @author Mohammad
 * @param <U>
 */
public class Hierarchy<U extends User> {

    private final HierarchyNode rootNode = new HierarchyNode(this, null);
    private final Int2ObjectOpenHashMap<GraphNode> userNodeMapping = new Int2ObjectOpenHashMap<>();
    private final String name;
    private int numberOfWeights;

    public Hierarchy(String name) {
        this.name = name;
    }

    public int getNumberOfWeights() {
        return numberOfWeights;
    }

    public GraphNode getUserNode(int userId) {
        return userNodeMapping.get(userId);
    }

    public Int2ObjectOpenHashMap<GraphNode> getUserNodeMapping() {
        return userNodeMapping;
    }

    public void load(String clustersFile, boolean create, String indexersRoot, Hierarchy hier, boolean addNodesAsClusters) throws IOException {
        rootNode.setPath(indexersRoot);
        this.loadHierarchy(create, clustersFile, addNodesAsClusters);
        rootNode.setValues(hier.rootNode);
    }

    public void loadSingleClusterHierarchy(Hierarchy hier, String graphFile) {
        rootNode.setValues(hier.rootNode);
        hier.getUserNodeMapping().forEach(new BiConsumer<Integer, GraphNode>() {
            @Override
            public void accept(Integer t, GraphNode u) {
                userNodeMapping.put(t.intValue(), new GraphNode(u.getId(), rootNode));
            }
        });
        if (Configs.loadGraph) {
            this.readGraph(graphFile, false);
        }
    }

    public void load(boolean create, String rootIndexer, String indexersRoot, String clustersFile, boolean addNodesAsClusters) throws IOException {
        rootNode.setPath(indexersRoot);
        rootNode.setLevel((short) 0);

        if (clustersFile != null) {
            this.loadHierarchy(create, clustersFile, addNodesAsClusters);
        }

        rootNode.setPath(rootIndexer);
        String path = rootNode.getPath() + File.separator + Configs.indexName;
        File indexPath = new File(path);
        if (create) {
            FileUtils.deleteDirectory(indexPath);
        }
        if (!indexPath.exists() && create) {
            indexPath.mkdir();
        }
        if (create) {
            rootNode.setIndexer(createIndexer(path));
        }

    }

    protected IndexWriter createIndexer(String relativePath) throws IOException {
        return new IndexWriter(new SimpleFSDirectory(
                new File(relativePath).toPath()), new IndexWriterConfig(new DatasetMain.MyAnalyzer()));
    }

    public void loadHierarchy(boolean create, String clustersFile, boolean addNodesAsClusters) throws FileNotFoundException, CorruptIndexException, CorruptIndexException, LockObtainFailedException, IOException {
        Logger.getLogger(Hierarchy.class.getName()).log(Level.INFO, "Loading hierarchy started.");
        try (Scanner scanner = new Scanner(new File(clustersFile))) {
            String topLevelCluster = null;
            while (scanner.hasNextLine()) {
                String str = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(str, "\t");
                int code = Integer.parseInt(tokenizer.nextToken());
                StringTokenizer hierarchy = new StringTokenizer(tokenizer.nextToken(), ":");
                HierarchyNode currentNode = rootNode;
                currentNode.setId(-1);
                boolean tl = true;
                for (StringTokenizer stringTokenizer = hierarchy; stringTokenizer.hasMoreTokens();) {
                    String token = stringTokenizer.nextToken();
                    if (Configs.ignoreTopLevelCluster) {
                        if (tl) {
                            if (topLevelCluster != null) {
                                if (!topLevelCluster.equals(token)) {
                                    throw new RuntimeException();
                                }
                            } else {
                                topLevelCluster = token;
                            }
                            tl = false;
                            continue;
                        }
                    }
                    HierarchyNode get = currentNode.getChildren().get(Short.parseShort(token));
                    if (get == null) {
                        get = createHierarchyNode(currentNode, Integer.parseInt(token), create);
                    }
                    currentNode = get;
                }
                if (addNodesAsClusters) {
                    currentNode = createHierarchyNode(currentNode, -(code + 1), create);
                }
                GraphNode user = new GraphNode(DatasetMain.getInstance().getUser(code), currentNode);
                currentNode.addUser(user);
                userNodeMapping.put(code, user);
            }
        }
        userNodeMapping.trim();
        rootNode.optimize();
        Logger.getLogger(Hierarchy.class.getName()).log(Level.INFO, "Loading hierarchy finished.");
    }

    protected HierarchyNode createHierarchyNode(HierarchyNode currentNode, int token, boolean create) throws NumberFormatException, IOException {
        HierarchyNode get = new HierarchyNode(this, currentNode);
        currentNode.getChildren().put(token, get);
        get.setId(token);
        String pathh = currentNode.getPath() + File.separator + token;
        short level = (short) (currentNode.getLevel() + 1);
        get.setPath(pathh);
        get.setLevel(level);
        File fil = new File(pathh);
        String p = pathh + File.separator + "index";
        if (!fil.exists()) {
            fil.mkdir();
            File index = new File(p);
            index.mkdir();

        }
        if (create) {
            get.setIndexer(createIndexer(p));
        }
        return get;
    }

    public String getName() {
        return name;
    }

    public HierarchyNode getRootNode() {
        return rootNode;
    }

    public List<HierarchyNode> getHierarchyNodesChildFirst(int owner) {
        HierarchyNode current = this.getUserNode(owner).getHierarchyNode();
        List<HierarchyNode> list = new LinkedList<>();
        while (current != null) {
            list.add(current);
            current = current.getParent();
        }
        if (list.size() <= 1) {
            throw new RuntimeException();
        }
        list.remove(list.size() - 1);
        return list;
    }

    public List<HierarchyNode> getHierarchyNodesParentFirst(int owner) {
        List<HierarchyNode> hierarchyNodesChildFirst = getHierarchyNodesChildFirst(owner);
        Collections.reverse(hierarchyNodesChildFirst);
        return hierarchyNodesChildFirst;
    }

    public void readGraph(String graphFileName, boolean ignoreLastWeight) {
        Logger.getLogger(Hierarchy.class.getName()).log(Level.INFO, "Reading graph started.");
        boolean isChanged = false;
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(graphFileName)))) {
            while (sc.hasNextLine()) {
                String[] nextLine = sc.nextLine().split("[ ,;]");
                final int parseInt1 = Integer.parseInt(nextLine[0]);
                GraphNode node1 = this.getUserNodeMapping().get(parseInt1);
                if (node1 == null) {
                    node1 = addNode(parseInt1);
                    isChanged = true;
                }
                final int parseInt2 = Integer.parseInt(nextLine[1]);
                GraphNode node2 = this.getUserNodeMapping().get(parseInt2);
                if (node2 == null) {
                    node2 = addNode(parseInt2);
                    isChanged = true;
                }
                float[] weights;
                if (nextLine.length == 2) {
                    weights = new float[]{1.f};
                } else {
                    weights = new float[nextLine.length - 2 - (ignoreLastWeight ? 1 : 0)];
                    for (int i = 0; i < weights.length; i++) {
                        weights[i] = Float.parseFloat(nextLine[i + 2]);
                    }
                }
                numberOfWeights = weights.length;
                GraphNode.HierarchicalEdge edge = new GraphNode.HierarchicalEdge(node1, node2, weights);

                HierarchyNode hierarchyNode1 = node1.getHierarchyNode();
                HierarchyNode hierarchyNode2 = node2.getHierarchyNode();

                HierarchyNode temp1;
                HierarchyNode temp2;
                if (hierarchyNode1.getLevel() >= hierarchyNode2.getLevel()) {
                    temp1 = hierarchyNode1;
                    temp2 = hierarchyNode2;
                } else {
                    temp1 = hierarchyNode2;
                    temp2 = hierarchyNode1;
                }

                while (temp1.getLevel() > temp2.getLevel()) {
                    temp1 = temp1.getParent();
                }

                while (temp1 != temp2) {
                    temp1 = temp1.getParent();
                    temp2 = temp2.getParent();
                }

                edge.hierarchyThreshold = temp1.getLevel();
                node1.addEdge(edge);
                node2.addEdge(edge);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Hierarchy.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        if (isChanged) {
            userNodeMapping.trim();
            rootNode.optimize();
        }

        for (GraphNode o : this.getUserNodeMapping().values()) {
            o.optimize();
        }
        Logger.getLogger(Hierarchy.class.getName()).log(Level.INFO, "Reading graph finished.");
    }

    public GraphNode addNode(final int parseInt1) {
        GraphNode user = new GraphNode(DatasetMain.getInstance().getUser(parseInt1), rootNode);
        rootNode.addUser(user);
        userNodeMapping.put(parseInt1, user);
        return user;
    }

}
