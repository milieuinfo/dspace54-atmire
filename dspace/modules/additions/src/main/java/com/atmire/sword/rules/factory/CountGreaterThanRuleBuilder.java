package com.atmire.sword.rules.factory;


import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Builder that will instantiate a CountGreaterThan rule based on a rule definition.
 */
public class CountGreaterThanRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        CountLesserThanRule rule = new CountLesserThanRule(ruleDefinition.getFieldDescription(), ruleDefinition.getField(), ruleDefinition.getFieldValue());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }

}
