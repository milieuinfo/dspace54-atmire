package com.atmire.sword.rules.factory;


import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Builder that will instantiate a ValueRule rule based on a rule definition.
 */
public class ValueRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        FieldHasValueRule rule = new FieldHasValueRule(ruleDefinition.getFieldDescription(), ruleDefinition.getField(),
                ruleDefinition.getFieldValue());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }

}
