/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import java.util.Objects;

/**
 *
 * @author shayan
 */
public class UniformPPR extends PPRCalculator {

    final Iterable<GraphNode> topic;
    final int topicSize;

    public UniformPPR(int topicNodeId, Iterable<GraphNode> topic, int topicSize, double pageRankAlpha) {
        super(topicNodeId, pageRankAlpha);
        this.topic = topic;
        this.topicSize = topicSize;
    }

    @Override
    public void updatePPRs(float[] zeroDegrees, double alpha) {
        for (GraphNode u : topic) {
            for (int i = 0; i < zeroDegrees.length; i++) {
                u.getTmpArray()[i] = (float) (u.getTmpArray()[i] + (alpha + zeroDegrees[i] * (1 - alpha)) / topicSize);
            }
        }
    }

    @Override
    public String toString() {
        return String.valueOf(topicNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicNodeId, alpha);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UniformPPR other = (UniformPPR) obj;
        if (topicNodeId != other.topicNodeId) {
            return false;
        }
        if (alpha != other.alpha) {
            return false;
        }

        return true;
    }

}
