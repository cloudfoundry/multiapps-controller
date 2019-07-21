package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.DtoWithPrimaryKey;

public abstract class AbstractDao<T, D extends DtoWithPrimaryKey<P>, P> {

    public void add(T object) {
        getDtoDao().add(toDto(object));
    }

    public T update(P primaryKey, T newObject) {
        D newDto = getDtoDao().update(primaryKey, toDto(newObject));
        return fromDto(newDto);
    }

    public void remove(P primaryKey) {
        getDtoDao().remove(primaryKey);
    }

    public void removeAll(List<T> objects) {
        getDtoDao().removeAll(toDtos(objects));
    }

    public List<T> findAll() {
        return getDtoDao().findAll()
            .stream()
            .map(this::fromDto)
            .collect(Collectors.toList());
    }

    public T find(P primaryKey) {
        D dto = getDtoDao().find(primaryKey);
        return dto != null ? fromDto(dto) : null;
    }

    protected List<D> toDtos(List<T> objects) {
        return objects.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    protected List<T> fromDtos(List<D> dtos) {
        return dtos.stream()
            .map(this::fromDto)
            .collect(Collectors.toList());
    }

    protected abstract AbstractDtoDao<D, P> getDtoDao();

    protected abstract T fromDto(D dto);

    protected abstract D toDto(T object);
}
