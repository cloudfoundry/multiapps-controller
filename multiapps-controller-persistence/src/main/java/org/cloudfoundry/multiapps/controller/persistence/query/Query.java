package org.cloudfoundry.multiapps.controller.persistence.query;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

public interface Query<R, T extends Query<?, ?>> {

    T limitOnSelect(int limit);

    T offsetOnSelect(int offset);

    /**
     * @return the result
     * @throws NoResultException Thrown when there is no result. Don't use this method if it's possible for this query to match nothing.
     * @throws NonUniqueResultException Thrown when there are multiple results. If you want to get the first of these, use
     *         {@code limitOnSelect(1).list()} instead.
     */
    R singleResult() throws NoResultException, NonUniqueResultException;

    List<R> list();

    int delete();

}