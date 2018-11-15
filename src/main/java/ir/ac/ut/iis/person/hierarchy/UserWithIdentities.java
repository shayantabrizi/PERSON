/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;

/**
 *
 * @author shayan
 */
public class UserWithIdentities implements IUser {

    private final IUser user;
    private final Int2ObjectOpenHashMap<Identity> identities = new Int2ObjectOpenHashMap<>(1, .75f);

    public UserWithIdentities(IUser user) {
        this.user = user;
    }

    public Identity getIdentity(int id) {
        return identities.get(id);
    }

    public void addIdentity(int id, Identity identity) {
        identities.put(id, identity);
    }

    @Override
    public int getId() {
        return user.getId();
    }

    @Override
    public int compareTo(IUser o) {
        return user.compareTo(o);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return user.equals(o);
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

    public Map<Integer, Identity> getIdentities() {
        return identities;
    }

//    @Override
//    public float[] getTmpArray() {
//        return user.getTmpArray();
//    }
//
//    @Override
//    public void setTmpArray(float[] tmpArray) {
//        user.setTmpArray(tmpArray);
//    }

    @Override
    public float[] getTopics() {
        return user.getTopics();
    }

    @Override
    public void setTopics(float[] topics) {
        user.setTopics(topics);
    }
}
