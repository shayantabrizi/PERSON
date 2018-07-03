/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

/**
 *
 * @author GOMROK IRAN
 */
public class IgnoreQueryEx extends RuntimeException{

    /**
	 * 
	 */
	private static final long serialVersionUID = -4954112613700014553L;

	public IgnoreQueryEx() {
    }

    public IgnoreQueryEx(String message) {
        super(message);
    }
    
}
