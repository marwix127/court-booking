-- ===========================================================================
-- V1: esquema inicial
-- ===========================================================================

-- btree_gist permite combinar el operador '=' sobre una columna escalar
-- (court_id) con '&&' sobre un rango dentro de la misma restriccion EXCLUDE.
-- Sin esta extension, booking_no_overlap no se puede crear.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name     varchar(120) NOT NULL,
    role          varchar(20)  NOT NULL DEFAULT 'USER',
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('USER','ADMIN'))
);

CREATE TABLE courts (
    id           uuid        PRIMARY KEY,
    name         varchar(80) NOT NULL,
    court_type   varchar(20) NOT NULL,
    slot_minutes smallint    NOT NULL,
    active       boolean     NOT NULL DEFAULT true,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_courts_name UNIQUE (name),
    CONSTRAINT ck_courts_type CHECK (court_type IN ('PADEL','TENNIS','FUTSAL')),
    CONSTRAINT ck_courts_slot CHECK (slot_minutes BETWEEN 15 AND 240 AND slot_minutes % 15 = 0)
);

CREATE TABLE opening_hours (
    id          uuid     PRIMARY KEY,
    court_id    uuid     NOT NULL REFERENCES courts(id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL,          -- ISO-8601: 1 = lunes ... 7 = domingo
    opens_at    time     NOT NULL,
    closes_at   time     NOT NULL,
    CONSTRAINT uq_opening_hours_court_day UNIQUE (court_id, day_of_week),
    CONSTRAINT ck_opening_hours_dow       CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_opening_hours_range     CHECK (closes_at > opens_at)
);

CREATE TABLE closures (
    id         uuid         PRIMARY KEY,
    court_id   uuid         NOT NULL REFERENCES courts(id) ON DELETE CASCADE,
    starts_at  timestamptz  NOT NULL,
    ends_at    timestamptz  NOT NULL,
    reason     varchar(200) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_closures_range CHECK (ends_at > starts_at)
);

CREATE INDEX ix_closures_court_time ON closures (court_id, starts_at, ends_at);

CREATE TABLE bookings (
    id         uuid        PRIMARY KEY,
    court_id   uuid        NOT NULL REFERENCES courts(id),
    user_id    uuid        NOT NULL REFERENCES users(id),
    starts_at  timestamptz NOT NULL,
    ends_at    timestamptz NOT NULL,
    status     varchar(20) NOT NULL,
    version    bigint      NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_bookings_range  CHECK (ends_at > starts_at),
    CONSTRAINT ck_bookings_status CHECK (status IN ('CONFIRMED','CANCELLED')),

    -- Garantia dura contra el doble booking.
    -- Dos filas de la misma pista no pueden tener rangos [inicio, fin) que se solapen.
    -- Las canceladas quedan fuera del indice (restriccion parcial) para poder
    -- reservar de nuevo un hueco liberado.
    CONSTRAINT booking_no_overlap EXCLUDE USING gist (
        court_id                              WITH =,
        tstzrange(starts_at, ends_at, '[)')   WITH &&
    ) WHERE (status <> 'CANCELLED')
);

CREATE INDEX ix_bookings_user ON bookings (user_id, starts_at DESC);
