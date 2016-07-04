package com.atmire.consumer;

import java.util.*;
import org.dspace.utils.*;
import org.springframework.scheduling.concurrent.*;

/**
 * @author philip at atmire.com
 */
public class AsynchronousConsumerDispatcher {

    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private List<String> asynchronousConsumers;

    public void dispatch(int objectId, int objectType) {
        for (String asynchronousConsumerName : asynchronousConsumers) {
            AsynchronousConsumer asynchronousConsumer = new DSpace().getServiceManager().getServiceByName(asynchronousConsumerName, AsynchronousConsumer.class);
            asynchronousConsumer.setObjectId(objectId);
            asynchronousConsumer.setObjectType(objectType);
            threadPoolTaskExecutor.execute(asynchronousConsumer);
        }
    }

    public void setThreadPoolTaskExecutor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    public void setAsynchronousConsumers(List<String> asynchronousConsumers) {
        this.asynchronousConsumers = asynchronousConsumers;
    }
}
