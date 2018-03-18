/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.lastfm;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author shayan
 */
public class LastFMUser extends ir.ac.ut.iis.person.hierarchy.User {

    private final Map<Track, Integer> docWeights = new TreeMap<>();

    public LastFMUser(int id) {
        super(id);
    }

    public Map<Track, Integer> getDocWeights() {
        return Collections.unmodifiableMap(docWeights);
    }

    public void addDocWeight(Track t, Integer weight) {
        docWeights.put(t, weight);
    }

}
