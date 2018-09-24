/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.topics;

/**
 *
 * @author shayan
 */
public class CachedInstanceClassifier {

    private String lastString = null;
    private float[] lastTopics;
    private Integer topClass;
    private final InstanceClassifier ic;

    public CachedInstanceClassifier(InstanceClassifier ic) {
        this.ic = ic;
    }

    public float[] getQueryTopics(String query) {
        if (lastString != null && lastString.equals(query)) {
            return lastTopics;
        } else {
            lastTopics = ic.getQueryTopics(query);
            lastString = query;
            topClass = InstanceClassifier.getTopClasses(lastTopics, 1).iterator().next();
            return lastTopics;
        }
    }

    public Integer getTopClass(String query) {
        if (lastString == null || !lastString.equals(query)) {
            getQueryTopics(query);
        }
        return topClass;
    }
    
    public void setPriors(float[] priors) {
        ic.setPriors(priors);
//        lastString = null;
    }

}
