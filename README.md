# Room API

Spring Boot REST API for rooms and reservations, running on MySQL through JPA,
with validation, one error shape for every failure, login with HTTP Basic or a JWT, and roles and
owners (only an admin changes rooms, only the owner moves a booking).

## Requirements

- JDK 21
- MySQL 8

## Database setup

Run these three statements once, as the MySQL `root` user:

```sql
CREATE DATABASE roomdb;
CREATE USER 'roomapp'@'localhost' IDENTIFIED BY 'roomapp1234';
GRANT ALL PRIVILEGES ON roomdb.* TO 'roomapp'@'localhost';
```

Then create the tables and sample data with `reset.sql`, which is at the top of the repository:

```
mysql -u roomapp -p roomdb < reset.sql
```

(password: `roomapp1234`). In MySQL Shell, use `\use roomdb` and then `\source reset.sql`.

`reset.sql` creates three tables and fills them with 3 rooms, 5 reservations and 3 accounts:

| Table         | Columns                                                       |
|---------------|---------------------------------------------------------------|
| `room`        | id (PK), name, capacity (CHECK 1 to 20)                       |
| `app_user`    | id (PK), username (UNIQUE), password_hash (BCrypt), role      |
| `reservation` | id (PK), room_id (FK → room.id), owner_id (FK → app_user.id), start_time, end_time |

In the sample data mina owns reservations 1 and 3, and omar owns 2, 4 and 5.

Run `reset.sql` again any time you want the data back to its starting state, and always before running `api.http`.

The app connects to `jdbc:mysql://localhost:3306/roomdb` as `roomapp`, with `ddl-auto=validate`, so the
tables must exist before it starts (an old `reset.sql` without `owner_id` stops it from starting).
No root password is stored in the project.

## Settings (environment variables)

Secrets and per-machine settings are read from the environment. Each one has a default in
`application.properties` that is for class only, so the project still starts without them.

| Variable       | Setting                        | Class-only default                              |
|----------------|--------------------------------|-------------------------------------------------|
| `DB_PASSWORD`  | `spring.datasource.password`   | `roomapp1234`                                   |
| `JWT_SECRET`   | `app.jwt.secret` (≥ 32 chars)  | `week10-devonly-secret-change-me-0123456789`    |
| `CORS_ORIGINS` | `app.cors.allowed-origins` (comma-separated) | `http://localhost:5500,http://127.0.0.1:5500` |

Set them in IntelliJ (Run configuration → Environment variables), in Windows cmd
(`set JWT_SECRET=...`), PowerShell (`$env:JWT_SECRET="..."`) or macOS/Linux (`export JWT_SECRET=...`)
before `./gradlew bootRun`. Never commit real values.

## Run

1. Start MySQL and run the setup above.
2. Optional: set `DB_PASSWORD`, `JWT_SECRET` and `CORS_ORIGINS` (see Settings). Without them the app uses
   the class-only defaults.
3. Start `RoomApiApplication` (or `./gradlew bootRun`, `gradlew.bat bootRun` on Windows). It listens on port 8080.
4. Run `reset.sql`, then `api.http` requests 1 to 82, in order.

Swagger UI: http://localhost:8080/swagger-ui.html

## Endpoints

| Method | Path                               | Who may call            | Status                      | Description                                     |
|--------|------------------------------------|-------------------------|-----------------------------|-------------------------------------------------|
| GET    | `/api/rooms`                       | anyone                  | 200                         | All rooms; optional `minCapacity` and `keyword` |
| GET    | `/api/rooms/{id}`                  | anyone                  | 200 / 400 / 404             | One room                                        |
| POST   | `/api/rooms`                       | admin                   | 201 / 400 / 401 / 403       | Create a room; `Location` header and the room   |
| PUT    | `/api/rooms/{id}`                  | admin                   | 200 / 400 / 401 / 403 / 404 | Replace a room's name and capacity              |
| DELETE | `/api/rooms/{id}`                  | admin                   | 204 / 401 / 403 / 404 / 409 | Delete a room; 409 if it has reservations       |
| GET    | `/api/stats`                       | anyone                  | 200                         | Number of rooms                                 |
| GET    | `/api/rooms/{roomId}/reservations` | anyone                  | 200 / 404                   | Reservations of one room, by start time         |
| POST   | `/api/rooms/{roomId}/reservations` | any logged-in user      | 201 / 400 / 401 / 404 / 409 | Book a room for yourself; 409 if the time overlaps |
| GET    | `/api/reservations/mine`           | any logged-in user      | 200 / 401                   | Your own reservations, by start time            |
| GET    | `/api/reservations/{id}`           | owner or admin          | 200 / 401 / 403 / 404       | One reservation                                 |
| PUT    | `/api/reservations/{id}`           | owner only              | 200 / 400 / 401 / 403 / 404 / 409 | Move a reservation to a new time          |
| DELETE | `/api/reservations/{id}`           | owner or admin          | 204 / 401 / 403 / 404       | Cancel a reservation                            |
| POST   | `/api/auth/signup`                 | anyone                  | 201 / 400 / 409             | Create an account (always role USER)            |
| POST   | `/api/auth/login`                  | anyone                  | 200 / 400 / 401             | Get a JWT                                       |
| GET    | `/api/auth/me`                     | any logged-in user      | 200 / 401                   | Who the server thinks you are                   |

