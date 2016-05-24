package com.atmire.sword.rules.factory;

import com.atmire.sword.rules.*;
import com.atmire.sword.rules.exception.*;
import com.atmire.sword.validation.model.*;
import java.io.*;
import java.util.*;
import javax.xml.bind.*;
import org.apache.commons.collections.*;
import org.dspace.core.*;

/**
 * Factory that is able to instantiate all required compliance validation categories with their rules based on the
 * Validation Rule definition file (config/item-validation-rules.xml)
 */
public class ComplianceCategoryRulesFactoryBean implements ComplianceCategoryRulesFactory {

    private static final String RULE_DEF_FILE = "item-validation-rules.xml";

    private CategorySetMarshaller marshaller = new CategorySetMarshaller();

    private Map<String, ComplianceRuleBuilder> builderMap = new HashMap<String, ComplianceRuleBuilder>();

    public void setBuilderMap(final Map<String, ComplianceRuleBuilder> builderMap) {
        this.builderMap = builderMap;
    }

    public CompliancePolicy createComplianceRulePolicy() throws ValidationRuleDefinitionException {
        CompliancePolicy output = new CompliancePolicy();

        CategorySet categorySet = loadRuleDefinitionSet();

        if (categorySet != null) {

            if (CollectionUtils.isNotEmpty(categorySet.getCategory())) {
                List<RuleCategory> categories = categorySet.getCategory();
                for (RuleCategory categoryDefinition : categories) {
                    ComplianceCategory category = convertToComplianceCategory(categoryDefinition);

                    if (category != null) {
                        output.addComplianceCategory(category);
                    }
                }
            }

            List<ComplianceRule> exceptions = convertRuleSet(categorySet.getExceptions());
            if(CollectionUtils.isNotEmpty(exceptions)) {
                output.addExceptionRules(exceptions);
            }
        }

        return output;
    }

    private ComplianceCategory convertToComplianceCategory(final RuleCategory categoryDefinition) throws ValidationRuleDefinitionException {
        if (categoryDefinition == null) {
            return null;
        } else {
            ComplianceCategory category = new ComplianceCategory();
            category.setOrdinal(categoryDefinition.getOrdinal());
            category.setName(categoryDefinition.getName());
            category.setDescription(categoryDefinition.getDescription());
            category.setResolutionHint(categoryDefinition.getResolutionHint());

            List<ComplianceRule> rules = convertRuleSet(categoryDefinition.getRules());
            if (CollectionUtils.isNotEmpty(rules)) {
                category.addComplianceRules(rules);
            }

            List<ComplianceRule> exceptions = convertRuleSet(categoryDefinition.getExceptions());
            if (CollectionUtils.isNotEmpty(exceptions)) {
                category.addExceptionRules(exceptions);
            }

            return category;
        }
    }

    private List<ComplianceRule> convertRuleSet(final RuleSet rules) throws ValidationRuleDefinitionException {
        List<ComplianceRule> output = new LinkedList<ComplianceRule>();

        if(rules != null && CollectionUtils.isNotEmpty(rules.getRule())) {
            List<RuleDefinition> definitions = rules.getRule();
            for (RuleDefinition ruleDefinition : definitions) {
                ComplianceRule rule = convertToComplianceRule(ruleDefinition);

                if (rule != null) {
                    output.add(rule);
                }
            }
        }

        return output;
    }

    private ComplianceRule convertToComplianceRule(final RuleDefinition ruleDefinition) throws ValidationRuleDefinitionException {
        ComplianceRule rule = null;
        ComplianceRuleBuilder builder = builderMap.get(ruleDefinition.getType());

        if (builder == null) {
            throw new ValidationRuleDefinitionException("Unable to find a rule builder for rule type " + ruleDefinition.getType());
        } else {
            rule = builder.buildRule(ruleDefinition);

            if (ruleDefinition.getExceptions() != null && CollectionUtils.isNotEmpty(ruleDefinition.getExceptions().getRule())) {
                for (RuleDefinition ruleDefinition1 : ruleDefinition.getExceptions().getRule()) {
                    ComplianceRule complianceRule = convertToComplianceRule(ruleDefinition1);

                    if (complianceRule != null) {
                        rule.addExceptionRule(complianceRule);
                    }
                }
            }

            if (ruleDefinition.getPreconditions() != null && CollectionUtils.isNotEmpty(ruleDefinition.getPreconditions().getRule())) {
                for (RuleDefinition ruleDefinition1 : ruleDefinition.getPreconditions().getRule()) {
                    ComplianceRule complianceRule = convertToComplianceRule(ruleDefinition1);

                    if (complianceRule != null) {
                        rule.addPreconditionRule(complianceRule);
                    }
                }
            }
        }
        return rule;
    }

    private CategorySet loadRuleDefinitionSet() throws ValidationRuleDefinitionException {
        CategorySet result = null;
        File file = new File(ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator + RULE_DEF_FILE);

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            result = marshaller.unmarshal(inputStream);
            inputStream.close();

        } catch (FileNotFoundException e) {
            throw new ValidationRuleDefinitionException("The validation rule definition file " + RULE_DEF_FILE + " was not found.", e);
        } catch (IOException e) {
            throw new ValidationRuleDefinitionException("There was a problem reading the validation rule definition file " + RULE_DEF_FILE + ".", e);
        } catch (JAXBException e) {
            throw new ValidationRuleDefinitionException("There was a problem unmarshalling the validation rule definitions from file " + RULE_DEF_FILE + ".", e);
        }

        return result;
    }
}
