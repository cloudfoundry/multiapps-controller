package com.sap.cloud.lm.sl.cf.core.persistence.query;

import java.util.List;

import javax.persistence.NoResultException;

public interface Query<R, T extends Query<?, ?>> {

    T limitOnSelect(int limit);

    T offsetOnSelect(int offset);

    R singleResult() throws NoResultException;

    List<R> list();

    int delete();

}