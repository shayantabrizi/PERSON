/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import ir.ac.ut.iis.person.datasets.citeseerx.CiteseerxPreprocessor;
import ir.ac.ut.iis.retrieval_tools.Config;
import ir.ac.ut.iis.retrieval_tools.citeseerx.PapersReader;
import ir.ac.ut.iis.retrieval_tools.domain.Edge;
import ir.ac.ut.iis.retrieval_tools.papers.Author;
import ir.ac.ut.iis.retrieval_tools.papers.BasePaper;
import ir.ac.ut.iis.retrieval_tools.papers.PreprocessedPaper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author shayan
 */
public class PapersPreprocessor {

    private int paperCount;
    private int authorCount;
    private final Map<String, BasePaper> papersMap = new HashMap<>();
    private final Map<AuthorRepresentation, Author> authorsMap = new HashMap<>();
    private final Map<BasePaper, List<String>> refsMap = new HashMap<>();
    private final Map<BasePaper, List<String>> creatorsMap = new HashMap<>();
    private final Map<String, BasePaper> papersMapByTitle = new HashMap<>();
    private final Map<String, String> paperIdConvertMap = new HashMap<>();
    private final AuthorFactory af;

    private long processableRefsCount = 0;
    private long unprocessableRefsCount = 0;
    private long authorsWeights = 0;
    private long authorsEdges = 0;
    private long mergedPapers = 0;

    int tryCount = 0;

    Rewriter rewriter = new Rewriter("&#([x|X])?([0-9A-Fa-f]{2,4});") {
        @Override
        public String replacement() {
            final String group1 = group(1);
            String group2 = group(2);
            int s;
            if (group2.equals("211C") || group2.equals("002F")) {
                s = Integer.parseInt(group(2), 16);
            } else {
                s = Integer.parseInt(group(2), group1 == null ? 10 : 16);
            }
            return String.valueOf(Character.toChars(s));
        }
    };

    public PapersPreprocessor(AuthorFactory af) {
        this.af = af;
    }

