package com.atmire.app.xmlui.aspect.compliance;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;

import java.sql.SQLException;

import com.atmire.sword.result.CategoryComplianceResult;
import com.atmire.sword.result.ComplianceResult;
import com.atmire.sword.result.RuleComplianceResult;
import com.atmire.sword.service.ComplianceCheckService;
import org.apache.cocoon.components.flow.WebContinuation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.core.Context;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 22/03/16
 * Time: 16:15
 */
public class ComplianceUI {

    protected static final Message T_info1 =
            message("xmlui.compliance.ComplianceUI.info1");

    protected static final Message T_continue =
            message("xmlui.compliance.ComplianceUI.continue");

    protected static final Message T_continue_info =
            message("xmlui.compliance.ComplianceUI.continue_info");

    protected static final Message T_return =
            message("xmlui.compliance.ComplianceUI.return");

    protected static final Message T_return_info =
            message("xmlui.compliance.ComplianceUI.return_info");

    protected static final Message T_cancel =
            message("xmlui.compliance.ComplianceUI.cancel");

    protected static final Message T_top_information_bar_compliant =
            message("xmlui.compliance.ComplianceUI.top_information_bar_compliant");

    protected static final Message T_top_information_bar_not_compliant =
            message("xmlui.compliance.ComplianceUI.top_information_bar_not_compliant");

    protected static final Message T_top_information_bar_not_applicable =
            message("xmlui.compliance.ComplianceUI.top_information_bar_not_applicable");

    protected static final Message T_resolution_summary_title =
            message("xmlui.compliance.ComplianceUI.resolution_summary_title");

    protected static final Message T_exceptions_section_title =
            message("xmlui.compliance.ComplianceUI.exceptions_section_title");

    protected static final Message T_circle_icon =
            message("xmlui.compliance.ComplianceUI.circle_icon");

    protected static final Message T_rules_show_all =
            message("xmlui.compliance.ComplianceUI.rules_show_all");

    protected static final Message T_rules_show_applicable =
            message("xmlui.compliance.ComplianceUI.rules_show_applicable");

    protected static final Message T_rules_show =
            message("xmlui.compliance.ComplianceUI.rules_show");

    protected static final Message T_rules_hide =
            message("xmlui.compliance.ComplianceUI.rules_hide");

    protected static final Message T_not_applicable =
            message("xmlui.compliance.ComplianceUI.not_applicable");

    protected static final Message T_workflow_disabled_hint =
            message("xmlui.compliance.ComplianceUI.workflow_disabled_hint");

    protected static final Message T_applicable_help =
            message("xmlui.compliance.ComplianceUI.applicable_help_");

    protected static final Message T_not_applicable_help =
            message("xmlui.compliance.ComplianceUI.not_applicable_help_");

    private String identifier;

    private String name;

    private String shortname;

    private String navigationKey;

    private ComplianceRelatedData complianceRelatedData;

    private ComplianceCheckService complianceCheckService;

    public void addComplianceSections(
            Division div,
            org.dspace.content.Item item,
            org.dspace.core.Context context
    ) throws WingException {
        ComplianceResult result = complianceCheckService.checkCompliance(context, item);

        if (result.isApplicable()) {
            addTopInformationBar(div, result);
            addResolutionSummaryBar(div, result);
            addShowAllRulesLink(div);

            div.addDivision("help-section")
                    .addPara()
                    .addContent(new Message("default", T_applicable_help.getKey() + identifier));

            addExceptionsSection(div, result);
            addCategorySections(div, result);

            if (complianceRelatedData != null) {
                complianceRelatedData.renderRelatedData(context, item, result, div);
            }
        } else {
            addTopInformationBar(div, result);
            addItemNotApplicableSection(div, result);
        }
    }

    private void addShowRulesLink(Division div) throws WingException {
        Division allRulesDiv = div.addDivision("show-rules", "show-rules");
        allRulesDiv.addPara("rules_show", "").addXref("", T_rules_show);
        allRulesDiv.addPara("rules_hide", "hidden").addXref("", T_rules_hide);
    }

    private void addShowAllRulesLink(Division div) throws WingException {
        Division allRulesDiv = div.addDivision("all-rules", "all-rules");
        allRulesDiv.addPara("rules_show_all", "").addXref("", T_rules_show_all);
        allRulesDiv.addPara("rules_show_applicable", "hidden").addXref("", T_rules_show_applicable);
    }

