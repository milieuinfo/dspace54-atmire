package com.atmire.scripts;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
* Created by: Antoine Snyers (antoine at atmire dot com)
* Date: 21 May 2014
*/
public class ContextScript extends Script {

    protected Context context;

    public ContextScript() {
    }

    public static void main(String[] args) {
        Script Script = new ContextScript();
        Script.mainImpl(args);
    }

    protected void mainImpl(String[] args) {
        context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();

            super.mainImpl(args);

        } catch (SQLException e) {
            printAndLogError(e);
        } finally {
            if (context != null) {
                context.abort();
            }
        }
    }

    @Override
    protected int processLine(CommandLine line) throws ParseException {
        // no extra arguments
        return 0;
    }


}
