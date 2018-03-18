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
public class User implements Comparable<User> {

    int id;
    private float[] topics;
    private float[] tmpArray;

    public User(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTmpArray(float[] tmpArray) {
        this.tmpArray = tmpArray;
    }

    public float[] getTmpArray() {
        return tmpArray;
    }

    public float[] getTopics() {
        return topics;
    }

    public void setTopics(float[] topics) {
        this.topics = topics;
    }

    @Override
    public int compareTo(User o) {
        return Integer.compare(id, o.id);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.id);
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
        final User other = (User) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

}
