/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.paper;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author shayan
 */
public class DisambiguatedAuthorFactory implements PapersPreprocessor.AuthorFactory {

    @Override
    public PapersPreprocessor.AuthorRepresentation create(String s) {
        String[] split = clean(s);
        return new DisambiguatedAuthor(split[0].charAt(0), split[split.length - 1]);
    }

    public static String[] clean(String s) {
        switch (s) {
            case "R. D. Ferrar0":
                s = "R. D. Ferraro";
                break;
            case "Robert Wilensky0":
                s = "Robert Wilensky";
                break;
            case "Toshiyuki KIT0":
                s = "Toshiyuki KITO";
                break;
            case "Hideo IT0":
                s = "Hideo ITO";
                break;
            case "\"lk\" G\"rler":
                s = "Ülkü Gürler";
                break;
            case "eljko Vrba":
                s = "Željko Vrba";
                break;
            case "tepán Oana":
                s = "Štepán Ožana";
                break;
        }
        String[] split = StringUtils.stripAccents(s.trim()).replace('Ø', 'O').replace('ø', 'o').replaceAll("0.", "O.").replaceAll("^\\*|\\+|\\.|\"|2", "").trim().toLowerCase().split(" ");
        char initial = split[0].charAt(0);
        if (split.length < 2 || !(initial >= 'a' && initial <= 'z') || split[split.length - 1].length() < 2) {
            return null;
        }

        return split;
    }

    static class DisambiguatedAuthor implements PapersPreprocessor.AuthorRepresentation {

        char initial;
        String lastName;

        DisambiguatedAuthor(char initial, String lastName) {
            this.initial = initial;
            this.lastName = lastName;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + this.initial;
            hash = 53 * hash + Objects.hashCode(this.lastName);
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
            final DisambiguatedAuthor other = (DisambiguatedAuthor) obj;
            if (this.initial != other.initial) {
                return false;
            }
            if (!Objects.equals(this.lastName, other.lastName)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return Character.toUpperCase(initial) + ". " + StringUtils.capitalize(lastName);
        }

    }
}
