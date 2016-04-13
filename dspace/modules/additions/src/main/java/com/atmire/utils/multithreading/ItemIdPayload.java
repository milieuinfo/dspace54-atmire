package com.atmire.utils.multithreading;

import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 07/01/13
 * Time: 10:24
 */
public abstract class ItemIdPayload implements Runnable {

    private Integer id;

    private Context context;

    public ItemIdPayload() {
    }

    public Integer getId() {

        return id;
    }

    protected ItemIdPayload(Integer id, Context context) {
        this.id = id;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public final void run() {
        Item item=null;
        if(id!=null&&context!=null){
            try {
                item= Item.find(context, id);
                doRun(item);
                item.update();
                item.decache();
            } catch (Exception e) {
                if(item!=null) try {
                    item.decache();
                } catch (SQLException e1) {
                    e1.printStackTrace();  //TODO: handle exception
                }
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        } else throw new IllegalArgumentException("Don't run with the prototype itself");
    }

    protected abstract void doRun(Item item);

    protected void intermittent(){};

    protected void finish(){};

    public abstract ItemIdPayload create(Integer id, Context context);

}
