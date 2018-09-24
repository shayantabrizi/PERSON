/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

/**
 *
 * @author shayan
 * @param <T>
 */
public abstract class ConnectedComponentsExtractor<T> {

    public Triple<Map<T, Integer>, Map<Integer, Integer>, Integer> getConnectedComponents() {
        Map<T, Integer> visited = new HashMap<>();
        int compId = 0;
        for (T id : getNodes()) {
            if (!visited.containsKey(id)) {
                bfs(id, compId, visited);
                compId++;
            }
        }

        Map<Integer, Integer> counts = new HashMap<>();
        int max = 0;
        int maxId = 0;
        for (Map.Entry<T, Integer> e : visited.entrySet()) {
            Integer get = counts.get(e.getValue());
            if (get == null) {
                get = 0;
            }
            get++;
            if (get > max) {
                max = get;
                maxId = e.getValue();
            }
            counts.put(e.getValue(), get);
        }

        return new MutableTriple<>(visited, counts, maxId);
    }

    protected abstract Set<T> getNodes();

    private void bfs(T src, int compId, Map<T, Integer> visited) {
        LinkedList<T> list = new LinkedList<>();
        list.add(src);
        visited.put(src, compId);
        while (!list.isEmpty()) {
            T currentnode = list.removeFirst();
            for (T n : getNeighbors(currentnode)) {
                if (!visited.containsKey(n)) {
                    visited.put(n, compId);
                    list.add(n);
                }
            }
        }
    }

    protected abstract Set<T> getNeighbors(T currentNode);

}
