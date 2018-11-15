/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 *
 * @author shayan
 */
public class Identity implements IUser {

    private final int id;
    private IUser user;
    private int topic;
    private Float weight;
    private float[] tmpArray;

    public Identity(int id) {
        this.id = id;
    }

    public Identity(int id, IUser user, int topic, Float weight) {
        this.id = id;
        this.user = user;
        this.topic = topic;
        this.weight = weight;
    }

    public Float getWeight() {
        return weight;
    }

    public void setUser(IUser user) {
        this.user = user;
    }

    public void setTopic(int topic) {
        this.topic = topic;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public int getTopic() {
        return topic;
    }

    @Override
    public int compareTo(IUser o) {
        return Integer.compare(id, o.getId());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + this.id;
        return hash;
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
        final Identity other = (Identity) obj;
        return this.id == other.id;
    }

    public IUser getUser() {
        return user;
    }

    @Override
    public int getId() {
        return id;
    }

//    @Override
//    public float[] getTmpArray() {
//        return tmpArray;
//    }
//
//    @Override
//    public void setTmpArray(float[] tmpArray) {
//        this.tmpArray = tmpArray;
//    }

    @Override
    public float[] getTopics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTopics(float[] topics) {
        throw new UnsupportedOperationException();
    }

}
