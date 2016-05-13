package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * Interface for a builder class that is able to instantiate compliance vaidation rules
 */
public abstract class ComplianceRuleBuilder {

    public abstract ComplianceRule buildRule(final RuleDefinition ruleDefinition);

    protected void applyDefinitionDescriptionAndResolutionHint(final AbstractComplianceRule rule, final RuleDefinition ruleDefinition) {
        rule.setDefinitionHint(ruleDefinition.getDescription());
        rule.setResolutionHint(ruleDefinition.getResolutionHint());
    }
}
