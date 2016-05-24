package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.validation.model.*;
import org.dspace.discovery.*;
import org.springframework.beans.factory.annotation.*;

/**
 * Builder that will instantiate a DiscoverableRule rule based on a rule definition.
 */
public class DiscoverableRuleBuilder extends ComplianceRuleBuilder {

    @Autowired
    private SearchService searchService;

    public ComplianceRule buildRule(final RuleDefinition ruleDefinition) {
        DiscoverableRule rule = new DiscoverableRule(searchService);
        applyDefinitionDescriptionAndResolutionHint(rule, ruleDefinition);
        return rule;
    }
}
