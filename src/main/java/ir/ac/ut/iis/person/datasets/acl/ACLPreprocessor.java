/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.acl;

import ir.ac.ut.iis.retrieval_tools.crawler.IterableToFiles;
import ir.ac.ut.iis.retrieval_tools.crawler.IterateOnFiles;
import ir.ac.ut.iis.retrieval_tools.crawler.StringParser;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author shayan
 */
public class ACLPreprocessor implements IterableToFiles<String> {

    int count = 0;

    public static void main(String[] args) {
        set.add("A83-1001.txt");
        ACLPreprocessor dp = new ACLPreprocessor();
        IterateOnFiles iof = new IterateOnFiles(dp, new StringParser());
        iof.iterate(new File("/home/shayan/Downloads/Datasets/aan/papers_text"));
    }

    String abstractRegExp = "(Abstract)|(Abst rac t)|(Abst ract)|(ABSTRACT)|(A BSTRA CT)|(Abst rac l)";
    String introductionRegExp = "(Introduction)|(1 Introduct ion)|(1 In t roduct ion)|(1 I n t roduct ion)|(1 The  Prob lem)|(1 Overv iew)|(1 Motivation)|(0 I n t roduct ion)|(1 Mot ivat ion)|(1 Int roduct ion)|(1 Background)|(I INTRODUCTION)|(1. Introduction)|(Prologue)|(1. INTRODUCTION)|(I n t roduct ion)|(1 h l t roduct ion)";
    String specificIntroductionRegExp = "(1 Des iderata  fo r)|(1 Full Morpho log ica l  Tagg ing)|(1 The Encyclopddie)|(I. ASK AS A DATABASE SYSTEM)|(1. In t roductory  Renmrks)";
    String specificRegExp = "((1 Introduction).*(2 Multi Usage Proof Nets))|((A. Overview).*(B. I s sues  of Transpor tab i l i ty))";
    Pattern pattern = Pattern.compile("(?s)((" + abstractRegExp + ")(.*)(" + introductionRegExp + "|" + specificIntroductionRegExp + "))"
            + "|" + specificRegExp);

    static Set<String> set = new HashSet<>();

    @Override
    public boolean doAction(File fileEntry, String d) {
        int substring = Integer.parseInt(fileEntry.getName().substring(1, 3));
        if (substring > 16 || substring < 5) {
            return true;
        }
        count++;
        Matcher matcher = pattern.matcher(d);
        if (!matcher.find()) {
            if (set.contains(fileEntry.getName())) {
                return true;
            }
            System.out.println(count);
            System.out.println(fileEntry.getName());
            System.out.println(d.substring(0, Math.min(d.length(), 7_000)));
            System.out.println("----------------------------------------------------");
        }
        return true;
    }
}
