CREATE TABLE iam_user
(
    uuid          VARCHAR(36)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL,
    created_by    VARCHAR(64)              NOT NULL,
    updated_by    VARCHAR(64)              NOT NULL,
    deleted       BOOLEAN                  NOT NULL DEFAULT FALSE,
    status        VARCHAR(32)              NOT NULL,
    name          VARCHAR(64)              NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (uuid)
);

CREATE INDEX iam_user_status_idx ON iam_user (status);

CREATE TABLE iam_identity
(
    uuid                VARCHAR(36)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT                   NOT NULL,
    created_by          VARCHAR(64)              NOT NULL,
    updated_by          VARCHAR(64)              NOT NULL,
    deleted             BOOLEAN                  NOT NULL DEFAULT FALSE,
    user_id             VARCHAR(36)              NOT NULL,
    identity_type       VARCHAR(32)              NOT NULL,
    identity            VARCHAR(320)             NOT NULL,
    normalized_identity VARCHAR(320)             NOT NULL,
    verified_at         TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (uuid),
    CONSTRAINT iam_identity_user_fk FOREIGN KEY (user_id) REFERENCES iam_user (uuid)
);

CREATE UNIQUE INDEX iam_identity_type_normalized_identity_uidx
    ON iam_identity (identity_type, normalized_identity)
    WHERE deleted = FALSE;

CREATE INDEX iam_identity_user_id_idx ON iam_identity (user_id);

CREATE TABLE iam_credential
(
    uuid            VARCHAR(36)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT                   NOT NULL,
    created_by      VARCHAR(64)              NOT NULL,
    updated_by      VARCHAR(64)              NOT NULL,
    deleted         BOOLEAN                  NOT NULL DEFAULT FALSE,
    user_id         VARCHAR(36)              NOT NULL,
    credential_type VARCHAR(32)              NOT NULL,
    secret          VARCHAR(255)             NOT NULL,
    PRIMARY KEY (uuid),
    CONSTRAINT iam_credential_user_fk FOREIGN KEY (user_id) REFERENCES iam_user (uuid)
);

CREATE UNIQUE INDEX iam_credential_user_id_credential_type_uidx
    ON iam_credential (user_id, credential_type)
    WHERE deleted = FALSE;

CREATE INDEX iam_credential_user_id_idx ON iam_credential (user_id);
