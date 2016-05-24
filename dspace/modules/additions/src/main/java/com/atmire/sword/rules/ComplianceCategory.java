package com.atmire.sword.rules;

import com.atmire.sword.result.*;
import java.util.Collection;
import java.util.*;
import org.dspace.content.*;
import org.dspace.core.*;

public class ComplianceCategory implements Comparable<ComplianceCategory> {

    private int ordinal;

    private String name;

    private String description;

    private String resolutionHint;

    private final List<ComplianceRule> complianceRules = new LinkedList<ComplianceRule>();

    private final List<ComplianceRule> exceptionRules = new LinkedList<ComplianceRule>();

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(final int ordinal) {
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getResolutionHint() {
        return resolutionHint;
    }

    public void setResolutionHint(final String resolutionHint) {
        this.resolutionHint = resolutionHint;
    }

    public List<ComplianceRule> getComplianceRules() {
        return complianceRules;
    }

    public List<ComplianceRule> getExceptionRules() {
        return exceptionRules;
    }

    public void addComplianceRule(final ComplianceRule rule) {
        complianceRules.add(rule);
    }

    public void addComplianceRules(final Collection<ComplianceRule> rules) {
        complianceRules.addAll(rules);
    }

    public void addExceptionRule(final ComplianceRule rule) {
        exceptionRules.add(rule);
    }

    public void addExceptionRules(final Collection<ComplianceRule> rules) {
        exceptionRules.addAll(rules);
    }

    public CategoryComplianceResult validate(final Context context, final Item item) {
        CategoryComplianceResult result = buildCategoryComplianceResult();

        for (ComplianceRule rule : complianceRules) {
            RuleComplianceResult ruleResult = rule.validate(context, item);
            result.addRuleResult(ruleResult);
        }

        for (ComplianceRule rule : exceptionRules) {
            RuleComplianceResult ruleResult = rule.validate(context, item);
            result.addExceptionResult(ruleResult);
        }

        return result;
    }

    private CategoryComplianceResult buildCategoryComplianceResult() {
        CategoryComplianceResult result = new CategoryComplianceResult();
        result.setOrdinal(ordinal);
        result.setCategoryName(name);
        result.setCategoryDescription(description);
        result.setResolutionHint(resolutionHint);
        return result;
    }

    public int compareTo(final ComplianceCategory o) {
        return getOrdinal() < o.getOrdinal() ? -1 : (getOrdinal() == o.getOrdinal() ? 0 : 1);
    }
}
