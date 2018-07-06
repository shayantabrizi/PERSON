/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.hep;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.hierarchy.Hierarchy;
import ir.ac.ut.iis.person.hierarchy.HierarchyNode;
import ir.ac.ut.iis.person.paper.Paper;
import ir.ac.ut.iis.retrieval_tools.citeseerx.PapersReader;
import ir.ac.ut.iis.retrieval_tools.domain.MyIterable;
import ir.ac.ut.iis.retrieval_tools.papers.Author;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 *
 * @author Shayan
 */
public class HEPExtractor {

    public void doParseAndIndex(Hierarchy root, String topic, boolean makeRootIndex) {
        PapersReader papersReader = new PapersReader(new Indexer(root, topic, makeRootIndex));
        papersReader.run(Configs.datasetRoot+"papers_giant.txt");

        try {
            if (makeRootIndex) {
                root.getRootNode().getIndexer().commit();
            } else {
                doCommit(root.getRootNode());
            }
            System.out.println(" commited ...");
        } catch (IOException ex) {
            Logger.getLogger(HEPExtractor.class.getName()).log(Level.SEVERE, null, ex);
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

    private class Indexer implements MyIterable<ir.ac.ut.iis.retrieval_tools.papers.BasePaper> {

        private final Hierarchy root;
        private int count = 0;
        private final String topic;
        private Map<Integer, List<String>> readWeights;
        private final boolean makeRootIndex;

        private Indexer(Hierarchy root, String topic, boolean makeRootIndex) {
            this.root = root;
            this.topic = topic;
            if (topic != null) {
                readWeights = ir.ac.ut.iis.person.datasets.citeseerx.old.GraphExtractor.readWeights(Configs.datasetRoot + "graph-weights.txt", 10);
            }
            this.makeRootIndex = makeRootIndex;
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
                    null,
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
                    Logger.getLogger(HEPExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            if (count % 600_000 == 0) {
                try {
                    doCommit(root.getRootNode());
                    System.out.println(" commited ...");
                } catch (IOException ex) {
                    Logger.getLogger(HEPExtractor.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            return true;
        }

        protected void doIndex(Paper node, Hierarchy<?> hier) throws IOException {
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
            doc.add(new TextField("content", node.getText(), Field.Store.YES));
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
            doc.add(new TextField("content", node.getText(), Field.Store.NO));
            return doc;
        }

    }
}
