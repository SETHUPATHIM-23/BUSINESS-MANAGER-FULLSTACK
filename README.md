# BusinessManager Enterprise

BusinessManager Enterprise is a monorepo containing the frontend and backend applications.

## Architecture

The system uses a modern web architecture:

- **Frontend**: React application (`frontend/` directory)
- **Backend API**: Spring Boot REST API (`backend/` directory)
  - **Service Layer**: Contains business logic
  - **Repository Layer**: Handles data access
- **Database**: MySQL

### Directory Structure

- `frontend/`: React frontend application
- `backend/`: Spring Boot backend API
- `docs/`: Documentation including SRS and SDD

### Database Migrations (Flyway)

We use Flyway for database migrations. All migration scripts are located in `backend/src/main/resources/db/migration/`.

**Migration Naming Convention:**
Every database change must be introduced via a new migration script following this precise format:
`V{n}__{description}.sql`

- `{n}`: The sequential version number (e.g., `1`, `2`, `3`, `4`). **Do not skip numbers or use decimals/dates** to prevent collision and ensure predictable ordering.
- `__`: Two underscores separating the version and description.
- `{description}`: A short, snake_case description of the change (e.g., `create_users_table`, `add_status_to_orders`).

*Example:* `V2__create_users_table.sql`

> **IMPORTANT**: Every subsequent module prompt must follow this exact convention to ensure migration numbers never collide. Check existing migrations before creating a new one to increment the version number correctly.

### Entity and Database Conventions

1. **Base Entity**: All primary JPA entities must extend `BaseEntity`. It provides the following fields automatically managed by Spring Data JPA Auditing:
   - `id` (Long, Auto-increment PK)
   - `createdAt` (Timestamp)
   - `updatedAt` (Timestamp)
   - `createdBy` (String - username from Security Context)
   - `updatedBy` (String - username from Security Context)
   - `version` (Long - for Optimistic Locking)

2. **Soft Deletes**: Wherever the SRS specifies that a record "may only be deactivated" (e.g., CUST-080, SUPP-050, PROD-080) instead of permanently deleted, use a status column based on the `RecordStatus` enum (`ACTIVE`, `INACTIVE`). Do not use hard deletes (`repository.delete()`) for these entities; instead, transition their status to `INACTIVE`.

### API Error Handling

All REST APIs must rely on the global `@RestControllerAdvice` (`GlobalExceptionHandler`) rather than catching exceptions locally. The standard JSON error response shape is defined by `ErrorResponse` and includes:
- `timestamp`: Date and time of the error.
- `status`: HTTP status code (e.g., 400, 404, 409, 500).
- `error`: HTTP status reason phrase.
- `message`: Specific error message or detail.
- `path`: The requested URI path.
- `fieldErrors`: Optional list of specific validation errors (e.g., for `MethodArgumentNotValidException`).

**Custom Exceptions:**
- Throw `ResourceNotFoundException` when looking up a record by ID that does not exist. (Returns `404 Not Found`).
- Throw `BusinessRuleException` when an operation violates an SRS business rule. (Returns `409 Conflict`).
