/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author shayan
 */
public class Statistic implements IStatistic {

    private final String name;
    private double sum = 0;
    private double sum2 = 0;
    private long cnt = 0;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    private double sum_all = 0;
    private double sum2_all = 0;
    private long cnt_all = 0;
    private double min_all = Double.POSITIVE_INFINITY;
    private double max_all = Double.NEGATIVE_INFINITY;

    public Statistic(String name) {
        this.name = name;
    }

    @Override
    public void add(double n) {
        sum += n;
        sum_all += n;
        sum2 += n * n;
        sum2_all += n * n;
        cnt++;
        cnt_all++;
        min = Math.min(min, n);
        max = Math.max(max, n);
        min_all = Math.min(min_all, n);
        max_all = Math.max(max_all, n);
    }

    @Override
    public long getCnt() {
        return cnt;
    }

    @Override
    public void initialize() {
        cnt = 0;
        sum = 0;
        sum2 = 0;
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
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
        final Statistic other = (Statistic) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public String toString(boolean verbose) {
        double avg = sum / cnt;
        double std = Math.sqrt(((double) sum2) / (cnt - 1) - (Math.pow(avg, 2) * cnt / (cnt - 1)));
        double avg_all = sum_all / cnt_all;
        double std_all = Math.sqrt(((double) sum2_all) / (cnt_all - 1) - (Math.pow(avg_all, 2) * cnt_all / (cnt_all - 1)));
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("{").append(avg).append("+-").append(std).append(",} (cov=").append(Math.abs(std / avg)).append(")(cnt=").append(cnt).append(")").append(verbose ? (" (min=" + min + ",max=" + max + ")") : "").append("\n");
        sb.append(name).append("{").append(avg_all).append("+-").append(std_all).append(",} (cov=").append(Math.abs(std_all / avg_all)).append(")(cnt=").append(cnt_all).append(")").append(verbose ? (" (min=" + min_all + ",max=" + max_all + ")") : "");
        return sb.toString();
    }

    public static class Statistics {

        private final List<IStatistic> list = new LinkedList<>();

        private boolean contains(IStatistic s) {
            for (IStatistic st : list) {
                if (st.equals(s)) {
                    return true;
                }
            }
            return false;
        }

        public void addStatistic(IStatistic s) {
            if (!contains(s)) {
                list.add(s);
            }
        }

        public void printStatistics(boolean verbose) {
            for (IStatistic s : list) {
                if (s.getCnt() > 0 || verbose) {
                    System.out.println(s.toString(verbose));
                }
            }
        }
    }

}
