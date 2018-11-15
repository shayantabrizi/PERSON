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
public interface IUser extends Comparable<IUser> {

    public int getId();

    @Override
    public int compareTo(IUser o);

    @Override
    public int hashCode();

    @Override
    public boolean equals(Object obj);

//    public float[] getTmpArray();
//
//    public void setTmpArray(float[] tmpArray);

    public float[] getTopics();

    public void setTopics(float[] topics);

}
