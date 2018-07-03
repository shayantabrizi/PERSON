/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.others;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.DatasetMain;
import ir.ac.ut.iis.person.paper.PapersRetriever;
import ir.ac.ut.iis.retrieval_tools.citeseerx.PapersReader;
import ir.ac.ut.iis.retrieval_tools.domain.MyIterable;
import ir.ac.ut.iis.retrieval_tools.papers.Author;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class ExportDataset {

    public static void main(String[] args) {
        try (Writer export = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(Configs.datasetRoot + "docs-mallet.txt")))) {
            final DocumentExporter exporter = new DocumentExporter(export);
//            final AuthorExporter exporter = new AuthorExporter();
            PapersReader papersReader = new PapersReader(exporter);
            papersReader.run(Configs.datasetRoot + "papers_giant.txt");
//            exporter.writeAuthors(export);
        } catch (IOException ex) {
            Logger.getLogger(ExportDataset.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

    private static class AuthorExporter implements MyIterable<ir.ac.ut.iis.retrieval_tools.papers.BasePaper> {

        private int count = 0;
        private final Map<Integer, StringBuilder> map = new HashMap<>();

        @Override
        public boolean doAction(ir.ac.ut.iis.retrieval_tools.papers.BasePaper p) {
            if (count % 10_000 == 0) {
                System.out.println(count + " " + new Date());
            }

            String text = p.getTitle() + " " + p.getAbs();

            List<String> tokenizeString = PapersRetriever.tokenizeString(text);
            StringBuilder sb = new StringBuilder();
            for (String s : tokenizeString) {
                sb.append(s).append(" ");
            }
            for (Author a : p.getAuthors()) {
                StringBuilder get = map.get(a.getId());
                if (get == null) {
                    get = new StringBuilder();
                    map.put(a.getId(), get);
                }
                get.append(" ").append(sb);
            }

            count++;
            return true;
        }

        public void writeAuthors(Writer writer) {
            for (Map.Entry<Integer, StringBuilder> e : map.entrySet()) {
                try {
                    writer.write(String.valueOf(e.getKey()));
                    writer.write(" 0 ");
                    writer.write(e.getValue().toString());
                    writer.write("\n");
                } catch (IOException ex) {
                    Logger.getLogger(ExportDataset.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    private static class DocumentExporter implements MyIterable<ir.ac.ut.iis.retrieval_tools.papers.BasePaper> {

        private int count = 0;
        private final Writer writer;

        private DocumentExporter(Writer writer) {
            this.writer = writer;
        }

        @Override
        public boolean doAction(ir.ac.ut.iis.retrieval_tools.papers.BasePaper p) {
            if (count % 10_000 == 0) {
                System.out.println(count + " " + new Date());
            }

            String text = p.getTitle() + " " + p.getAbs();

            List<String> tokenizeString = PapersRetriever.tokenizeString(text);
            StringBuilder sb = new StringBuilder();
            for (String s : tokenizeString) {
                sb.append(s).append(" ");
            }

            try {
                writer.write(String.valueOf(p.getId()));
                writer.write(" 0 ");
                writer.write(sb.toString());
                writer.write("\n");
            } catch (IOException ex) {
                Logger.getLogger(ExportDataset.class.getName()).log(Level.SEVERE, null, ex);
            }

            count++;
            return true;
        }

    }
}
