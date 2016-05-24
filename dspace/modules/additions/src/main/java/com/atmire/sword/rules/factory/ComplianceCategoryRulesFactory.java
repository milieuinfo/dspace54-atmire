package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.rules.exception.*;

/**
 * Interface for a factory that is able to instantiate all required compliance validation categories and their rules based on the
 * Validation Rule definition file (config/item-validation-rules.xml)
 */
public interface ComplianceCategoryRulesFactory {

    CompliancePolicy createComplianceRulePolicy() throws ValidationRuleDefinitionException;

}