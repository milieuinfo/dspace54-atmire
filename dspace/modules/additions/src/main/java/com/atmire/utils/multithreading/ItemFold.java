package com.atmire.utils.multithreading;


import org.dspace.content.ItemIdIterator;
import org.dspace.core.Context;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 07/01/13
 * Time: 10:44
 */
public class ItemFold {

    private Integer threads;
    private ItemIdIterator items;

    public ItemFold(Integer threads, ItemIdPayload payload, ItemIdIterator iterator) {
        this.threads = threads;

        this.payload = payload;
       this.items=iterator;
    }



    private ItemIdPayload payload;
    private AtomicInteger count=new AtomicInteger(0);

    public void execute(Context context) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(threads) {


            @Override
            public boolean offer(Runnable e) {

                try {
                    return offer(e, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            /**
             *
             */
            private static final long serialVersionUID = -5496902746741753845L;

        });
        try {
            LinkedList<Context> contexts=new LinkedList<Context>();
            int commit=100;
            int j=0;

            try {
                context.turnOffAuthorisationSystem();
                for(int i=0;i<threads;i++){
                    Context c=new ContextWrapper();
                    contexts.add(c);
                    c.turnOffAuthorisationSystem();
                }
                long time=System.currentTimeMillis();

                for (; items.hasNext(); ) {
                    Integer item = items.next();
                    Context c=contexts.poll();
                    executor.submit(payload.create(item, c));
                    contexts.offer(c);
                    if((count.incrementAndGet()%commit)==0){
                        payload.intermittent();
                        context.commit();
                        long cur=System.currentTimeMillis();
                        System.out.println(count.get()+" time: "+((cur-time)/1000)+" "+items.getPos()+"/"+items.getTotal());
                        if(((cur-time)/1000)>100)
                            Thread.sleep(1000);
                        time=cur;
                        j++;
                        if(j==contexts.size()) j=0;
                        Context context1=contexts.get(j);
                        context1.commit();
                        context1.clearCache();

                    }

                }
            } finally {
                if (items != null) {
                    items.close();
                }
            }


            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            System.out.println("done: writing to db");
            for(Context c1:contexts)
                c1.complete();
            payload.finish();
            System.out.println("written to db");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
