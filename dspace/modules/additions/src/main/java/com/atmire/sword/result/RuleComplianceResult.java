package com.atmire.sword.result;

import java.util.*;
import org.apache.commons.collections.*;
import org.apache.commons.lang3.*;

/**
 * Model class that represents the compliance result of a single rule.
 * This class also contains logic to build a meaningful description explaining the result.
 */
public class RuleComplianceResult {

    private boolean compliant;

    private String ruleDescriptionViolation;

    private String ruleDescriptionCompliant;

    private String preconditionDescription;

    private String exceptionDescription;

    private String exceptionHint;

    private String definitionHint;

    private String resolutionHint;

    private List<String> violationDescriptions = new LinkedList<String>();

    private boolean applicable = true;

    public boolean isCompliant() {
        return compliant;
    }

    public void setCompliant(final boolean compliant) {
        this.compliant = compliant;
    }

    public void setRuleDescriptionViolation(final String ruleDescriptionViolation) {
        this.ruleDescriptionViolation = preformat(ruleDescriptionViolation);
    }

    public void setRuleDescriptionCompliant(final String ruleDescriptionCompliant) {
        this.ruleDescriptionCompliant = preformat(ruleDescriptionCompliant);
    }

    public void setPreconditionDescription(final String preconditionDescription) {
        this.preconditionDescription = preformat(preconditionDescription);
    }

    public void setExceptionDescription(final String exceptionDescription) {
        this.exceptionDescription = preformat(exceptionDescription);
    }

    public void setExceptionHint(final String exceptionHint) {
        this.exceptionHint = preformat(exceptionHint);
    }

    public void setDefinitionHint(final String definitionHint) {
        this.definitionHint = preformat(definitionHint);
    }

    public String getDefinitionHint() {
        return format(definitionHint);
    }

    public void setResolutionHint(final String resolutionHint) {
        this.resolutionHint = preformat(resolutionHint);
    }

    public String getResolutionHint() {
        return format(resolutionHint);
    }

    public String getResultDescription() {
        StringBuilder description = new StringBuilder();
        if(StringUtils.isNotBlank(preconditionDescription)) {
            description.append("als stelling ");
            description.append(preconditionDescription);
            description.append(" waar is, dan geldt dat ");
        }

        if(isCompliant() && !exceptionApplied() && isApplicable()) {
            description.append(ruleDescriptionCompliant);
        } else {
            description.append(ruleDescriptionViolation);
        }

        return format(description.toString());
    }

    public String getRuleDescriptionCompliant() {
        return ruleDescriptionCompliant;
    }

    public void addViolationDescriptions(final List<String> descriptions) {
        if(CollectionUtils.isNotEmpty(descriptions)) {
            for (String description : descriptions) {
                violationDescriptions.add(format(preformat(description)));
            }
        }
    }

    public List<String> getViolationDescriptions() {
        return violationDescriptions;
    }

    public String getExceptionDescription() {
        return format(exceptionDescription);
    }

    public String getExceptionHint() {
        return format(exceptionHint);
    }

    public boolean exceptionApplied() {
        return StringUtils.isNotBlank(exceptionDescription);
    }

    private String preformat(final String description) {
        String result = StringUtils.uncapitalize(description);
        if(StringUtils.endsWith(result, ".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String format(final String str) {
        return str ==  null ? null : StringUtils.capitalize(str) + ".";
    }

    public void setApplicable(final boolean applicable) {
        this.applicable = applicable;
    }

    public boolean isApplicable() {
        return applicable;
    }
}
