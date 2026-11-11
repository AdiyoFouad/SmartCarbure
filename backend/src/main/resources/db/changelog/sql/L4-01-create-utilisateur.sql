CREATE TABLE utilisateur (
    id                BIGSERIAL PRIMARY KEY,
    nom               VARCHAR(100) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    mot_de_passe      VARCHAR(100) NOT NULL,
    carburant_prefere VARCHAR(10),
    rayon_prefere     INTEGER,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_utilisateur_email UNIQUE (email),
    CONSTRAINT ck_utilisateur_rayon CHECK (rayon_prefere IS NULL OR rayon_prefere BETWEEN 1 AND 50),
    CONSTRAINT ck_utilisateur_carburant CHECK (carburant_prefere IS NULL OR carburant_prefere IN ('GAZOLE', 'SP95', 'SP98', 'E10', 'E85', 'GPLC'))
);