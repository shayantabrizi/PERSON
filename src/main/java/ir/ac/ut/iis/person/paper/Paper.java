/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import ir.ac.ut.iis.retrieval_tools.domain.Document;
import ir.ac.ut.iis.retrieval_tools.papers.Author;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shayan
 */
public class Paper extends Document {

    /**
	 * 
	 */
	private static final long serialVersionUID = 657890078640254881L;
	private final String name;
    private final String paperAbstract;
    private final String title;
    private final Integer year;
    private final boolean isMerged;
    private List<Author> creators = new LinkedList<>();
    private List<String> references = new LinkedList<>();
    private Map<Integer, Double> topics;

    public Paper(int id, String name, String paperAbstract, String title, Integer year, boolean isMerged) {
        super(id);
        this.name = name;
        this.paperAbstract = paperAbstract;
        this.title = title;
        this.year = year;
        this.isMerged = isMerged;
    }

    public String getName() {
        return name;
    }

    public String getPaperAbstract() {
        return paperAbstract;
    }

    public String getTitle() {
        return title;
    }

    public boolean isIsMerged() {
        return isMerged;
    }

    public List<Author> getCreators() {
        return Collections.unmodifiableList(creators);
    }

    public List<String> getReferences() {
        return Collections.unmodifiableList(references);
    }

    public void setCreators(List<Author> creators) {
        this.creators = creators;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public Map<Integer, Double> getTopics() {
        return Collections.unmodifiableMap(topics);
    }

    public void setTopics(Map<Integer, Double> topics) {
        this.topics = topics;
    }

    public Integer getYear() {
        return year;
    }

    @Override
    public String getText() {
        return title + " " + paperAbstract;
    }

}