Every endpoint returns a response record (`RoomResponse`, `ReservationResponse`, `UserResponse`), never an entity.

## Rules and where they live

| The rule needs | Examples                                             | Lives in           | Status    |
|----------------|------------------------------------------------------|--------------------|-----------|
| one field      | name required, capacity 1 to 20, startTime required  | the request record (`@Valid`) | 400 |
| two fields     | startTime before endTime                             | the service        | 400       |
| the data       | room exists, no overlap, no reservations left        | the service        | 404 / 409 |
| the caller     | admin for rooms (rule D), owner of a reservation (rule C) | SecurityConfig / the service | 403 |
| (last guard)   | NOT NULL, CHECK, FOREIGN KEY, UNIQUE                 | the database       | 409       |

**409 for a room with reservations.** I chose option B, "ask before you act": `RoomService.delete` checks
`reservationRepository.existsByRoomId(id)` and throws `ConflictException`, so the reason is a clear message
instead of a database error. The foreign key still guards the table, and if it ever refuses a change the
handler still answers 409.

**Overlapping bookings.** Two bookings of one room overlap when `new.start < old.end AND new.end > old.start`.
Request 19 (11:00 to 12:00) succeeds because the comparison is strict: it starts exactly when Mina's
10:00 to 11:00 ends, and touching is not overlapping.

**PUT and dirty checking.** `RoomService.update` loads the room (a managed entity), calls `room.update(...)`
and returns. There is no `save()`: at commit, Hibernate compares the room with its snapshot and writes an
`UPDATE` only if something changed. Request 26 (same values) prints one `select`; request 27 prints a
`select` and an `update`.

**One select for my reservations.** `findMine` uses `join fetch r.room join fetch r.owner`, so
`GET /api/reservations/mine` sends one query instead of one per room and owner (the N+1 problem).

**Moving a reservation never clashes with itself.** `countOverlappingExcept` is the overlap query plus
`r.id <> :id`, so moving a booking to the same time (or a time that only overlaps its old slot) is allowed.

## Errors

