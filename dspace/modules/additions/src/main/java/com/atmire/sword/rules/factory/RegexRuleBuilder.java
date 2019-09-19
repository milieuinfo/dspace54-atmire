package com.atmire.sword.rules.factory;


import com.atmire.sword.rules.ComplianceRule;
import com.atmire.sword.rules.RegexRule;
import com.atmire.sword.validation.model.RuleDefinition;

/**
 * Builder that will instantiate a CountGreaterThan rule based on a rule definition.
 */
public class RegexRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        RegexRule rule = new RegexRule(
                ruleDefinition.getFieldDescription(),
                ruleDefinition.getField(),
                ruleDefinition.getFieldValue()
        );
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }

}
