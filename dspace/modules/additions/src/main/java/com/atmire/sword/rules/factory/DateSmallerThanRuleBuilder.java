package com.atmire.sword.rules.factory;


import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Builder that will instantiate a DateSmallerThan rule based on a rule definition.
 */
public class DateSmallerThanRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        DateSmallerThanRule rule = new DateSmallerThanRule(ruleDefinition.getFieldDescription(), ruleDefinition.getField(),
                ruleDefinition.getFieldValue());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }
}
