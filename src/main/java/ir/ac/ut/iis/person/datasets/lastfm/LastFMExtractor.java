package ir.ac.ut.iis.person.datasets.lastfm;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import ir.ac.ut.iis.retrieval_tools.domain.MyIterable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 *
 * @author shayan
 */
public class LastFMExtractor {

    private final Map<String, Track> trackMap = new HashMap<>();

    public Map<String, Track> getTrackMap() {
        return Collections.unmodifiableMap(trackMap);
    }

    public void loadTracks(Hierarchy hier, String users_track, String track_tag) {
        int cnt = 0;
        try (Scanner scanner = new Scanner(new File(users_track))) {
            while (scanner.hasNext()) {
                String str = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(str, ";");
                while (tokenizer.hasMoreTokens()) {
                    String userId = tokenizer.nextToken();
                    String trackID = tokenizer.nextToken();
                    String weight = tokenizer.nextToken();
                    Track t = trackMap.get(trackID);
                    if (t == null) {
                        t = new Track(Integer.valueOf(trackID));
                        trackMap.put(trackID, t);
                    }
                    LastFMUser u = (LastFMUser) hier.getUserNode(Integer.parseInt(userId)).getId();
                    if (u == null) {
                        cnt++;
                        continue;
                    }
                    t.addUser(u, Integer.parseInt(weight));
                    u.addDocWeight(t, Integer.parseInt(weight));
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LastFMExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

        try (Scanner scanner = new Scanner(new File(track_tag))) {
            while (scanner.hasNext()) {
                String str = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(str, ";");

                while (tokenizer.hasMoreTokens()) {
                    String tag = tokenizer.nextToken();
                    String id = tokenizer.nextToken();

                    Track t = trackMap.get(id);
                    if (t == null) {
                        continue;
                    }
                    t.addTag(tag);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LastFMExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public void doParseAndIndex(Hierarchy root, boolean makeRootIndex, String users_track, String track_tag) {
        loadTracks(root, users_track, track_tag);
        Indexer indexer = new Indexer(root, makeRootIndex);
        for (Track t : trackMap.values()) {
            indexer.doAction(t);
        }

        try {
            if (makeRootIndex) {
                root.getRootNode().getIndexer().commit();
            } else {
                doCommit(root.getRootNode());
            }
            System.out.println(" commited ...");
        } catch (IOException ex) {
            Logger.getLogger(LastFMExtractor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    protected void doCommit(HierarchyNode root) throws IOException {
        root.getIndexer().commit();
        //        root.getIndexer().waitForMerges();
        for (HierarchyNode value : root.getChildren().values()) {
            doCommit(value);
        }
    }

    private class Indexer implements MyIterable<Track> {

        private final Hierarchy root;
        private int count = 0;
        private final boolean makeRootIndex;

        private Indexer(Hierarchy root, boolean makeRootIndex) {
            this.root = root;
            this.makeRootIndex = makeRootIndex;
        }

        @Override
        public boolean doAction(Track t) {
            if (count % 10_000 == 0) {
                System.out.println(count + " " + new Date());
            }

            count++;
            try {
                doIndex(t, root);
            } catch (IOException ex) {
                Logger.getLogger(LastFMExtractor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            if (count % 600_000 == 0) {
                try {
                    doCommit(root.getRootNode());
                    System.out.println(" commited ...");
                } catch (IOException ex) {
                    Logger.getLogger(LastFMExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            return true;
        }

        protected void doIndex(Track node, Hierarchy hier) throws IOException {
            Document doc;
            if (makeRootIndex) {
                doc = makeCompleteDocument(node);
                hier.getRootNode().getIndexer().addDocument(doc);
                return;
            } else {
                doc = makeDocument(node);
            }
            for (LastFMUser user : node.getUsers()) {
                List<HierarchyNode> list = hier.getHierarchyNodesParentFirst(user.getId());

                if (list.isEmpty()) {
//                System.out.println(node.intID + " " + node.creators.get(0).getId() + " " + currentNode.path + " not exists");
//            throw new RuntimeException();
                    continue;
                }

                for (HierarchyNode n : list) {
                    n.getIndexer().addDocument(doc);
                }
            }
        }

        public Document makeDocument(Track node) {
            org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
            doc.add(new StringField("id", String.valueOf(node.getId()), Field.Store.YES));
            doc.add(new TextField("content", node.getText(), Field.Store.NO));
            return doc;
        }

        public Document makeCompleteDocument(Track node) {
            org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
            doc.add(new StringField("id", String.valueOf(node.getId()), Field.Store.YES));
            doc.add(new TextField("content", node.getText(), Field.Store.YES));
//            StringBuilder users = new StringBuilder();
//            for (Map.Entry<String, Integer> c : node.getUserWeights()) {
//                users.append(c.getKey()).append(" ").append(c.getValue()).append(";");
//            }
//            doc.add(new Field("users", users.toString(), Field.Store.YES, Field.Index.ANALYZED));
            return doc;
        }
    }
}
