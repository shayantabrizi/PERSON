/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.query.Query;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.queries.function.ValueSource;

/**
 *
 * @author shayan
 */
public abstract class MyValueSource extends ValueSource {

    private Map<String, Double> features = null;

    public void initialize(Query query) {
        if (features != null) {
            features.clear();
        }
    }

    public void startCollectingFeatures() {
        features = new HashMap<>();
    }

    protected void addFeature(String name, Double value) {
        if (features != null) {
            features.put(name, value);
        }
    }

    public Map<String, Double> getFeatures() {
        return Collections.unmodifiableMap(features);
    }
    
    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String description() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public abstract String getName();

}
