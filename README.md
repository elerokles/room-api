# room-api — Week 5 starter

This is the Week 4 reference solution: Controller → Service → Repository interface,
wired by constructor injection, with Swagger UI. Use it as your base for Weeks 5 and 6
if your own Assignment 2 code does not have a `RoomRepository` interface.

## Requirements

- JDK 21

## Run

```
./gradlew bootRun          # macOS / Linux
gradlew.bat bootRun        # Windows
```

The server starts on http://localhost:8080. Run the requests in `api.http` from top to bottom.
Swagger UI: http://localhost:8080/swagger-ui.html

## Endpoints

| Method | Path              | Status          | Description                                        |
|--------|-------------------|-----------------|----------------------------------------------------|
| GET    | `/api/rooms`      | 200             | All rooms; optional `minCapacity` and `keyword`    |
| GET    | `/api/rooms/{id}` | 200 / 404       | One room                                           |
| POST   | `/api/rooms`      | 201 / 400       | Create a room; `Location` header and the new room  |
| PUT    | `/api/rooms/{id}` | 200 / 400 / 404 | Replace a room                                     |
| DELETE | `/api/rooms/{id}` | 204 / 404       | Delete a room                                      |
| GET    | `/api/stats`      | 200             | Number of rooms (dependency-injection demo)        |

Capacity must be between 1 and 20. The rule lives in `RoomService`.

## AI use

(Write one line here if you used AI tools.)
I never used github, so im having help with pushing and commiting things.

# Room API

Spring Boot REST API for rooms, running on MySQL through JPA.

## Database setup

Run these three statements once, as the MySQL `root` user:

```sql
CREATE DATABASE roomdb;
CREATE USER 'roomapp'@'localhost' IDENTIFIED BY 'roomapp1234';
GRANT ALL PRIVILEGES ON roomdb.* TO 'roomapp'@'localhost';
```

Then create the tables and sample data (3 rooms, 5 reservations) with `reset.sql`, which is at the top of the repository:

```
mysql -u roomapp -p roomdb < reset.sql
```

(password: `roomapp1234`). In MySQL Shell, use `\use roomdb` and then `\source reset.sql`.

Run `reset.sql` again any time you want the data back to its starting state, and always before running `api.http`.

The app connects to `jdbc:mysql://localhost:3306/roomdb` as `roomapp`. No root password is stored in the project.

## Run

1. Start MySQL and run the setup above.
2. Start `RoomApiApplication`. It listens on port 8080.
3. Run `api.http` requests 1 to 15, in order, after a reset.

## Expected answers (after a reset)

| # | Request | Answer |
|---|---------|--------|
| 1 | GET /api/rooms | 200, 3 rooms |
| 2 | GET /api/rooms?minCapacity=6 | 200, 2 rooms |
| 3 | GET /api/rooms?keyword=pod | 200, 1 room |
| 4 | GET /api/rooms?minCapacity=6&keyword=roof | 200, 1 room |
| 5 | GET /api/rooms/2 | 200 |
| 6 | GET /api/rooms/99 | 404 |
| 7 | POST /api/rooms | 201, Location: /api/rooms/4 |
| 8 | PUT /api/rooms/4 | 200 |
| 9 | PUT /api/rooms/99 | 404 |
| 10 | DELETE /api/rooms/4 | 204 |
| 11 | DELETE /api/rooms/4 | 404 |
| 12 | POST with capacity 0 | 400 |
| 13 | PUT with capacity 50 | 400 |
| 14 | GET /api/stats | 200, {"rooms": 3} |
| 15 | DELETE /api/rooms/1 | 409 |

## 409 Conflict

The controller returns it, because the status code is an HTTP concern; the service only signals that the database refused the delete (foreign key from `reservation`) and stays unaware of HTTP.

## PUT: SQL printed by Hibernate (request 8)

```sql
select
    r1_0.id,
    r1_0.capacity,
    r1_0.name
from
    room r1_0
where
    r1_0.id=?

update
    room
set
    capacity=?,
    name=?
where
    id=?
```

Hibernate first loads the room by id to check that it exists, then updates its name and capacity in one `UPDATE`, because `save` on an entity that already has an id is a merge.



