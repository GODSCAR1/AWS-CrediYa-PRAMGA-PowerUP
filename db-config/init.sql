CREATE DATABASE IF NOT EXISTS solicitudes
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE solicitudes;

CREATE TABLE IF NOT EXISTS estados(
    id_estado   CHAR(36) DEFAULT (UUID()),
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_estado)
);

CREATE TABLE IF NOT EXISTS tipo_prestamo(
    id_tipo_prestamo        CHAR(36) DEFAULT (UUID()),
    nombre                  VARCHAR(120) NOT NULL UNIQUE,
    monto_minimo            DECIMAL(15,2) NOT NULL,
    monto_maximo            DECIMAL(15,2) NOT NULL,
    tasa_interes            DECIMAL(5,2) NOT NULL,
    validacion_automatica   BOOLEAN NOT NULL, 
    PRIMARY KEY (id_tipo_prestamo),
    CHECK(monto_minimo > 0),
    CHECK(monto_maximo > 0),
    CHECK(monto_maximo > monto_minimo),
    CHECK(tasa_interes > 0)
);

CREATE TABLE IF NOT EXISTS solicitud(
    id_solicitud        CHAR(36) DEFAULT (UUID()),
    monto               DECIMAL(15,2),
    plazo               INT NOT NULL,
    email               VARCHAR(160),
    id_estado           CHAR(36) NOT NULL,
    id_tipo_prestamo    CHAR(36) NOT NULL,
    CHECK (monto > 0),
    CHECK (plazo > 0),
    CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'),
    CONSTRAINT fk_solicitud_estado
        FOREIGN KEY (id_estado) REFERENCES estados(id_estado),
    CONSTRAINT fk_solicitud_tipo_prestamo
        FOREIGN KEY (id_tipo_prestamo) REFERENCES tipo_prestamo(id_tipo_prestamo)
);

INSERT INTO estados (nombre, descripcion) VALUES
('Pendiente de revision', 'Solicitud creada y pendiente de validaciones iniciales.'),
('Rechazada', 'Solicitud no aprobada según reglas automáticas o análisis.'),
('Revision manual', 'Requiere evaluación humana por alertas o umbrales.'),
('Aprobado', 'Solicitud aprobada; puede continuar a desembolso.');

INSERT INTO tipo_prestamo (nombre, monto_minimo, monto_maximo, tasa_interes, validacion_automatica) VALUES
('Microcredito',   200000.00,  3000000.00, 2.10,  TRUE),   
('Consumo',       1000000.00, 50000000.00, 1.60,  TRUE),   
('Hipotecario',  30000000.00,500000000.00, 0.95,  FALSE);  
