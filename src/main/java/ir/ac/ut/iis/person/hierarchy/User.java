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
public class User implements Comparable<IUser>, IUser {

    int id;
    private float[] topics;
//    private float[] tmpArray;

    public User(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

//    @Override
//    public void setTmpArray(float[] tmpArray) {
//        this.tmpArray = tmpArray;
//    }
//
//    @Override
//    public float[] getTmpArray() {
//        return tmpArray;
//    }
    @Override
    public float[] getTopics() {
        return topics;
    }

    @Override
    public void setTopics(float[] topics) {
        this.topics = topics;
    }

    @Override
    public int compareTo(IUser o) {
        return Integer.compare(id, o.getId());
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
        return Objects.equals(this.id, other.id);
    }

}
