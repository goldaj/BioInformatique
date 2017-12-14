package model;

import java.util.LinkedHashMap;

public abstract class NucleotidesHolder {
    private String type;
    private String path;
    private double totalProbaTrinu1;
    private double totalProbaTrinu2;
    private double totalProbaDinu0;
    private double totalProbaDinu1;
    private double totalProbaTrinu0;
    private int totalPrefTrinu0;
    private int totalPrefTrinu1;
    private int totalPrefTrinu2;
    private long totalTrinucleotide;
    private long totalDinucleotide;
    private long totalCds;
    private int totalUnprocessedCds;

    // Trinucleotide
    private LinkedHashMap<String, Integer> trinuStatPhase0;
    private LinkedHashMap<String, Double> trinuProbaPhase0;
    private LinkedHashMap<String, Integer> trinuPrefPhase0;

    private LinkedHashMap<String, Integer> trinuStatPhase1;
    private LinkedHashMap<String, Double> trinuProbaPhase1;
    private LinkedHashMap<String, Integer> trinuPrefPhase1;

    private LinkedHashMap<String, Integer> trinuStatPhase2;
    private LinkedHashMap<String, Double> trinuProbaPhase2;
    private LinkedHashMap<String, Integer> trinuPrefPhase2;

    // Dinucleotide
    private LinkedHashMap<String, Integer> dinuStatPhase0;
    private LinkedHashMap<String, Double> dinuProbaPhase0;

    private LinkedHashMap<String, Integer> dinuStatPhase1;
    private LinkedHashMap<String, Double> dinuProbaPhase1;

    NucleotidesHolder(String type, String path, int totalDinucleotide, int totalTrinucleotide)
    {
        this.type = type;
        this.path = path;
        this.totalTrinucleotide = totalDinucleotide;
        this.totalDinucleotide = totalTrinucleotide;
        this.totalProbaTrinu0 = 0.0;
        this.totalProbaTrinu1 = 0.0;
        this.totalProbaTrinu2 = 0.0;
        this.totalPrefTrinu0 = 0;
        this.totalPrefTrinu1 = 0;
        this.totalPrefTrinu2 = 0;
        this.totalProbaDinu0 = 0.0;
        this.totalProbaDinu1 = 0.0;
        this.totalCds = 0;
        this.totalUnprocessedCds = 0;
    }

    public long getTotalTrinucleotide() {
        return totalTrinucleotide;
    }

    public void setTotalTrinucleotide(long totalTrinucleotide) {
        this.totalTrinucleotide = totalTrinucleotide;
    }
    
    public int getTotalPrefTrinu0() {
    	return totalPrefTrinu0;
    }
    
    public void setTotalPrefTrinu0(int totalPrefTrinu0) {
    	this.totalPrefTrinu0 = totalPrefTrinu0;
    }
    
    public int getTotalPrefTrinu1() {
    	return totalPrefTrinu1;
    }
    
    public void setTotalPrefTrinu1(int totalPrefTrinu1) {
    	this.totalPrefTrinu1 = totalPrefTrinu1;
    }

    
    public int getTotalPrefTrinu2() {
    	return totalPrefTrinu2;
    }
    
    public void setTotalPrefTrinu2(int totalPrefTrinu2) {
    	this.totalPrefTrinu2 = totalPrefTrinu2;
    }


    public long getTotalDinucleotide() {
        return totalDinucleotide;
    }

    public void setTotalDinucleotide(long totalDinucleotide) {
        this.totalDinucleotide = totalDinucleotide;
    }

    public LinkedHashMap<String, Integer> getTrinuStatPhase0() {
        return trinuStatPhase0;
    }

    public void setTrinuStatPhase0(LinkedHashMap<String, Integer> trinuStatPhase0) {
        this.trinuStatPhase0 = trinuStatPhase0;
    }

    public LinkedHashMap<String, Double> getTrinuProbaPhase0() {
        return trinuProbaPhase0;
    }

    public void setTrinuProbaPhase0(LinkedHashMap<String, Double> trinuProbaPhase0) {
        this.trinuProbaPhase0 = trinuProbaPhase0;
    }
    
    public LinkedHashMap<String, Integer> getTrinuPrefPhase0() {
    	return trinuPrefPhase0;
    }

    public void setTrinuPrefPhase0(LinkedHashMap<String, Integer> trinuPrefPhase0) {
        this.trinuPrefPhase0 = trinuPrefPhase0;
    }

    public LinkedHashMap<String, Integer> getTrinuStatPhase1() {
        return trinuStatPhase1;
    }

