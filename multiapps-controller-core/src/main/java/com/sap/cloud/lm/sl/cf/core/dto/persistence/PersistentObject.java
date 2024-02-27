package com.sap.cloud.lm.sl.cf.core.dto.persistence;

public class PersistentObject<T> {

    private long id;
    private T object;

    public PersistentObject(long id, T object) {
        this.id = id;
        this.object = object;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

}
