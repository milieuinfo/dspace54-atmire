package com.atmire.consumer;

/**
 * @author philip at atmire.com
 */
public interface AsynchronousConsumer extends Runnable {

    public void setObjectId(int objectId) ;

    public void setObjectType(int objectType);
}
