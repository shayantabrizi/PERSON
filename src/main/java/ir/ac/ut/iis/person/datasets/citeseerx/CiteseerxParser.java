/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.citeseerx;

import ir.ac.ut.iis.retrieval_tools.crawler.Parser;
import ir.ac.ut.iis.retrieval_tools.papers.PreprocessedPaper;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

/**
 *
 * @author shayan
 */
public class CiteseerxParser implements Parser<PreprocessedPaper> {

    private final Digester digester;

    public CiteseerxParser() {
        digester = new Digester();
        digester.addObjectCreate("OAI-PMH/ListRecords", LinkedList.class);
        digester.addObjectCreate("OAI-PMH/ListRecords/record", PreprocessedPaper.class);
        digester.addSetNext("OAI-PMH/ListRecords/record", "add");
        digester.addBeanPropertySetter("OAI-PMH/ListRecords/record/header/identifier", "docId");
        digester.addBeanPropertySetter("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:title", "title");
        digester.addBeanPropertySetter("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:description", "abs");
        digester.addBeanPropertySetter("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:identifier", "identifier");
        digester.addBeanPropertySetter("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:source", "source");
        digester.addBeanPropertySetter("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:language", "lang");

        digester.addCallMethod("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:date", "addDate", 1);
        digester.addCallParam("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:date", 0);
        digester.addCallMethod("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:creator", "addCreator", 1);
        digester.addCallParam("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:creator", 0);
        digester.addCallMethod("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:subject", "addSubject", 1);
        digester.addCallParam("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:subject", 0);
        digester.addCallMethod("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:relation", "addRelation", 1);
        digester.addCallParam("OAI-PMH/ListRecords/record/metadata/oai_dc:dc/dc:relation", 0);
    }

    @Override
    public PreprocessedPaper parse(String content) {
        try {
            return digester.parse(content);
        } catch (IOException | SAXException ex) {
            Logger.getLogger(CiteseerxParser.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
    }

}
