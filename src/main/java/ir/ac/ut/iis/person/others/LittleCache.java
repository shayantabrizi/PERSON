/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.others;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author shayan
 */
public class LittleCache<A, B> {

    private final List<Search> list = new LinkedList<>();
    private final int maxSize;

    public LittleCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized void add(A key, B value) {
        if (list.size() == maxSize) {
            list.remove(0);
        }
        list.add(new Search(key, value));
    }

    public synchronized B get(A key) {
        for (Search s : list) {
            if (s.key.equals(key)) {
                return s.value;
            }
        }
        return null;
    }

    private class Search {

        A key;
        B value;

        Search(A key, B value) {
            this.key = key;
            this.value = value;
        }

    }

}
