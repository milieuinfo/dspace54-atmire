package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Builder that will instantiate a NotBlank rule based on a rule definition.
 */
public class FailOnPreconditionSuccessRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        FailOnPreconditionSuccessRule rule = new FailOnPreconditionSuccessRule(ruleDefinition.getFieldDescription(), ruleDefinition.getField());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }
}
