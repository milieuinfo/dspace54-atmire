package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;

/**
 * @author philip at atmire.com
 *
 * Builder that will instantiate a UniqueValueByAuthorityId rule based on a rule definition.
 */
public class UniqueValueByAuthorityIdRuleBuilder extends ComplianceRuleBuilder {
    @Override
    public ComplianceRule buildRule(RuleDefinition ruleDefinition) {
        UniqueValueByAuthorityId rule = new UniqueValueByAuthorityId(ruleDefinition.getFieldDescription(), ruleDefinition.getField());
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }
}
