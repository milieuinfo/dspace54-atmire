package com.atmire.utils.multithreading;

import org.dspace.core.Context;
import org.dspace.event.Event;

import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 07/01/13
 * Time: 12:07
 */
public class ContextWrapper extends Context {
    /**
     * Construct a new context object. A database connection is opened. No user
     * is authenticated.
     *
     * @throws SQLException if there was an error obtaining a database connection
     */
    public ContextWrapper() throws SQLException {
        super();
    }


    private ReentrantLock lock=new ReentrantLock();

    /**
     * Add an event to be dispatched when this context is committed.
     *
     * @param event
     */
    @Override
    public void addEvent(Event event) {
        try {
            lock.lock();
            super.addEvent(event);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Commit any transaction that is currently in progress, but do not close
     * the context.
     *
     * @throws SQLException if there was an error completing the database transaction
     *                               or closing the connection
     */
    @Override
    public void commit() throws SQLException {
        try {
            lock.lock();
            super.commit();
        } finally {
            lock.unlock();
        }

    }
}
