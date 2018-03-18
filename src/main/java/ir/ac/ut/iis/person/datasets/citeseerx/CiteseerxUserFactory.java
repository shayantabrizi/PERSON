/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.citeseerx;

import ir.ac.ut.iis.person.hierarchy.User;
import ir.ac.ut.iis.person.hierarchy.UserFactory;
    
/**
 *
 * @author shayan
 */
public class CiteseerxUserFactory implements UserFactory {

    @Override
    public User createUser(int id) {
        return new User(id);
    }

}
