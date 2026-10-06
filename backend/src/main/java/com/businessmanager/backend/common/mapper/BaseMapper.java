package com.businessmanager.backend.common.mapper;

import java.util.List;

/**
 * Base MapStruct mapper convention.
 * @param <D> the DTO type
 * @param <E> the Entity type
 */
public interface BaseMapper<D, E> {
    
    E toEntity(D dto);
    
    D toDto(E entity);
    
    List<E> toEntityList(List<D> dtoList);
    
    List<D> toDtoList(List<E> entityList);
}
