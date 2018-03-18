/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import java.util.Objects;

/**
 *
 * @author shayan
 */
public class BasicAuthorFactory implements PapersPreprocessor.AuthorFactory {

    @Override
    public PapersPreprocessor.AuthorRepresentation create(String s) {
        return new BasicAuthor(s);
    }

    static class BasicAuthor implements PapersPreprocessor.AuthorRepresentation {

        String name;

        BasicAuthor(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.name);
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
            final BasicAuthor other = (BasicAuthor) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }

    }
}
