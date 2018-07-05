package com.atmire.sword.rules.factory;


import com.atmire.sword.rules.ComplianceRule;
import com.atmire.sword.rules.PastOrCurrentDateRule;
import com.atmire.sword.validation.model.RuleDefinition;

/**
 * Builder that will instantiate a DateSmallerThan rule based on a rule definition.
 */
public class PastOrCurrentDateRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        PastOrCurrentDateRule rule = new PastOrCurrentDateRule(
                ruleDefinition.getFieldDescription(),
                ruleDefinition.getField()
        );
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }
}
