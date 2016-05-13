package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Builder that will instantiate a NotBlank rule based on a rule definition.
 */
public class NotBlankRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        FieldIsNotBlankRule rule = new FieldIsNotBlankRule(ruleDefinition.getFieldDescription(), ruleDefinition.getField());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }
}
