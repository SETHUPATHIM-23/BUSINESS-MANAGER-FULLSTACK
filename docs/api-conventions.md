# API Conventions

## Pagination and Sorting

All endpoints that return collections or lists of data must implement pagination and sorting using a standard contract to ensure consistency and performance (traces to NFR-USE-010).

### Query Parameters

Every list endpoint will accept the following standard query parameters:
- `page`: The zero-indexed page number to retrieve (default: 0).
- `size`: The number of records per page (default: 20, max: 100).
- `sort`: The sorting instructions in the format `field,direction`. Multiple sort parameters can be provided (e.g., `sort=createdAt,desc&sort=name,asc`).

### Filtering

In addition to standard pagination and sorting, each module's list endpoints will accept a generic filter object (usually passed as mapped query parameters for GET requests). The filter fields depend on the specific module (e.g., `CustomerFilter`, `ProductFilter`) but adhere to the same structural approach.

### Response Shape

List endpoints must wrap the returned data in the `PageResponse<T>` DTO, which provides a standard representation of a Spring Data `Page<T>`.

```json
{
  "content": [
    {
      "id": 1,
      // ... DTO fields
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "pageNumber": 0,
  "pageSize": 20
}
```

## DTO Mapping

We use MapStruct for mapping between internal Entities and external DTOs.
All mappers should extend the generic `BaseMapper<D, E>` interface where `D` is the DTO type and `E` is the Entity type. This ensures a consistent approach to generating standard mapping functions across the entire application:

- `toEntity(dto)`
- `toDto(entity)`
- `toEntityList(dtoList)`
- `toDtoList(entityList)`
