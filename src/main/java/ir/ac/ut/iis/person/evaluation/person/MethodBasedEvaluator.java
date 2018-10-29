/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.evaluation.person;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.paper.PapersInvertedRetriever;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 *
 * @author shayan
 */
public class MethodBasedEvaluator extends PERSONEvaluator {

    private final Map<String, QueryInfo> queriesInfo = new HashMap<>();
    private final int numOfJudgments;
    private final String judgmentsFileName;

    public MethodBasedEvaluator(String judgmentsFileName) {
        numOfJudgments = readResultsFile(judgmentsFileName, queriesInfo);
        this.judgmentsFileName = judgmentsFileName;
    }

    private static int readResultsFile(String judgmentsFileName, final Map<String, QueryInfo> info) throws RuntimeException {
        return readResultsFile(judgmentsFileName, info, -1, false);
    }

    private static int readResultsFile(String judgmentsFileName, final Map<String, QueryInfo> info, int limit, boolean check) throws RuntimeException {
        Integer numOfJudgments = null;
        try (Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(judgmentsFileName)))) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                final String[] read = nextLine.split(",");
                String[] split = read[2].split(" ");
                List<String> judgments = new ArrayList<>();
                for (String s : split) {
                    judgments.add(s);
                }
                if (numOfJudgments == null) {
                    numOfJudgments = split.length;
                } else if (check && numOfJudgments != split.length) {
                    continue;
                }
                Set<String> ignoredSelfCitations = null;
                if (Configs.ignoreSelfCitations) {
                    ignoredSelfCitations = new HashSet<>();
                    split = read[3].split(" ");
                    ignoredSelfCitations.addAll(Arrays.asList(split));
                }
                info.put(read[0], new QueryInfo(judgments, ignoredSelfCitations));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MethodBasedEvaluator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        return numOfJudgments;
    }

    @Override
    protected Map<String, Double> findRelevants(String docId, Integer year, Set<String> ignoredSelfCitations) {
        searcher.setSimilarity(new ClassicSimilarity());
        final Map<String, Double> map = new HashMap<>();
        String[] authors;
        int docIndexId;
        try {
            TopDocs results = searcher.search(new TermQuery(new Term("id", docId)), 1);
            docIndexId = results.scoreDocs[0].doc;
            authors = searcher.getIndexReader().document(docIndexId).get("authors").split(" ");
        } catch (IOException ex) {
            Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        final QueryInfo queryInfo = queriesInfo.get(docId);
        if (queryInfo == null) {
            return null;
        }
        if (Configs.ignoreSelfCitations) {
            ignoredSelfCitations.addAll(queryInfo.ignoredSelfCitations);
        }
        for (String r : queryInfo.judgments) {
            if (r.equals(docIndexId)) {
                throw new RuntimeException();
            }
            try {
                Document document = searcher.getIndexReader().document(Integer.parseInt(r));
                if (Configs.yearFiltering) {
                    int refYear = Integer.parseInt(document.get("year"));
                    if (refYear > year) {
                        throw new RuntimeException();
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PapersInvertedRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            String[] refAuthors;
            try {
                final Document document = searcher.getIndexReader().document(Integer.parseInt(r));
                refAuthors = document.get("authors").split(" ");
            } catch (IOException ex) {
                Logger.getLogger(PapersRetriever.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
            if (Configs.ignoreSelfCitations) {
                for (String s : refAuthors) {
                    for (String t : authors) {
                        if (t.equals(s)) {
                            throw new RuntimeException();
                        }
//                            if (Configs.ignoreSelfCitations == 1) {
//                                break;
//                            }
                    }
                }
            }
            map.put(r, 1.);
        }

        if (map.size() <= 5) {
            throw new RuntimeException();
        }
        return map;
    }

    @Override
    public String getName() {
        return "MethodBased[" + Configs.personParameters() + ",NOJ=" + numOfJudgments + ",JFN=" + judgmentsFileName + "]";
    }

    private static class QueryInfo {

        List<String> judgments;
        Set<String> ignoredSelfCitations;

        public QueryInfo(List<String> judgments, Set<String> ignoredSelfCitations) {
            this.judgments = judgments;
            this.ignoredSelfCitations = ignoredSelfCitations;
        }

    }

    public static void main(String[] args) {
        String[] baselineFiles = {"LM-100-queries.txt", "MyLM-400-queries.txt", "Classic-queries.txt"};
        String judgmentsFileName = "IRR-queries.txt";
        int limit = 100;
        Map<String, Map<String, QueryInfo>> baselineInfos = new HashMap<>();
        for (String s : baselineFiles) {
            HashMap<String, QueryInfo> info = new HashMap<>();
            baselineInfos.put(s, info);
            readResultsFile(s, info, limit, true);
        }
        Map<String, QueryInfo> judgmentsInfo = new HashMap<>();
        readResultsFile(judgmentsFileName, judgmentsInfo, limit, true);
        aggregate2(judgmentsInfo, baselineFiles, baselineInfos);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("queries-agg.txt"))) {
            for (Map.Entry<String, QueryInfo> e : judgmentsInfo.entrySet()) {
                if (e.getValue().judgments.size() <= 5) {
                    continue;
                }
                writer.write(e.getKey() + ",,");
                for (String j : e.getValue().judgments) {
                    writer.write(j + " ");
                }
                writer.write(",");
                if (e.getValue().ignoredSelfCitations != null) {
                    for (String s : e.getValue().ignoredSelfCitations) {
                        writer.write(s + " ");
                    }
                }
                writer.write("\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(MethodBasedEvaluator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }

    }

    private static void aggregate(Map<String, QueryInfo> judgmentsInfo, String[] baselineFiles, Map<String, Map<String, QueryInfo>> baselineInfos) {
        for (Iterator<Map.Entry<String, QueryInfo>> it = judgmentsInfo.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, QueryInfo> e = it.next();
            boolean check = false;
            for (String s : baselineFiles) {
                final QueryInfo inf = baselineInfos.get(s).get(e.getKey());
                if (inf == null) {
                    check = true;
                    break;
                }
                e.getValue().judgments.removeAll(inf.judgments);
            }
            if (check) {
                it.remove();
            }
        }
    }

    private static void aggregate2(Map<String, QueryInfo> judgmentsInfo, String[] baselineFiles, Map<String, Map<String, QueryInfo>> baselineInfos) {
        for (Iterator<Map.Entry<String, QueryInfo>> it = judgmentsInfo.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, QueryInfo> e = it.next();
            boolean check = false;
            Map<String, Map<String, Integer>> map = new HashMap<>();
            for (String s : baselineFiles) {
                final QueryInfo inf = baselineInfos.get(s).get(e.getKey());
                if (inf == null) {
                    check = true;
                    break;
                }
                HashMap<String, Integer> m = new HashMap<>();
                map.put(s, m);
                int i = 0;
                for (String j : inf.judgments) {
                    m.put(j, i);
                    i++;
                }
            }
            if (check) {
                it.remove();
                continue;
            }
            HashMap<String, Integer> my = new HashMap<>();
            int i = 0;
            for (String j : e.getValue().judgments) {
                my.put(j, i);
                i++;
            }
            for (Map.Entry<String, Integer> j : my.entrySet()) {
                int min = Integer.MAX_VALUE;
                for (String s : baselineFiles) {
                    Integer get = map.get(s).get(j.getKey());
                    if (get != null) {
                        if (get < min) {
                            min = get;
                        }
                    }
                }
                if (min == Integer.MAX_VALUE) {
                    min = 100;
                }
                if (j.getValue() > min - 10 && !(j.getValue() < 10 && min >= 10)) {
                    e.getValue().judgments.remove(j.getKey());
                }
            }
        }
    }
}