    private void addTopInformationBar(Division div, ComplianceResult result) throws WingException {
        Division barDiv = div.addDivision("top-information-bar", "top-information-bar");

        if (!result.isApplicable()) {
            Para para = barDiv.addPara(
                    "top-information-bar-not-applicable",
                    "compliance-bar top-information-bar-not-applicable"
            );
            para.addContent(T_top_information_bar_not_applicable.parameterize(shortname));
        } else if (result.isCompliant()) {
            Para para = barDiv.addPara(
                    "top-information-bar-compliant",
                    "compliance-bar top-information-bar-compliant"
            );
            para.addContent(T_top_information_bar_compliant.parameterize(name));
        } else {
            Para para = barDiv.addPara(
                    "top-information-bar-not-compliant",
                    "compliance-bar top-information-bar-not-compliant"
            );
            para.addContent(T_top_information_bar_not_compliant.parameterize(name));
        }
    }

    private void addResolutionSummaryBar(Division div, ComplianceResult result)
            throws WingException {
        if (!result.isCompliant()) {
            Division barDiv = div.addDivision(
                    "resolution-summary-bar",
                    "compliance-bar resolution-summary-bar"
            );
            barDiv.addPara("", "bold").addContent(T_resolution_summary_title);
            List list = barDiv.addList("resolution-summary-list");
            for (CategoryComplianceResult categoryComplianceResult : result.getOrderedCategoryResults()) {
                if (!categoryComplianceResult.isCompliant() && categoryComplianceResult.isApplicable()) {
                    for (RuleComplianceResult ruleComplianceResult : categoryComplianceResult.getViolatedRules()) {
                        if (StringUtils.isNotBlank(ruleComplianceResult.getResolutionHint())) {
                            list.addItem().addContent(ruleComplianceResult.getResolutionHint());
                        }
                    }

                    if (StringUtils.isNotBlank(categoryComplianceResult.getResolutionHint())) {
                        list.addItem().addContent(categoryComplianceResult.getResolutionHint());
                    }
                }
            }
        }
    }

    private void addExceptionsSection(Division div, ComplianceResult result) throws WingException {
        if (result.isCompliantByException()) {
            Division exceptionsDiv = div.addDivision("exceptions-section", "exceptions-section");
            Para para = exceptionsDiv.addPara(
                    "exceptions-section-title",
                    "exceptions-section-title"
            );
            para.addContent(T_exceptions_section_title);
            List list = exceptionsDiv.addList("exceptions-list");

            for (RuleComplianceResult ruleComplianceResult : result.getAppliedExceptions()) {
                Item item = list.addItem();
                item.addContent(ruleComplianceResult.getResultDescription());
                item.addXref(ruleComplianceResult.getDefinitionHint(),
                             T_circle_icon,
                             "rule-tooltip");
            }
        }
    }

    private void addCategorySections(Division div, ComplianceResult result) throws WingException {

        for (CategoryComplianceResult categoryComplianceResult : result.getOrderedCategoryResults()) {
            boolean applicable = categoryComplianceResult.isApplicable();
            boolean compliantByException = categoryComplianceResult.isCompliantByException();

            String render = "compliance-section";

            if (!applicable) {
                render += " not-applicable-section";
            }

            Division complianceDiv = div.addDivision(
                    "item-compliance-" + categoryComplianceResult.getOrdinal(),
                    render
            );
            Para para = complianceDiv.addPara("category-name", "category-name");
            para.addContent(categoryComplianceResult.getOrdinal() + " " + categoryComplianceResult.getCategoryName());

            if (!applicable) {
                complianceDiv.addPara("not-applicable-text", "not-applicable-text")
                        .addContent(T_not_applicable);
            }

            // this link is rendered as tooltip
            complianceDiv.addPara("question-circle", "question-circle")
                    .addXref(categoryComplianceResult.getCategoryDescription(),
                             T_circle_icon,
                             "rule-tooltip");

            org.dspace.app.xmlui.wing.element.List complianceList = complianceDiv.addList(
                    "item-compliance-list-" + categoryComplianceResult.getCategoryName());

            if (compliantByException) {
                renderCategoryExceptions(complianceList, categoryComplianceResult);
            }

            renderComplianceResultList(
                    complianceList,
                    categoryComplianceResult.getCompliantRules(),
                    categoryComplianceResult.isApplicable(),
                    applicable,
                    compliantByException
            );
            renderComplianceResultList(
                    complianceList,
                    categoryComplianceResult.getViolatedRules(),
                    categoryComplianceResult.isApplicable(),
                    applicable,
                    compliantByException
            );
        }
    }

    private void renderCategoryExceptions(
            List list,
            CategoryComplianceResult categoryComplianceResult
    ) throws WingException {
        for (RuleComplianceResult rule : categoryComplianceResult.getAppliedExceptions()) {
            if (StringUtils.isNotBlank(rule.getResultDescription())) {
                String render = "compliant exception";

                Item ruleItem = list.addItem("compliance-item", render);
                ruleItem.addContent("Toegepaste uitzondering: " + rule.getResultDescription());

                ruleItem.addXref(rule.getDefinitionHint(), T_circle_icon, "rule-tooltip");
            }
        }
    }

