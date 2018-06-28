package com.atmire.submission;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 23/03/16
 * Time: 11:02
 */
public class MilieuinfoComplianceStep extends AbstractProcessingStep {

    public static final int STATUS_BLOCKED = 2;

    @Override
    public int doProcessing(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo
    ) throws ServletException, IOException, SQLException, AuthorizeException {
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        if (NEXT_BUTTON.equals(buttonPressed)) {
            return STATUS_COMPLETE;
        } else {
            return STATUS_BLOCKED;
        }
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo)
            throws ServletException {
        return 1;
    }
}
