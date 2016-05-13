package com.atmire.sword.service;

import com.atmire.sword.result.*;
import com.atmire.sword.rules.*;
import com.atmire.sword.rules.exception.*;
import com.atmire.sword.rules.factory.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.springframework.beans.factory.annotation.*;

/**
 * Implementation of {@link ComplianceCheckService}
 */
public class ComplianceCheckServiceBean implements ComplianceCheckService {

    private static Logger log = Logger.getLogger(ComplianceCheckServiceBean.class);

    @Autowired
    private ComplianceCategoryRulesFactory rulesFactory;

    public ComplianceResult checkCompliance(final Context context, final Item item) {
        CompliancePolicy policy = getCompliancePolicy();
        if(policy == null) {
            return new ComplianceResult();
        } else {
            ComplianceResult complianceResult = policy.validate(context, item);

            return complianceResult;
        }
    }

    private CompliancePolicy getCompliancePolicy() {
        try {
            return rulesFactory.createComplianceRulePolicy();
        } catch (ValidationRuleDefinitionException e) {
            log.warn("Unable to load the validation rules: " + e.getMessage(), e);
        }
        return null;
    }
}
