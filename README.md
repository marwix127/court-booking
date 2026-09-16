# Court Booking

[![CI](https://github.com/marwix127/court-booking/actions/workflows/ci.yml/badge.svg)](https://github.com/marwix127/court-booking/actions/workflows/ci.yml)

REST API for booking the courts of a sports club: padel, tennis and futsal.

Built with Spring Boot and PostgreSQL. The point of this project wasn't to write another CRUD
app, but to properly solve the problem a booking system actually has: **making sure two people
can't book the same court at the same time, not even when they try within the same
millisecond**.

---

## The double-booking problem

The obvious solution is the wrong one:

```java
if (!overlappingBookingExists(court, start, end)) {
    save(booking);            // ⟵ another request fits right here
}
```

Between the check and the `INSERT` there's a window another request can slip into: both query,
both see the slot free, both insert. PostgreSQL's default isolation level (`READ COMMITTED`)
does nothing to prevent it. Under light traffic it never happens, which is exactly why this
kind of bug reaches production.

Here the guarantee lives in the database, as an exclusion constraint:

```sql
CONSTRAINT booking_no_overlap EXCLUDE USING gist (
    court_id                            WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
) WHERE (status <> 'CANCELLED')
```

Two rows for the same court cannot hold overlapping time ranges. There's no race window
because there are no two steps: the engine checks it as part of the `INSERT` itself.

Three details worth noting about that constraint:

- The range is `[)`, end-exclusive. A 10:00–11:00 booking and an 11:00–12:00 booking do **not**
  overlap.
- It's **partial** (`WHERE status <> 'CANCELLED'`). Cancelling releases the slot while keeping
  the row as history.
- It needs the `btree_gist` extension so `=` on `court_id` and `&&` on the range can live in
  the same index.

The service follows from that: it doesn't check first, it attempts the insert and translates
PostgreSQL's rejection into a `409 Conflict`. Optimistic rather than defensive.

### The deadlock I didn't see coming

I found this while writing the tests, and it's the reason writing them was worth it.

With two concurrent requests everything worked. With eight, PostgreSQL started returning this:

```
ERROR: deadlock detected
Where: while checking exclusion constraint on tuple (0,8) in relation "bookings"
```

When several transactions insert overlapping ranges at once, they end up waiting on each other
while the constraint is validated, and the engine kills one of them as a deadlock victim. That
surfaces as `CannotAcquireLockException` (SQLState `40P01`), **not** as a constraint violation
(`23P01`), so my `catch` didn't cover it and the user got a 500.

A deadlock victim doesn't know whether the slot was taken or whether it just lost a lock race.
Returning a 409 outright would reject bookings that were actually possible. So it retries,
**opening a fresh transaction** each time — an aborted transaction can't be reused, which is why
that method uses `TransactionTemplate` instead of `@Transactional`. By the time the retry runs
the rival transaction has finished, so the answer is deterministic: either a clean conflict, or
the booking goes through.

---

## Stack

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Database | PostgreSQL 17 |
| Persistence | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Security | Spring Security + JWT (HMAC-SHA256) |
| Tests | JUnit 5 + Testcontainers |
| Build | Maven (wrapper included) |

---

## Getting started

You need **JDK 21** and **Docker**. Maven isn't required — the wrapper is in the repo.

```bash
# 1. Start PostgreSQL
docker compose up -d --wait

# 2. Run the application (Flyway creates the schema on first start)
./mvnw spring-boot:run
```

The API comes up on `http://localhost:8080`.

The container publishes PostgreSQL on host port **5433** rather than 5432, to avoid clashing
with a local PostgreSQL install. If you change that, adjust `DB_PORT` accordingly.

### Seed data

There are no admin endpoints yet, so courts and their opening hours are loaded via SQL. This
gives you one usable court, open 09:00–22:00 every day:

```bash
docker exec -i court-booking-db psql -U court -d court_booking <<'SQL'
INSERT INTO courts (id, name, court_type, slot_minutes)
VALUES (gen_random_uuid(), 'Padel 1', 'PADEL', 60);

INSERT INTO opening_hours (id, court_id, day_of_week, opens_at, closes_at)
SELECT gen_random_uuid(), c.id, d, '09:00', '22:00'
FROM courts c, generate_series(1, 7) AS d;
SQL
```

Valid court types are `PADEL`, `TENNIS` and `FUTSAL`, and `slot_minutes` must be between 15 and
240 and a multiple of 15.

### Environment variables

Every one of these has a sensible local default, so the project runs with no configuration at
all.

| Variable | Default | |
|---|---|---|
| `DB_HOST` / `DB_PORT` | `localhost` / `5433` | |
| `DB_NAME` | `court_booking` | |
| `DB_USER` / `DB_PASSWORD` | `court` / `court_dev_password` | |
| `JWT_SECRET` | development value | **Change it before deploying.** At least 32 bytes |
| `JWT_TTL_MINUTES` | `60` | Token lifetime |

---

## API

| Method | Path | Auth | |
|---|---|---|---|
| `POST` | `/api/users` | — | Register |
| `POST` | `/api/auth/login` | — | Returns a token |
| `GET` | `/api/courts` | — | List courts |
| `GET` | `/api/courts/{id}` | — | A single court |
| `GET` | `/api/courts/{id}/availability?date=YYYY-MM-DD` | — | Free slots for a day |
| `POST` | `/api/bookings` | Yes | Create a booking |
| `POST` | `/api/bookings/{id}/cancel` | Yes | Cancel |

### End-to-end example

```bash
# Register
curl -X POST localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"claveSegura99","fullName":"Ana García"}'

# Log in
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"claveSegura99"}' \
  | jq -r .accessToken)

# What's free on 8 October?
COURT=$(curl -s localhost:8080/api/courts | jq -r '.[0].id')
curl -s "localhost:8080/api/courts/$COURT/availability?date=2026-10-08" | jq

# Book 10:00–11:00 local time (Madrid is UTC+2 in summer, so 08:00Z)
curl -X POST localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"courtId\":\"$COURT\",\"startsAt\":\"2026-10-08T08:00:00Z\",\"endsAt\":\"2026-10-08T09:00:00Z\"}"
```

Run that last call twice and the second one gets a `409`.

### Status codes

Errors come back as `application/problem+json`
([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)), all resolved in a single
`@RestControllerAdvice`. Controllers never decide an error code: they throw domain exceptions
and the advice translates them.

| | |
|---|---|
| `400` | Field validation. Includes an `errors` map with the reason per field |
| `401` | No token, invalid or expired token, bad credentials |
| `403` | Trying to cancel someone else's booking |
| `404` | Court or booking doesn't exist |
| `409` | Slot already booked, email already registered, optimistic-lock conflict |
| `422` | Business rules: outside opening hours, court closed, invalid duration |

The split between `400` and `422` is deliberate: `400` means "your request is malformed",
`422` means "your request is fine, but it can't be fulfilled".

---

## Data model

```
courts ──┬── opening_hours      opening hours per weekday (ISO-8601: 1 = Monday)
         ├── closures           maintenance or event shutdowns
         └── bookings ── users
```

Five tables, created by [`V1__init.sql`](src/main/resources/db/migration/V1__init.sql).
Primary keys are `uuid` so the API doesn't expose sequential identifiers, which would let
anyone enumerate other people's resources and infer how much business the club does.

**Flyway owns the schema.** Hibernate runs with `ddl-auto=validate`: it verifies that the
entities match the database and never modifies it. A mismatched mapping stops the application
from starting, instead of showing up the first time you try to save something.

### Local times vs. instants

`bookings` stores `timestamptz` (absolute instants) while `opening_hours` stores `time` (local
wall-clock times). Crossing the two requires an explicit time zone, and it's declared in
`app.club.timezone` — never inherited from the system.

This isn't hypothetical. A booking at `00:30` on Friday in Madrid is `22:30` on **Thursday** in
UTC, so deriving the day of week from the instant instead of the local time validates it
against the wrong day's opening hours.

For the same reason `hibernate.jdbc.time_zone` is deliberately **not** enabled. It sounds like
a good idea — "store everything in UTC" — but it also applies to timezone-less columns, and it
was shifting `opening_hours` by an hour depending on the JVM's zone. The effect was accepting
bookings outside opening hours. It isn't needed anyway: every instant column is `timestamptz`
and the driver already preserves the absolute instant.

---

## Project layout

Packages are organised **by feature**, not by layer:

```
com.marwix127.court_booking
├── availability/     free-slot calculation
├── bookings/         entity, service, controller, DTOs and booking exceptions
├── closures/
├── court/
├── opening_hours/
├── user/             registration and users
├── security/         SecurityConfig, token issuing, UserDetailsService
└── common/           base exceptions, global error handling, converters
```

Each package holds everything that belongs to it. With four entities, the layered alternative
would leave you with folders of fifteen unrelated files.

`common/` holds the base exceptions (`NotFoundException`, `ConflictException`,
`ForbiddenException`, `UnprocessableException`) and the advice that maps them. Adding a new
domain error is three lines and it inherits its HTTP status without touching anything else.

### Availability

`GET /api/courts/{id}/availability` is the only endpoint doing real work: it splits the opening
hours into `slot_minutes` chunks and drops the ones taken by bookings or closures.

Two decisions baked into it: it runs **one query per table** for the whole day window and
filters in memory (querying per slot would be twenty-six round trips), and the last chunk is
only offered if it fits entirely before closing time.

---

## Authentication

JWTs are issued and validated with Spring Security's own support
(`spring-boot-starter-oauth2-resource-server`): `NimbusJwtEncoder` and `NimbusJwtDecoder`. No
`jjwt`, no hand-written filter — parsing the `Authorization` header, verifying the signature and
enforcing expiry are all done by the filter chain, and rejections come back with the
`WWW-Authenticate` header the spec calls for.

Signing is **HMAC** rather than RSA because the same application both issues and validates the
tokens; there's no other service that needs to verify the signature. With multiple services
you'd switch to RSA, which is those two beans.

The role travels in a `roles` claim and becomes a `ROLE_*` authority when the token is read.
Passwords are stored with BCrypt, which embeds the salt in the hash itself.

Two things the server always decides, never the client:

- **The role on registration.** `RegisterRequest` has no `role` field. If it did, anyone could
  sign up as an administrator.
- **Who a booking belongs to.** `BookingRequest` has no `userId`; it comes from the
  authenticated identity. Otherwise you could book on someone else's behalf.

---

## Tests

```bash
./mvnw test
```

The tests spin up their own PostgreSQL through Testcontainers, so **Docker is required** but
there's nothing to set up.

It had to be real PostgreSQL: H2 doesn't support `EXCLUDE USING gist`. Against H2 the overlap
tests would pass green while verifying nothing at all — which is worse than not having them.

[`BookingOverlapTest`](src/test/java/com/marwix127/court_booking/bookings/BookingOverlapTest.java)
covers what the project is about:

- **Eight simultaneous attempts at the same slot.** Eight threads parked on a `CountDownLatch`
  and released at once. It asserts that exactly one is confirmed, that the other seven are
  rejected **because of the overlap and not some other error**, and that a single active row
  ends up in the table.
- **Adjacent bookings.** That 11:00–12:00 right after 10:00–11:00 isn't treated as an overlap.
- **Cancelling frees the slot.** That someone else can then book it, and that the cancelled row
  is kept.

None of the tests are annotated `@Transactional`: each thread needs a real transaction of its
own, and a test-wide transaction wrapping everything would make the scenario meaningless.

---

## Scope

Things left out on purpose, so the project has an ending:

- **No payments.** There are no prices or charges anywhere.
- **One opening window per day.** `opening_hours` has a `UNIQUE (court_id, day_of_week)`, so
  split schedules (morning and afternoon with a midday break) aren't supported.
- **One club, one time zone** — configurable, but global.
- **No admin endpoints.** Courts, opening hours and closures are loaded via SQL.
- **No "my bookings" listing.** You can create and cancel, not browse.

The last two are what I'd add first: a `GET /api/bookings` scoped to the authenticated user,
and a catalogue CRUD behind `ROLE_ADMIN` — the role already exists in the model and in the
tokens, but no endpoint requires it yet. After that, OpenAPI docs via springdoc and a GitHub
Actions workflow running the tests.
