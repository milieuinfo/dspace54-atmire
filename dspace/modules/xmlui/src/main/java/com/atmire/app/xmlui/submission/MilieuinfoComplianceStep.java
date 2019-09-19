package com.atmire.app.xmlui.submission;


import static com.atmire.submission.MilieuinfoComplianceStep.STATUS_BLOCKED;

import java.io.IOException;
import java.sql.SQLException;

import com.atmire.app.xmlui.aspect.compliance.ComplianceUI;
import org.apache.cocoon.ProcessingException;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 23/03/16
 * Time: 10:57
 */
public class MilieuinfoComplianceStep extends AbstractSubmissionStep {

    private static final String blockSubmissionConfig = "milieuinfo.submission.block.on.rule.violation.";
    private static final String defaultConfig = "default";

    protected static final Message T_error_blocked =
            message("xmlui.compliance.submission.error_blocked");

    protected static final Message T_head =
            message("xmlui.compliance.submission.head");

    private ComplianceUI complianceUI = new DSpace().getServiceManager()
            .getServiceByName("milieuinfoComplianceUI", ComplianceUI.class);

    @Override
    public List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException,
            AuthorizeException {
        return null;
    }

    @Override
    public void addBody(Body body)
            throws SAXException, WingException, UIException, SQLException, IOException,
            AuthorizeException, ProcessingException {
        org.dspace.content.Item item = submission.getItem();
        Collection collection = submission.getCollection();
        String actionURL
                = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        Division div = body.addInteractiveDivision(
                "submit-describe",
                actionURL,
                Division.METHOD_POST,
                "primary submission"
        );
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);

        div.addList("submit-compliance-1", List.TYPE_FORM)
                .setHead(T_head.parameterize(complianceUI.getShortname()));

        boolean compliant = complianceUI.addComplianceSections(div, item, context);

        setErrorFlagForCompliance(collection, compliant);

        if (this.errorFlag == STATUS_BLOCKED) {
            div.addPara("compliance-error", "compliance-error").addContent(T_error_blocked);
        }

        List form = div.addList("submit-compliance-2", List.TYPE_FORM);

        addControlButtons(form);
    }

    private void setErrorFlagForCompliance(final Collection collection, final boolean compliant) {
        String blockSubmissionOnViolation = ConfigurationManager.getProperty(
                "item-compliance",
                blockSubmissionConfig + collection.getHandle()
        );

        if (StringUtils.isBlank(blockSubmissionOnViolation)) {
            blockSubmissionOnViolation = ConfigurationManager.getProperty(
                    "item-compliance",
                    blockSubmissionConfig + defaultConfig
            );
        }

        if (StringUtils.isNotBlank(blockSubmissionOnViolation) && Boolean.parseBoolean(
                blockSubmissionOnViolation)) {
            if (!compliant) {
                this.errorFlag = STATUS_BLOCKED;
            }
        }
    }

    @Override
    public void addControlButtons(List controls)
            throws WingException
    {
        Item actions = controls.addItem();

        // only have "<-Previous" button if not first step
        if(!isFirstStep())
        {
            actions.addButton(AbstractProcessingStep.PREVIOUS_BUTTON).setValue(T_previous);
        }

        // always show "Save/Cancel"
        actions.addButton(AbstractProcessingStep.CANCEL_BUTTON).setValue(T_save);

        // If last step, show "Complete Submission"
        boolean blocked = this.errorFlag == STATUS_BLOCKED;
        Button nextButton;

        if(blocked) {
            nextButton = actions.addButton("submit_blocked");
        } else {
            nextButton = actions.addButton(AbstractProcessingStep.NEXT_BUTTON);
        }

        if(isLastStep())
        {
            nextButton.setValue(T_complete);
        }
        else // otherwise, show "Next->"
        {
            nextButton.setValue(T_next);
        }

        if(blocked) {
            nextButton.setDisabled(true);
        }
    }
}

