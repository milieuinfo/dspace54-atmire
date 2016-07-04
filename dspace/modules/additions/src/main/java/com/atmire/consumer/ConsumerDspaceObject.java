package com.atmire.consumer;

/**
 * @author philip at atmire.com
 */
public class ConsumerDspaceObject {

    private int id;
    private int type;

    public ConsumerDspaceObject(int id, int type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConsumerDspaceObject that = (ConsumerDspaceObject) o;

        if (id != that.id) return false;
        return type == that.type;

    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + type;
        return result;
    }
}