    public boolean processPaper(Object o) throws RuntimeException {
        tryCount++;
        if (tryCount % 10_000 == 0) {
            System.out.println(tryCount + " " + paperCount);
        }
        PreprocessedPaper p = (PreprocessedPaper) o;
        cleanPaper(p);
        String simplifiedPaperTitle = p.getTitle().replaceAll(" |\\-|\\.|'|,", "").toLowerCase();
        if (validate(p) == false) {
            return true;
        }
        BasePaper paper;
        try {
            paper = new BasePaper(p, ++paperCount);
        } catch (ParseException ex) {
            Logger.getLogger(CiteseerxPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
//                return false;
        }
        if (papersMap.containsKey(paper.getDocId())) {
            Logger.getLogger(CiteseerxPreprocessor.class.getName()).log(Level.WARNING, "Error {0}", paper.getDocId());
            throw new RuntimeException();
//                return false;
        }
        BasePaper pp = papersMapByTitle.get(simplifiedPaperTitle);
        if (pp != null) {
            updatePaper(pp, paper, p.getRelations(), p.getCreators());
            paperIdConvertMap.put(paper.getDocId(), pp.getDocId());
            paperCount--;
            return true;
        }
        papersMap.put(paper.getDocId(), paper);
        papersMapByTitle.put(p.getTitle(), paper);
        List<String> creators = new LinkedList<>();
        for (String s : p.getCreators()) {
            creators.add(rewriter.rewrite(s));
        }
        creatorsMap.put(paper, creators);
        refsMap.put(paper, p.getRelations());
        return false;
    }

    protected void processAuthors() {
        for (Map.Entry<BasePaper, List<String>> e : creatorsMap.entrySet()) {
            Set<AuthorRepresentation> localSet = new HashSet<>();
            for (String a : e.getValue()) {
                AuthorRepresentation disambiguatedAuthor = af.create(a);
                if (localSet.contains(disambiguatedAuthor)) {
                    continue;
                } else {
                    localSet.add(disambiguatedAuthor);
                }
                Author get = authorsMap.get(disambiguatedAuthor);
                if (get == null) {
                    get = new Author(++authorCount, disambiguatedAuthor.toString());
                    authorsMap.put(disambiguatedAuthor, get);
                }
                e.getKey().addAuthor(get);
            }
        }

        for (BasePaper paper : papersMap.values()) {
//            if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
//                throw new RuntimeException();
//            }
            if (paper.isIsMerged()) {
                mergedPapers++;
                throw new RuntimeException();
            }
            for (Author src : paper.getAuthors()) {
                for (Author dst : paper.getAuthors()) {
                    if (src.getId().compareTo(dst.getId()) < 0) {
                        boolean ch = false;
                        authorsWeights++;
                        for (Edge<Author> e : src.getEdges()) {
                            if (e.getOtherSide(src).equals(dst)) {
                                e.getWeight()[0] += 1;
                                ch = true;
                                break;
                            }
                        }
                        if (!ch) {
                            Edge<Author> edge = new Edge<>(src, dst);
                            src.addEdge(edge);
                            dst.addEdge(edge);
                            authorsEdges++;
                        }
                    }
                }
            }
        }
    }

    protected void cleanPaper(PreprocessedPaper p) {
        p.setTitle(rewriter.rewrite(StringUtils.stripAccents(p.getTitle().trim())));
        String trim = rewriter.rewrite(StringUtils.stripAccents(p.getAbs()).trim());

        String toLowerCase = trim.toLowerCase();
        if (toLowerCase.startsWith("abstract")) {
            if (toLowerCase.startsWith("abstract ")
                    || toLowerCase.startsWith("abstract.")
                    || toLowerCase.startsWith("abstract-")
                    || toLowerCase.startsWith("abstract⎯")
                    || toLowerCase.startsWith("abstract–")
                    || toLowerCase.startsWith("abstract—")
                    || toLowerCase.startsWith("abstractÐ")
                    || toLowerCase.startsWith("abstract:")) {
                trim = trim.substring(8).trim();
                if (trim.startsWith(".")
                        || trim.startsWith("-")
                        || trim.startsWith("⎯")
                        || trim.startsWith("–")
                        || trim.startsWith("—")
                        || trim.startsWith("ð")
                        || trim.startsWith(":")) {
                    trim = trim.substring(1);
                }
            }
        }
        p.setAbs(trim.trim());
        p.setDocId(p.getDocId().trim());
    }

    private void processRefs() {
        for (Iterator<Map.Entry<BasePaper, List<String>>> it = refsMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BasePaper, List<String>> e = it.next();
            BasePaper p = e.getKey();
            List<String> refs = e.getValue();
            Set<String> localSet = new HashSet<>();
            for (String r : refs) {
                String convertedId = paperIdConvertMap.get(r);
                if (convertedId == null) {
                    convertedId = r;
                }

                if (localSet.contains(convertedId)) {
                    continue;
                } else {
                    localSet.add(convertedId);
                }
//                System.out.println(convertedId);
                BasePaper get = papersMap.get(convertedId);
                if (get == null) {
                    p.addUnprocessableRef(convertedId);
                    unprocessableRefsCount++;
                } else {
                    p.addRef(get);
                    processableRefsCount++;
                }
            }
            it.remove();
        }
        System.out.println(processableRefsCount + " " + unprocessableRefsCount + " " + paperCount + " " + authorCount + " " + authorsEdges + " " + authorsWeights + " " + paperIdConvertMap.size() + " " + mergedPapers);
    }

    public void saveDataset(String papersFile, String authorsFile) throws IOException {
        try (OutputStreamWriter papers = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(papersFile)))) {
            for (BasePaper p : papersMap.values()) {
                papers.write(p.getId() + "\n");
                papers.write((p.isIsMerged() == true ? 1 : 0) + "\n");
                papers.write((p.getDocId() == null ? "" : p.getDocId().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");
                papers.write((p.getIdentifier() == null ? "" : p.getIdentifier().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");
                papers.write((p.getSource() == null ? "" : p.getSource().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");
                papers.write((p.getURI() == null ? "" : p.getURI().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");
                papers.write((p.getLang() == null ? "" : p.getLang().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");

                papers.write((p.getTitle() == null ? "" : p.getTitle().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");
                papers.write((p.getAbs() == null ? "" : p.getAbs().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");
                papers.write(p.getDate() + "\n");
                for (String s : p.getSubjects()) {
                    papers.write((s == null ? "" : s.replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ").replace(",", " ")) + ",");
                }
                papers.write("\n");
                for (String s : p.getUnprocessableRefs()) {
                    papers.write((s == null ? "" : s.replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ").replace(",", " ")) + ",");

                }
                papers.write("\n");
                for (BasePaper paper : p.getRefs()) {
                    papers.write(paper.getId() + ",");
                }
                papers.write("\n");
                for (Author author : p.getAuthors()) {
                    papers.write(author.getId() + ",");
                }
                papers.write("\n");
            }
        }
        try (OutputStreamWriter authors = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(authorsFile)))) {
            for (Author a : authorsMap.values()) {
                authors.write(a.getId() + "\n");
                authors.write((a.getName() == null ? "" : a.getName().replaceAll("[\n\r\uDB4A\uDCA9\u0085]", " ")) + "\n");

                for (Edge<Author> edge : a.getEdges()) {
                    Author b = edge.getOtherSide(a);
                    if (a.getId().compareTo(b.getId()) < 0) {
                        authors.write(b.getId() + " " + edge.getWeight()[0] + ",");
                    }
                }
                authors.write("\n");
            }
        }

    }

    public static void convertAuthorsFileToGraphs(String authorsFile, String graphFile) {
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(authorsFile))).useDelimiter("\n")) {
            try (OutputStreamWriter os = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(graphFile)))) {
                while (sc.hasNext()) {
                    String nextLine = sc.next();
                    if (nextLine.isEmpty()) {
                        break;
                    }
//                    System.out.println(nextLine);
                    long src = Long.parseLong(nextLine);
                    sc.next();
                    String edges = sc.next();
                    if (!edges.isEmpty()) {
                        String[] split = edges.split(",");
                        for (String s : split) {
//                            System.out.println(s);
                            String[] split1 = s.split(" ");
                            long dst = Long.parseLong(split1[0]);
                            Integer weight = Integer.parseInt(split1[1].substring(0, split1[1].length() - 2));
                            os.write(src + "," + dst + "," + weight + "\n");
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PapersPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PapersPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    public static void convertPapersFileToGraph(String papersFile, String graphFile) {
        try (OutputStreamWriter os = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(graphFile)))) {
            ir.ac.ut.iis.retrieval_tools.domain.MyIterable<BasePaper> iterable = new ir.ac.ut.iis.retrieval_tools.domain.MyIterable<BasePaper>() {

                @Override
                public boolean doAction(BasePaper d) {
                    for (String r : d.getUnprocessableRefs()) {
                        try {
                            os.write(d.getId() + " " + r + "\n");
                        } catch (IOException ex) {
                            Logger.getLogger(CiteseerxPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    return true;
                }
            };
            PapersReader papersReader = new PapersReader(iterable);
            papersReader.run(papersFile);
        } catch (IOException ex) {
            Logger.getLogger(PapersPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

    }

    private boolean validate(PreprocessedPaper p) {
        String title = clean(p.getTitle());
        if (title.startsWith("LIST OF")) {
            return false;
        }
        if (title.startsWith("INCLUDING ANY IMPLIED")) {
            return false;
        }
        if (title.matches("Proceedings of the .* Winter Simulation Conference")) {
            return false;
        }
        final int countTokens = countTokens(title);

        if (countTokens <= 2 || countTokens > 50) {
            return false;
        }

        String abs = clean(p.getAbs());
        final int countTokens1 = countTokens(abs);
        if (countTokens1 <= 25 || countTokens1 > 500) {
            return false;
        }

        if ((abs.trim() + title.trim()).length() <= 200) {
            return false;
        }

        if (p.getCreators().isEmpty()) {
            return false;
        }
        return true;

    }

    private int countTokens(String title) {
        String[] split = title.split(" ");
        int check = 0;
        for (String s : split) {
            if (!Config.stopWords.contains(s)) {
                check++;
            }
        }
        return check;
    }

    private String clean(String get) {
        get = get.replace('+', ' ');
        get = get.replace('-', ' ');
        get = get.replaceAll("&&", " ");
        get = get.replaceAll("\\|\\|", " ");
        get = get.replace('!', ' ');
        get = get.replace('(', ' ');
        get = get.replace(')', ' ');
        get = get.replace('{', ' ');
        get = get.replace('}', ' ');
        get = get.replace('[', ' ');
        get = get.replace(']', ' ');
        get = get.replace('^', ' ');
        get = get.replace('\"', ' ');
        get = get.replace('~', ' ');
        get = get.replace('*', ' ');
        get = get.replace('?', ' ');
        get = get.replace(':', ' ');
        get = get.replace('\\', ' ');
        get = get.replace('/', ' ');
        get = get.trim();

        return get;
    }

    private void updatePaper(BasePaper pp, BasePaper p, List<String> relations, List<String> creators) {
        boolean isMerged = false;
        boolean isOriginal = false;
//        System.out.println("----" + pp.getTitle() + "----");

        if (p.getTitle().length() > pp.getTitle().length()) {
            pp.setTitle(p.getTitle());
            isMerged = true;
        } else if (!p.getTitle().equals(pp.getTitle())) {
            isOriginal = true;
        }

        if (p.getAbs().length() > pp.getAbs().length()) {
            pp.setAbs(p.getAbs());
            isMerged = true;
        } else if (!p.getAbs().equals(pp.getAbs())) {
            isOriginal = true;
        }

        if (p.getDate() != null && pp.getDate() == null) {
            pp.setDate(p.getDate());
        }

        if (p.getSubjects().size() > pp.getSubjects().size()) {
            pp.setSubjects(p.getSubjects());
        }

        List<String> get = creatorsMap.get(pp);
        if (creators.size() > get.size()) {
            creatorsMap.put(pp, creators);
            isMerged = true;
        } else {
            Set<Object> set1 = new HashSet<>();
            set1.addAll(creators);
            Set<Object> set2 = new HashSet<>();
            set2.addAll(get);
            if (!set1.equals(set2)) {
                isOriginal = true;
            }
        }

        List<String> get2 = refsMap.get(pp);
        if (relations.size() > get2.size()) {
            refsMap.put(pp, relations);
            isMerged = true;
        } else {
            Set<Object> set1 = new HashSet<>();
            set1.addAll(relations);
            Set<Object> set2 = new HashSet<>();
            set2.addAll(get2);
            if (!set1.equals(set2)) {
                isOriginal = true;
            }
        }

        if (isMerged) {
            if (isOriginal) {
                pp.setIsMerged(true);
            } else {
                pp.setIsMerged(false);
            }
        }

//                if (!pp.getTitle().toLowerCase().equals(p.getTitle().toLowerCase())) {
//                    System.out.println("Title:\n" + pp.getTitle() + "\n" + p.getTitle() + "\n-------------------------------");
//                }
//                if (!pp.getAbs().toLowerCase().equals(p.getAbs().toLowerCase())) {
//                    System.out.println("Abs:\n" + pp.getAbs() + "\n" + p.getAbs() + "\n-------------------------------");
//                }
//                if (!Objects.equals(pp.getDate(), p.getDate())) {
//                    System.out.println("Date:\n" + pp.getDate() + "\n" + p.getDate() + "\n-------------------------------");
//                }
        ////        if (!pp.getDocId().equals(p.getDocId())) {
        ////            System.out.println("DocId:\n" + pp.getDocId() + "\n" + p.getDocId() + "\n-------------------------------");
        ////        }
        ////        if (!pp.getIdentifier().equals(p.getIdentifier())) {
        ////            System.out.println("Identifier:\n" + pp.getIdentifier() + "\n" + p.getIdentifier() + "\n-------------------------------");
        ////        }
//                if (!pp.getLang().equals(p.getLang())) {
//                    System.out.println("Lang:\n" + pp.getLang() + "\n" + p.getLang() + "\n-------------------------------");
//                }
        ////        if (!Objects.equals(pp.getSource(), p.getSource())) {
        ////            System.out.println("Source:\n" + pp.getSource() + "\n" + p.getSource() + "\n-------------------------------");
        ////        }
//                Set<Object> set1 = new HashSet<>();
//                set1.addAll(pp.getSubjects());
//                Set<Object> set2 = new HashSet<>();
//                set2.addAll(p.getSubjects());
//                if (!set1.equals(set2)) {
//                    System.out.println("Subjects:\n" + StringUtils.join(pp.getSubjects()) + "\n" + StringUtils.join(p.getSubjects()) + "\n-------------------------------");
//                }
//                if (!pp.getAuthors().equals(p.getAuthors())) {
//                    System.out.println("Authors:\n" + StringUtils.join(pp.getAuthors()) + "\n" + StringUtils.join(p.getAuthors()) + "\n-------------------------------");
//                }
        //
//                set1.clear();
//                set1.addAll(get);
//                set2.clear();
//                set2.addAll(relations);
//        
//                if (!set1.equals(set2)) {
//                    System.out.println("Refs:\n" + StringUtils.join(get) + "\n" + StringUtils.join(relations) + "\n-------------------------------");
//                }
    }

    public void postProcess() {
        processAuthors();
        processRefs();
        try {
            saveDataset("papers.txt", "authors.txt");
        } catch (IOException ex) {
            Logger.getLogger(PapersPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    static interface AuthorFactory {

        AuthorRepresentation create(String s);
    }

    static interface AuthorRepresentation {

        @Override
         String toString();

        @Override
         boolean equals(Object obj);

        @Override
         int hashCode();

    }

}
