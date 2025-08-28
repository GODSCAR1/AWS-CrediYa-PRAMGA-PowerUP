CREATE DATABASE IF NOT EXISTS autenticacion
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE autenticacion;

CREATE TABLE IF NOT EXISTS rol(
    id_rol      CHAR(36) DEFAULT (UUID()),
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_rol)
);

CREATE TABLE IF NOT EXISTS usuario(
    id_usuario          CHAR(36) DEFAULT (UUID()),
    nombre              VARCHAR(120) NOT NULL,
    apellido            VARCHAR(120) NOT NULL,
    email               VARCHAR(160) NOT NULL UNIQUE,
    contrasena          VARCHAR(255) NOT NULL,
    documento_identidad VARCHAR(15) NOT NULL UNIQUE,
    telefono            VARCHAR(15),
    id_rol              CHAR(36) NOT NULL,
    salario_base        DECIMAL(15,2) NOT NULL,
    fecha_nacimiento    DATE,
    CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'),
    CHECK (documento_identidad REGEXP '^[0-9]+$'),
    CHECK (salario_base >= 0),
    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

INSERT INTO rol(nombre, descripcion)
VALUES
    ('Administrador', 'Administrador del sistema.'),
    ('Asesor', 'Asesor comercial.'),
    ('Solicitante', 'Cliente potencial.')