package com.atmire.sword.result;

import java.util.*;

/**
 * Model class that represents the result of a compliance check
 */
public class ComplianceResult {

    private List<CategoryComplianceResult> categoryResults = new LinkedList<CategoryComplianceResult>();

    private List<RuleComplianceResult> exceptionResults = new LinkedList<RuleComplianceResult>();

    public List<CategoryComplianceResult> getOrderedCategoryResults() {
        Collections.sort(categoryResults);

        return categoryResults;
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

            Iterator<CategoryComplianceResult> it = getOrderedCategoryResults().iterator();

            while (it.hasNext() && isCompliant) {
                CategoryComplianceResult next = it.next();
                isCompliant = next.isCompliant() || !next.isApplicable();
            }
        }

        return isCompliant;
    }

    public boolean isCompliantByException() {
        return !getAppliedExceptions().isEmpty();
    }

    public void addExceptionResult(final RuleComplianceResult ruleResult) {
        exceptionResults.add(ruleResult);
    }

    public void addCategoryResult(final CategoryComplianceResult categoryResult) {
        categoryResults.add(categoryResult);
    }
}
