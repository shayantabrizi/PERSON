/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

/**
 *
 * @author shayan
 */
public interface IStatistic {

    void add(double n);

    long getCnt();

    void initialize();

    @Override
     boolean equals(Object obj);

    @Override
    public String toString();

     String toString(boolean verbose);
}