    public void setTrinuStatPhase1(LinkedHashMap<String, Integer> trinuStatPhase1) {
        this.trinuStatPhase1 = trinuStatPhase1;
    }

    public LinkedHashMap<String, Double> getTrinuProbaPhase1() {
        return trinuProbaPhase1;
    }

    public void setTrinuProbaPhase1(LinkedHashMap<String, Double> trinuProbaPhase1) {
        this.trinuProbaPhase1 = trinuProbaPhase1;
    }
    
    public LinkedHashMap<String, Integer> getTrinuPrefPhase1() {
    	return trinuPrefPhase1;
    }

    public void setTrinuPrefPhase1(LinkedHashMap<String, Integer> trinuPrefPhase1) {
        this.trinuPrefPhase1 = trinuPrefPhase1;
    }

    public LinkedHashMap<String, Integer> getTrinuStatPhase2() {
        return trinuStatPhase2;
    }

    public void setTrinuStatPhase2(LinkedHashMap<String, Integer> trinuStatPhase2) {
        this.trinuStatPhase2 = trinuStatPhase2;
    }

    public LinkedHashMap<String, Double> getTrinuProbaPhase2() {
        return trinuProbaPhase2;
    }

    public void setTrinuProbaPhase2(LinkedHashMap<String, Double> trinuProbaPhase2) {
        this.trinuProbaPhase2 = trinuProbaPhase2;
    }
    
    public LinkedHashMap<String, Integer> getTrinuPrefPhase2() {
    	return trinuPrefPhase2;
    }

    public void setTrinuPrefPhase2(LinkedHashMap<String, Integer> trinuPrefPhase2) {
        this.trinuPrefPhase2 = trinuPrefPhase2;
    }

    public LinkedHashMap<String, Integer> getDinuStatPhase0() {
        return dinuStatPhase0;
    }

    public void setDinuStatPhase0(LinkedHashMap<String, Integer> dinuStatPhase0) {
        this.dinuStatPhase0 = dinuStatPhase0;
    }

    public LinkedHashMap<String, Double> getDinuProbaPhase0() {
        return dinuProbaPhase0;
    }

    public void setDinuProbaPhase0(LinkedHashMap<String, Double> dinuProbaPhase0) {
        this.dinuProbaPhase0 = dinuProbaPhase0;
    }

    public LinkedHashMap<String, Integer> getDinuStatPhase1() {
        return dinuStatPhase1;
    }

    public void setDinuStatPhase1(LinkedHashMap<String, Integer> dinuStatPhase1) {
        this.dinuStatPhase1 = dinuStatPhase1;
    }

    public LinkedHashMap<String, Double> getDinuProbaPhase1() {
        return dinuProbaPhase1;
    }

    public void setDinuProbaPhase1(LinkedHashMap<String, Double> dinuProbaPhase1) {
        this.dinuProbaPhase1 = dinuProbaPhase1;
    }

    public long getTotalCds() {
        return totalCds;
    }

    public void setTotalCds(long totalCds) {
        this.totalCds = totalCds;
    }

    public int getTotalUnprocessedCds() {
        return totalUnprocessedCds;
    }

    public void setTotalUnprocessedCds(int totalUnprocessedCds) {
        this.totalUnprocessedCds = totalUnprocessedCds;
    }

    public double getTotalProbaTrinu1() {
        return totalProbaTrinu1;
    }

    public double getTotalProbaTrinu2() {
        return totalProbaTrinu2;
    }

    public double getTotalProbaDinu0() {
        return totalProbaDinu0;
    }

    public double getTotalProbaDinu1() {
        return totalProbaDinu1;
    }

    public double getTotalProbaTrinu0() {
        return totalProbaTrinu0;
    }

    public void setTotalProbaTrinu1(double totalProbaTrinu1) {
        this.totalProbaTrinu1 = totalProbaTrinu1;
    }

    public void setTotalProbaTrinu2(double totalProbaTrinu2) {
        this.totalProbaTrinu2 = totalProbaTrinu2;
    }

    public void setTotalProbaDinu0(double totalProbaDinu0) {
        this.totalProbaDinu0 = totalProbaDinu0;
    }

    public void setTotalProbaDinu1(double totalProbaDinu1) {
        this.totalProbaDinu1 = totalProbaDinu1;
    }

    public void setTotalProbaTrinu0(double totalProbaTrinu0) {
        this.totalProbaTrinu0 = totalProbaTrinu0;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

}
