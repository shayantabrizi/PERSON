/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.lastfm;

import ir.ac.ut.iis.retrieval_tools.domain.Document;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author GOMROK IRAN
 */
public class Track extends Document {

    private final List<String> tags = new LinkedList<>();
    private final Map<LastFMUser, Integer> userWeights = new TreeMap<>();

    public Track(int id) {
        super(id);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void addUser(LastFMUser user, Integer weight) {
        userWeights.put(user, weight);
    }

    public Set<LastFMUser> getUsers() {
        return userWeights.keySet();
    }

    public Map<LastFMUser, Integer> getUserWeights() {
        return Collections.unmodifiableMap(userWeights);
    }

    @Override
    public String getText() {
        StringBuilder s = new StringBuilder();
        for (String tag : tags) {
            s.append(" ").append(tag);
        }
        return s.toString();
    }

    public int compareTo(Track o) {
        return Integer.compare(getId(), o.getId());
    }

}
