CREATE TABLE students (
    id                     BIGSERIAL PRIMARY KEY,
    first_names            VARCHAR(150) NOT NULL,
    last_names             VARCHAR(150) NOT NULL,
    birth_date             DATE         NOT NULL,
    birth_place            VARCHAR(150),
    nationality            VARCHAR(80)  NOT NULL DEFAULT 'Venezolana',
    sex                    VARCHAR(20)  NOT NULL,
    height_cm              NUMERIC(5,1),
    weight_kg              NUMERIC(5,1),
    school_registry_number VARCHAR(50),
    phone                  VARCHAR(30),
    address                VARCHAR(500),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_students_sex CHECK (sex IN ('FEMALE', 'MALE')),
    CONSTRAINT ck_students_height CHECK (height_cm IS NULL OR (height_cm > 0 AND height_cm < 300)),
    CONSTRAINT ck_students_weight CHECK (weight_kg IS NULL OR (weight_kg > 0 AND weight_kg < 300))
);

CREATE UNIQUE INDEX uk_students_identity
    ON students (LOWER(first_names), LOWER(last_names), birth_date);

CREATE INDEX idx_students_name ON students (last_names, first_names);

CREATE TABLE guardians (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT       NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    relationship    VARCHAR(20)  NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    national_id     VARCHAR(20),
    address         VARCHAR(500),
    birth_date      DATE,
    occupation      VARCHAR(150),
    works           BOOLEAN      NOT NULL DEFAULT FALSE,
    employer        VARCHAR(150),
    email           VARCHAR(255),
    home_phone      VARCHAR(30),
    mobile_phone    VARCHAR(30),
    office_phone    VARCHAR(30),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_guardians_student_relationship UNIQUE (student_id, relationship),
    CONSTRAINT ck_guardians_relationship CHECK (relationship IN ('MOTHER', 'FATHER'))
);

CREATE INDEX idx_guardians_student ON guardians (student_id);

CREATE TABLE enrollments (
    id                        BIGSERIAL PRIMARY KEY,
    student_id                BIGINT       NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    school_year               VARCHAR(9)   NOT NULL,
    shift                     VARCHAR(20)  NOT NULL,
    group_name                VARCHAR(20)  NOT NULL,
    enrollment_date           DATE         NOT NULL,
    previous_schooling        VARCHAR(30)  NOT NULL,
    previous_institution      VARCHAR(200),
    responsible_type          VARCHAR(20)  NOT NULL,
    responsible_other         VARCHAR(150),
    lives_with_mother         BOOLEAN      NOT NULL DEFAULT FALSE,
    lives_with_father         BOOLEAN      NOT NULL DEFAULT FALSE,
    pickup_type               VARCHAR(20)  NOT NULL,
    pickup_other              VARCHAR(150),
    illness                   VARCHAR(1000),
    allergies                 VARCHAR(1000),
    operated                  BOOLEAN      NOT NULL DEFAULT FALSE,
    operation_details         VARCHAR(500),
    psychological_treatment   BOOLEAN      NOT NULL DEFAULT FALSE,
    psychological_details     VARCHAR(500),
    photo_data_url            TEXT,
    commitment_accepted       BOOLEAN      NOT NULL,
    commitment_signature_name VARCHAR(150) NOT NULL,
    commitment_signature_date DATE         NOT NULL,
    commitment_signature_data TEXT,
    created_by                BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_enrollments_student_year UNIQUE (student_id, school_year),
    CONSTRAINT ck_enrollments_shift CHECK (shift IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT ck_enrollments_group CHECK (group_name IN ('MATERNAL', 'GROUP_I', 'GROUP_II', 'GROUP_III')),
    CONSTRAINT ck_enrollments_previous_schooling
        CHECK (previous_schooling IN ('HOME', 'SAME_SCHOOL', 'OTHER_SCHOOL', 'DAYCARE', 'OTHER')),
    CONSTRAINT ck_enrollments_responsible CHECK (responsible_type IN ('MOTHER', 'FATHER', 'OTHER')),
    CONSTRAINT ck_enrollments_pickup
        CHECK (pickup_type IN ('MOTHER', 'FATHER', 'BOTH', 'TRANSPORT', 'OTHER')),
    CONSTRAINT ck_enrollments_commitment CHECK (commitment_accepted)
);

CREATE INDEX idx_enrollments_school_year ON enrollments (school_year);
CREATE INDEX idx_enrollments_group ON enrollments (school_year, group_name);
CREATE INDEX idx_enrollments_created_at ON enrollments (created_at DESC);

CREATE TABLE enrollment_documents (
    id            BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT       NOT NULL REFERENCES enrollments (id) ON DELETE CASCADE,
    document_type VARCHAR(30)  NOT NULL,
    presented     BOOLEAN      NOT NULL DEFAULT FALSE,
    file_name     VARCHAR(255),
    storage_key   VARCHAR(255),
    mime_type     VARCHAR(100),
    size_bytes    BIGINT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_enrollment_documents_type UNIQUE (enrollment_id, document_type),
    CONSTRAINT ck_enrollment_documents_type CHECK (document_type IN (
        'BIRTH_CERTIFICATE',
        'VACCINE_CERTIFICATE',
        'MOTHER_ID',
        'FATHER_ID',
        'STUDENT_PHOTO'
    ))
);

CREATE INDEX idx_enrollment_documents_enrollment ON enrollment_documents (enrollment_id);
