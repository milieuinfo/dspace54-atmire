package com.atmire.sword.rules;


import com.atmire.sword.result.*;
import java.util.Collection;
import java.util.*;
import org.dspace.content.*;
import org.dspace.core.*;

public class CompliancePolicy {

    private final List<ComplianceCategory> categories = new LinkedList<ComplianceCategory>();

    private final List<ComplianceRule> exceptionRules = new LinkedList<ComplianceRule>();

    public void addComplianceCategory(final ComplianceCategory category) {
        categories.add(category);
    }

    public void addExceptionRules(final Collection<ComplianceRule> rules) {
        exceptionRules.addAll(rules);
    }

    public ComplianceResult validate(final Context context, final Item item) {

        Collections.sort(categories);

        boolean exceptionEncountered = false;
        ComplianceResult result = new ComplianceResult();

        //First check all the exceptions
        for (ComplianceRule exceptionRule : exceptionRules) {
            RuleComplianceResult ruleResult = exceptionRule.validate(context, item);
            result.addExceptionResult(ruleResult);
        }

        //If we found an exception, all categories do not matter anymore (they're not applicable)
        if(result.isCompliantByException()) {
            exceptionEncountered = true;
        }

        //Check the categories and indicate if they are applicable or not
        for (ComplianceCategory category : categories) {
            CategoryComplianceResult categoryResult = category.validate(context, item);

            if(exceptionEncountered) {
                categoryResult.setApplicable(false);
            } else {
                categoryResult.setApplicable(true);
            }

            //If this category has an exception, all next categories are not applicable any more
            if(categoryResult.isCompliantByException()) {
                exceptionEncountered = true;
            }

            result.addCategoryResult(categoryResult);
        }

        return result;
    }
}
