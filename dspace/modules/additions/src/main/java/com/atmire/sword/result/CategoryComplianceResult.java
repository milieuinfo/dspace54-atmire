package com.atmire.sword.result;

import java.util.*;

/**
 * TODO TOM UNIT TEST
 */
public class CategoryComplianceResult implements Comparable<CategoryComplianceResult>{

    private int ordinal;

    private String categoryName = null;

    private String categoryDescription = null;

    private String resolutionHint = null;

    private List<RuleComplianceResult> exceptionResults = new LinkedList<RuleComplianceResult>();

    private List<RuleComplianceResult> ruleResults = new LinkedList<RuleComplianceResult>();

    private boolean applicable = true;


    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(final int ordinal) {
        this.ordinal = ordinal;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(final String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public List<RuleComplianceResult> getCompliantRules() {
        List<RuleComplianceResult> output = new LinkedList<RuleComplianceResult>();
        for (RuleComplianceResult ruleResult : ruleResults) {

            if(ruleResult.isCompliant()) {
                output.add(ruleResult);
            }
        }

        return output;
    }

    public List<RuleComplianceResult> getViolatedRules() {
        List<RuleComplianceResult> output = new LinkedList<RuleComplianceResult>();
        for (RuleComplianceResult ruleResult : ruleResults) {

            if(!ruleResult.isCompliant()) {
                output.add(ruleResult);
            }
        }

        return output;
    }

    public List<RuleComplianceResult> getAppliedExceptions() {
        List<RuleComplianceResult> output = new LinkedList<RuleComplianceResult>();
        for (RuleComplianceResult ruleResult : exceptionResults) {

            if(ruleResult.isCompliant() && ruleResult.isApplicable()) {
                output.add(ruleResult);
            }
        }

        return output;
    }

    public boolean isCompliant() {
        boolean isCompliant = true;

        if(!isCompliantByException()) {

            Iterator<RuleComplianceResult> it = ruleResults.iterator();
            while (it.hasNext() && isCompliant) {
                RuleComplianceResult next = it.next();
                isCompliant = next.isCompliant();
            }

        }

        return isCompliant;
    }

    public boolean isCompliantByException() {
        return !getAppliedExceptions().isEmpty();
    }

    public void addRuleResult(final RuleComplianceResult ruleResult) {
        ruleResults.add(ruleResult);
    }

    public void addExceptionResult(final RuleComplianceResult ruleResult) {
        exceptionResults.add(ruleResult);
    }

    public void setResolutionHint(final String resolutionHint) {
        this.resolutionHint = resolutionHint;
    }

    public String getResolutionHint() {
        return resolutionHint;
    }

    public int compareTo(final CategoryComplianceResult o) {
        return getOrdinal() < o.getOrdinal() ? -1 : (getOrdinal() == o.getOrdinal() ? 0 : 1);
    }

    public void setApplicable(final boolean applicable) {
        this.applicable = applicable;
    }

    public boolean isApplicable() {
        return applicable;
    }
}
