package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Builder that will instantiate a DateRangeSmallerThan rule based on a rule definition.
 */
public class DateRangeSmallerThanRuleBuilder extends ComplianceRuleBuilder {

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        DateRangeSmallerThanRule rule = new DateRangeSmallerThanRule(ruleDefinition.getFrom(), ruleDefinition.getTo(),
                ruleDefinition.getFieldDescription(), ruleDefinition.getFieldValue());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }

}
