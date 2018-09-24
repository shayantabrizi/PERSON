/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

/**
 *
 * @author shayan
 */
public interface MeasureCalculator {

    String toString();

    int hashCode();

    boolean equals(Object obj);

    float[] calc(int numOfWeights, GraphNode node, Iterable<GraphNode> parent, int parentSize, short level);
    
    int getSeedsId();

}
