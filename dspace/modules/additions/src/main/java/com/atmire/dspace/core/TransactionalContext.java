package com.atmire.dspace.core;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jul 2016
 */
public class TransactionalContext extends Context {

    private static Logger log = Logger.getLogger(TransactionalContext.class);

    private int numberCommits = 0;
    private int numberComplete = 0;
    private int numberAbort = 0;

    private boolean doRealCommit = false;

    public TransactionalContext() throws SQLException {
    }

    public TransactionalContext(short options) throws SQLException {
        super(options);
    }

    @Override
    public void commit() throws SQLException {
        if (doRealCommit) {
            super.commit();
        } else {
            numberCommits++;
        }
    }

    @Override
    public void complete() throws SQLException {
        numberComplete++;
    }

    @Override
    public void abort() {
        numberAbort++;
    }

    public void realCommit() throws SQLException {
        log.info("Number of commits:" + numberCommits);
        log.debug(toString());
        if (wasAbortInvoked()) {
            log.debug("Aborted, not proceeding with the real commit");
        } else {
            log.info("Not aborted, proceeding with the real commit");
            long t0 = System.currentTimeMillis();
            super.commit();
            long t1 = System.currentTimeMillis();
            log.info("Commit completed, execution time: ... " + executionTimeFormatted(t0, t1));
        }
    }

    public void realComplete() throws SQLException {
        log.info("Number of completes:" + numberComplete);
        log.debug(toString());
        if (wasAbortInvoked()) {
            log.info("Aborted, not proceeding with the real complete");
        } else {
            log.debug("Not aborted, proceeding with the real complete");
            long t0 = System.currentTimeMillis();
            doRealCommit = true;
            super.complete();
            doRealCommit = false;
            long t1 = System.currentTimeMillis();
            log.info("Complete completed, execution time: ... " + executionTimeFormatted(t0, t1));
        }
    }

    public void realAbort() throws SQLException {
        log.info("Number of aborts:" + numberAbort);
        log.debug(toString());
        log.debug("Proceeding with the real abort");
        long t0 = System.currentTimeMillis();
        super.abort();
        long t1 = System.currentTimeMillis();
        log.info("Abort completed, execution time: ... " + executionTimeFormatted(t0, t1));
    }

    public boolean wasCommitInvoked() {
        return numberCommits > 0;
    }

    public boolean wasCompleteInvoked() {
        return numberComplete > 0;
    }

    public boolean wasAbortInvoked() {
        return numberAbort > 0;
    }

    private String executionTimeFormatted(long t0, long t1) {
        long time = t1 - t0;
        return time + " milliseconds";
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("numberCommits", numberCommits)
                .append("numberComplete", numberComplete)
                .append("numberAbort", numberAbort)
                .toString();
    }
}

