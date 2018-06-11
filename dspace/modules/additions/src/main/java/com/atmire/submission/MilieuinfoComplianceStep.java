package com.atmire.submission;

import com.atmire.sword.result.ComplianceResult;
import com.atmire.sword.service.ComplianceCheckService;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 23/03/16
 * Time: 11:02
 */
public class MilieuinfoComplianceStep extends AbstractProcessingStep {

    public static final int STATUS_BLOCKED = 2;

    private static final String blockSubmissionConfig = "milieuinfo.submission.block.on.rule.violation.";
    private static final String defaultConfig = "default";

    private static ComplianceCheckService complianceCheckService = new DSpace().getServiceManager()
            .getServiceByName("milieuinfoComplianceCheckService", ComplianceCheckService.class);

    @Override
    public int doProcessing(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo
    ) throws ServletException, IOException, SQLException, AuthorizeException {
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        if (NEXT_BUTTON.equals(buttonPressed)) {

            Item item = subInfo.getSubmissionItem().getItem();

            String blockSubmissionOnViolation = ConfigurationManager.getProperty(
                    "item-compliance",
                    blockSubmissionConfig + subInfo.getCollectionHandle()
            );

            if (StringUtils.isBlank(blockSubmissionOnViolation)) {
                blockSubmissionOnViolation = ConfigurationManager.getProperty(
                        "item-compliance",
                        blockSubmissionConfig + defaultConfig
                );
            }

            if (StringUtils.isNotBlank(blockSubmissionOnViolation) && Boolean.parseBoolean(
                    blockSubmissionOnViolation)) {
                ComplianceResult result = complianceCheckService.checkCompliance(context, item);

                if (!result.isCompliant()) {
                    return STATUS_BLOCKED;
                }
            }
        }

        return STATUS_COMPLETE;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo)
            throws ServletException {
        return 1;
    }
}