    public void renderComplianceResultList(
            List list,
            java.util.List<RuleComplianceResult> rules,
            boolean resultIsApplicable,
            boolean categoryIsApplicable,
            boolean categoryIsCompliantByException
    ) throws WingException {
        for (RuleComplianceResult rule : rules) {
            if (StringUtils.isNotBlank(rule.getResultDescription())) {
                String render = "compliant";

                if (!rule.isCompliant()) {
                    render = "violating";
                }
                if (!rule.isApplicable() || !categoryIsApplicable || categoryIsCompliantByException) {
                    render += " not-applicable-rule hidden";
                }

                if (!resultIsApplicable) {
                    render += " not-applicable";
                }

                Item ruleItem = list.addItem("compliance-item", render);
                ruleItem.addContent(rule.getResultDescription());

                ruleItem.addXref(rule.getDefinitionHint(), T_circle_icon, "rule-tooltip");

                if (rule.isCompliant() && rule.exceptionApplied()) {
                    Highlight highlight = ruleItem.addHighlight("rule-description");
                    highlight.addHighlight("fa fa-exclamation-triangle");
                    highlight.addHighlight("rule-description-text")
                            .addContent("Toegepaste uitzondering: " + rule.getExceptionDescription());
                    highlight.addXref(rule.getExceptionHint(), T_circle_icon, "rule-tooltip");

                } else if (!rule.isCompliant() && CollectionUtils.isNotEmpty(rule.getViolationDescriptions())) {
                    for (String description : rule.getViolationDescriptions()) {
                        Highlight highlight = ruleItem.addHighlight("rule-description");
                        highlight.addHighlight("fa fa-bolt");
                        highlight.addHighlight("rule-description-text").addContent(description);
                    }

                    if (resultIsApplicable && StringUtils.isNotBlank(rule.getResolutionHint())) {
                        Highlight highlight = ruleItem.addHighlight("rule-resolution");
                        highlight.addHighlight("fa fa-arrow-right");
                        highlight.addHighlight("rule-resolution-text")
                                .addContent(rule.getResolutionHint());
                    }
                }
            }
        }
    }

    public void addWorkflowButtons(
            Division div,
            Context context,
            org.dspace.content.Item item,
            WebContinuation knot,
            String collectionHandle
    ) throws WingException, SQLException {
        boolean blockWorkflow = complianceCheckService.blockOnWorkflow(collectionHandle);
        boolean disabled = false;

        if (blockWorkflow) {
            ComplianceResult result = complianceCheckService.checkCompliance(context, item);

            if (!result.isCompliant()) {
                disabled = true;
            }
        }

        Row row;

        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);

        row = table.addRow();
        Cell cell = row.addCell();
        cell.addContent(T_continue_info);

        if (blockWorkflow) {
            cell.addContent(T_workflow_disabled_hint);
        }

        Button submit_continue = row.addCell().addButton("submit_continue");
        submit_continue.setValue(T_continue);
        submit_continue.setDisabled(disabled);


        row = table.addRow();
        row.addCellContent(T_return_info);
        row.addCell().addButton("submit_return").setValue(T_return);

        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_cancel);

        div.addHidden("submission-continue").setValue(knot.getId());

    }

    public void addItemNotApplicableSection(Division div, ComplianceResult result)
            throws WingException {
        Division notApplicableDiv = div.addDivision("item-not-applicable", "item-not-applicable");

        notApplicableDiv.addPara()
                .addContent(new Message("default", T_not_applicable_help.getKey() + identifier));

        addShowRulesLink(notApplicableDiv);

        java.util.List<RuleComplianceResult> violatedPreconditions
                = result.getViolatedPreconditions();

        org.dspace.app.xmlui.wing.element.List complianceList = notApplicableDiv.addList(
                "item-compliance-precondition",
                null,
                "hidden"
        );

        renderComplianceResultList(complianceList, violatedPreconditions, false, true, false);
    }

    public void setComplianceCheckService(ComplianceCheckService complianceCheckService) {
        this.complianceCheckService = complianceCheckService;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getNavigationKey() {
        return navigationKey;
    }

    public void setNavigationKey(String navigationKey) {
        this.navigationKey = navigationKey;
    }

    public ComplianceRelatedData getComplianceRelatedData() {
        return complianceRelatedData;
    }

    public void setComplianceRelatedData(ComplianceRelatedData complianceRelatedData) {
        this.complianceRelatedData = complianceRelatedData;
    }
}