Every failure, from a controller, a service or the security filters, has the same body (`ApiError`):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "request has invalid fields",
  "path": "/api/rooms",
  "fieldErrors": [
    { "field": "capacity", "message": "capacity must be between 1 and 20" }
  ]
}
```

`fieldErrors` is empty unless `@Valid` failed. For other errors, `message` says what happened.

| Status | Means | Example message |
|--------|-------|-----------------|
| 400 | The request is wrong: invalid fields, start not before end, body not valid JSON, wrong type in the path, missing parameter | `startTime must be before endTime` |
| 401 | We do not know who you are: no login, wrong password, or a bad or expired token | `login required` |
| 403 | We know who you are, but you may not do this: not an admin, or not your reservation | `admin role required` |
| 404 | No such room, reservation or URL | `room 99 not found` |
| 409 | Valid request, but it clashes with the data: overlap, room has reservations, username taken | `room 1 has reservations` |
| 500 | A bug on our side. The details go to the log, never to the client | `unexpected server error` |

All handlers are in `ApiExceptionHandler` (`@RestControllerAdvice`). A 401 happens in a security filter,
before any controller, so `ApiAuthenticationEntryPoint` passes it to the same handler. A 403 from a URL rule
also happens in a filter, so `ApiAccessDeniedHandler` does the same for it.

## Authentication

Reading rooms (and a room's reservations) is public. Everything else needs a login: every write, all of
`/api/reservations/**` (who booked what is personal) and `/api/auth/me`. Sign-up and login are public.

**Sample accounts** (from `reset.sql`, class only): `mina` / `mina1234`, `omar` / `omar1234` (role USER),
`admin` / `admin1234` (role ADMIN). Passwords are stored only as BCrypt hashes.

**HTTP Basic.** Send the username and password on every request:

```
Authorization: Basic bWluYTptaW5hMTIzNA==
```

(that is base64 of `mina:mina1234`, so anyone can decode it: use HTTPS on a real server).

**Token (JWT).** Log in once and send the token instead of the password:

```
POST /api/auth/login
{ "username": "lee", "password": "lee12345" }

→ { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 1800 }
```

```
Authorization: Bearer eyJ...
```

The token is signed with HS256 using `app.jwt.secret`, which comes from the `JWT_SECRET` environment
variable. It lasts 30 minutes (`app.jwt.ttl-minutes`) and holds the username (`sub`) and the `role`.
A `JwtAuthenticationConverter` turns the `role` claim into `ROLE_USER` or `ROLE_ADMIN`, the same authority
HTTP Basic gets from the database, so every rule works the same with Basic and with a token.

The three 401 messages: `login required` (no login), `wrong username or password`,
`token is invalid or expired`.

## Authorization

Authentication asks who you are (401). Authorization asks whether you may (403). Logging in again does not
change a 403.

| Who               | May                                                                                     |
|-------------------|-----------------------------------------------------------------------------------------|
| anyone            | read rooms, a room's reservations and the stats; sign up; log in                         |
| any logged-in user (USER or ADMIN) | book a room (the booking's owner is you), list your own reservations (`/mine`), see who you are |
| the owner         | read, move (`PUT`) and cancel their own reservation                                     |
| an admin (ADMIN)  | create, change and delete rooms; read and cancel **any** reservation                    |

**Rule D, roles: only an admin changes rooms.** Checked by URL rules in `SecurityConfig`:
`hasRole("ADMIN")` on `POST /api/rooms`, `PUT /api/rooms/*` and `DELETE /api/rooms/*`. Without a login the
answer is 401 `login required`; a logged-in USER gets 403 `admin role required`. `/api/rooms/*` matches one
path segment only, so booking (`POST /api/rooms/{id}/reservations`) stays open to every logged-in user.

**Rule C, owners: a reservation belongs to an account.** `reservation.owner_id` is a foreign key to
`app_user`. The owner is always the logged-in user (`authentication.getName()`); the request body has no name
field, so nobody can book in someone else's name. Ownership needs the reservation from the database, so it is
checked in `ReservationService`, not in a URL rule:

- `GET` and `DELETE /api/reservations/{id}`: the owner or an admin.
- `PUT /api/reservations/{id}`: the owner only. Not even an admin may move someone else's booking.
- Anyone else gets 403 `reservation {id} belongs to another user`.

**Order of checks.** 401 (filter) → 400 (bad times) → 404 (no such reservation) → 403 (not yours) → 409
(overlap). Because we look the reservation up first, a stranger learns that it exists (404 before 403).
I chose 403 rather than hiding it behind a 404, as in the brief, and use it everywhere.

**Roles are never chosen by the client.** Sign-up always creates a USER, even if the body says
`"role": "ADMIN"`. The only admin is the `admin` account from `reset.sql`.

**Both 403s use `ApiError`.** Rule D's 403 (Spring's `AccessDeniedException`) answers `admin role required`;
rule C's 403 (our `ForbiddenException`) answers `reservation {id} belongs to another user`.

**CORS.** Browser pages may call the API only from the origins in `CORS_ORIGINS` (methods GET, POST, PUT,
DELETE; headers `Authorization`, `Content-Type`; `Location` is exposed). A preflight from another origin is
refused with 403 `Invalid CORS request`. CORS is enforced by browsers only; curl and api.http are not affected.

## Expected answers (after a reset, 1 to 82 in order)

Each request in `api.http` has a comment with the answer it should give. The Week 11 requests:

| # | Request | Who | Answer |
|---|---------|-----|--------|
| 52 | GET /api/auth/me with the token | lee | 200, ROLE_USER |
| 57 | POST /api/auth/signup with "role": "ADMIN" | anyone | 201, role USER |
| 58 | POST /api/auth/login admin | anyone | 200, keeps adminToken |
| 59 | GET /api/auth/me | admin token | 200, ROLE_ADMIN |
| 60 | POST /api/rooms | admin token | 201, /api/rooms/5 |
| 61 | POST /api/rooms | mina | 403, admin role required |
| 62 | PUT /api/rooms/5 | lee token | 403, admin role required |
| 63 | DELETE /api/rooms/5 | nobody | 401, login required |
| 64 | POST /api/rooms/5/reservations | mina | 201, /api/reservations/9 |
| 65 | DELETE /api/rooms/5 | admin token | 409, room 5 has reservations |
| 66 | GET /api/reservations/mine | mina | 200, 5: ids 3, 1, 6, 8, 9 |
| 67 | GET /api/reservations/mine | omar | 200, 3: ids 2, 4, 5 |
| 68 | GET /api/reservations/9 | omar | 403, belongs to another user |
| 69 | GET /api/reservations/9 | admin token | 200 |
| 70 | DELETE /api/reservations/9 | omar | 403 |
| 71 | GET /api/reservations/99 | omar | 404 |
| 72 | PUT /api/reservations/9 14:00–15:00 | mina | 200 |
| 73 | PUT /api/reservations/9 same time | mina | 200 (not itself) |
| 74 | PUT /api/reservations/9 | omar | 403 |
| 75 | PUT /api/reservations/9 | admin token | 403, owner only |
| 76 | PUT /api/reservations/6 onto 10:30 on 6 Oct | mina | 409, already booked |
| 77 | PUT /api/reservations/9 end before start | mina | 400 |
| 78 | PUT /api/reservations/99 | mina | 404 |
| 79–80 | DELETE reservation 9, then room 5 | admin token | 204, 204 |
| 81 | OPTIONS preflight from http://localhost:5500 | browser | 200 with Access-Control-Allow-Origin |
| 82 | OPTIONS preflight from another site | browser | 403, Invalid CORS request |

Requests 49 and 52 also list a `FACTOR_PASSWORD` or `FACTOR_BEARER` authority: Spring Security 7 adds one
for the way you logged in.

## AI use

I used Claude to learn how to commit and push with GitHub.
