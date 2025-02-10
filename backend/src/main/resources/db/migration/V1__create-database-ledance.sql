-- ========================================
-- CREACIÓN DE TABLAS BÁSICAS
-- ========================================

-- 1) ROLES
CREATE TABLE roles
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(50) NOT NULL UNIQUE,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 2) USUARIOS
CREATE TABLE usuarios
(
    id             SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    contrasena     VARCHAR(255) NOT NULL,
    rol_id         INTEGER      REFERENCES roles (id) ON DELETE SET NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 3) TIPOS DE PRODUCTOS
CREATE TABLE tipo_productos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 4) PRODUCTOS
CREATE TABLE productos
(
    id                        SERIAL PRIMARY KEY,
    nombre                    VARCHAR(100)   NOT NULL,
    precio                    NUMERIC(10, 2) NOT NULL,
    tipo_producto_id          INTEGER        REFERENCES tipo_productos (id) ON DELETE SET NULL,
    stock                     INTEGER        NOT NULL CHECK (stock >= 0),
    requiere_control_de_stock BOOLEAN        NOT NULL DEFAULT TRUE,
    codigo_barras             VARCHAR(100),
    activo                    BOOLEAN        NOT NULL DEFAULT TRUE
);

-- 5) RECARGOS
CREATE TABLE recargos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL
);

-- 6) RECARGO DETALLES
CREATE TABLE recargo_detalles
(
    id         SERIAL PRIMARY KEY,
    recargo_id INTEGER       NOT NULL REFERENCES recargos (id) ON DELETE CASCADE,
    dia_desde  INTEGER       NOT NULL CHECK (dia_desde BETWEEN 1 AND 31),
    porcentaje NUMERIC(5, 2) NOT NULL CHECK (porcentaje >= 0)
);

-- 7) PROFESORES
CREATE TABLE profesores
(
    id               SERIAL PRIMARY KEY,
    nombre           VARCHAR(100)   NOT NULL,
    apellido         VARCHAR(100)   NOT NULL,
    especialidad     VARCHAR(100),
    fecha_nacimiento DATE,
    edad             INTEGER,
    telefono         VARCHAR(20),
    usuario_id       INTEGER UNIQUE REFERENCES usuarios (id) ON DELETE SET NULL,
    activo           BOOLEAN        NOT NULL DEFAULT TRUE
);

-- 8) SALONES
CREATE TABLE salones
(
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT
);

-- 9) DISCIPLINAS
CREATE TABLE disciplinas
(
    id                 SERIAL PRIMARY KEY,
    nombre             VARCHAR(100)   NOT NULL,
    horario_inicio     TIME           NOT NULL,
    duracion           NUMERIC(5, 2)  NOT NULL,
    frecuencia_semanal INTEGER,
    profesor_id        INTEGER        REFERENCES profesores (id) ON DELETE SET NULL,
    salon_id           INTEGER        REFERENCES salones (id) ON DELETE SET NULL,
    recargo_id         INTEGER        REFERENCES recargos (id) ON DELETE SET NULL,
    valor_cuota        NUMERIC(10, 2) NOT NULL,
    matricula          NUMERIC(10, 2) NOT NULL,
    clase_suelta       NUMERIC(10, 2),
    clase_prueba       NUMERIC(10, 2),
    activo             BOOLEAN        NOT NULL DEFAULT TRUE
);

-- 10) BONIFICACIONES
CREATE TABLE bonificaciones
(
    id                   SERIAL PRIMARY KEY,
    descripcion          VARCHAR(100) NOT NULL,
    porcentaje_descuento INTEGER      NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    observaciones        TEXT
);

-- 11) ALUMNOS
CREATE TABLE alumnos
(
    id                         SERIAL PRIMARY KEY,
    nombre                     VARCHAR(100) NOT NULL,
    apellido                   VARCHAR(100),
    fecha_nacimiento           DATE,
    edad                       INTEGER,
    celular1                   VARCHAR(20),
    celular2                   VARCHAR(20),
    email1                     VARCHAR(100),
    email2                     VARCHAR(100),
    documento                  VARCHAR(50),
    cuit                       VARCHAR(50),
    fecha_incorporacion        DATE         NOT NULL,
    fecha_de_baja              DATE,
    deuda_pendiente            BOOLEAN               DEFAULT FALSE,
    nombre_padres              VARCHAR(200),
    autorizado_para_salir_solo BOOLEAN,
    otras_notas                TEXT,
    cuota_total                NUMERIC(10, 2),
    activo                     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 12) INSCRIPCIONES
CREATE TABLE inscripciones
(
    id                SERIAL PRIMARY KEY,
    alumno_id         INTEGER     NOT NULL REFERENCES alumnos (id) ON DELETE CASCADE,
    disciplina_id     INTEGER     NOT NULL REFERENCES disciplinas (id) ON DELETE CASCADE,
    bonificacion_id   INTEGER     REFERENCES bonificaciones (id) ON DELETE SET NULL,
    fecha_inscripcion DATE        NOT NULL,
    fecha_baja        DATE,
    estado            VARCHAR(20) NOT NULL CHECK (estado IN ('ACTIVA', 'INACTIVA', 'FINALIZADA')),
    notas             TEXT
);

-- 13) ASISTENCIAS MENSUALES
CREATE TABLE asistencias_mensuales
(
    id             SERIAL PRIMARY KEY,
    mes            INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    anio           INTEGER NOT NULL CHECK (anio >= 2000),
    inscripcion_id INTEGER NOT NULL REFERENCES inscripciones (id) ON DELETE CASCADE
    -- El campo 'observaciones' ha sido eliminado
);

-- Nueva tabla: OBSERVACIONES MENSUALES
CREATE TABLE observaciones_mensuales
(
    id                    SERIAL PRIMARY KEY,
    asistencia_mensual_id INTEGER NOT NULL,
    alumno_id             INTEGER NOT NULL,
    observacion           TEXT,
    FOREIGN KEY (asistencia_mensual_id) REFERENCES asistencias_mensuales (id) ON DELETE CASCADE,
    FOREIGN KEY (alumno_id) REFERENCES alumnos (id) ON DELETE CASCADE
);

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_observaciones_mensuales_asistencia_mensual_id ON observaciones_mensuales (asistencia_mensual_id);
CREATE INDEX idx_observaciones_mensuales_alumno_id ON observaciones_mensuales (alumno_id);

-- 14) ASISTENCIAS DIARIAS
CREATE TABLE asistencias_diarias
(
    id                    SERIAL PRIMARY KEY,
    fecha                 DATE        NOT NULL,
    estado                VARCHAR(10) NOT NULL CHECK (estado IN ('PRESENTE', 'AUSENTE')),
    alumno_id             INTEGER     NOT NULL REFERENCES alumnos (id) ON DELETE CASCADE,
    asistencia_mensual_id INTEGER     NOT NULL REFERENCES asistencias_mensuales (id) ON DELETE CASCADE,
    observacion           VARCHAR(255)
);

-- 15) MÉTODOS DE PAGO
CREATE TABLE metodo_pagos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(50) NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE
);

-- 16) PAGOS
CREATE TABLE pagos
(
    id                    SERIAL PRIMARY KEY,
    fecha                 DATE           NOT NULL,
    monto                 NUMERIC(10, 2) NOT NULL CHECK (monto >= 0),
    fecha_vencimiento     DATE           NOT NULL,
    inscripcion_id        INTEGER        NOT NULL REFERENCES inscripciones (id) ON DELETE CASCADE,
    metodo_pago_id        INTEGER REFERENCES metodo_pagos (id),
    recargo_aplicado      BOOLEAN,
    bonificacion_aplicada BOOLEAN,
    saldo_restante        NUMERIC(10, 2) CHECK (saldo_restante >= 0),
    activo                BOOLEAN        NOT NULL DEFAULT TRUE
);

-- 17) CAJA
CREATE TABLE caja
(
    id                  SERIAL PRIMARY KEY,
    fecha               DATE           NOT NULL,
    total_efectivo      NUMERIC(10, 2) NOT NULL CHECK (total_efectivo >= 0),
    total_transferencia NUMERIC(10, 2) NOT NULL CHECK (total_transferencia >= 0),
    total_tarjeta       NUMERIC(10, 2) NOT NULL CHECK (total_tarjeta >= 0),
    rango_desde_hasta   VARCHAR(100),
    observaciones       TEXT,
    activo              BOOLEAN        NOT NULL DEFAULT TRUE
);

-- 18) REPORTES
CREATE TABLE reportes
(
    id               SERIAL PRIMARY KEY,
    tipo             VARCHAR(100) NOT NULL CHECK (tipo IN ('Recaudacion', 'Asistencia', 'Pagos', 'Otro')),
    descripcion      TEXT         NOT NULL,
    fecha_generacion DATE         NOT NULL,
    usuario_id       INTEGER      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE disciplina_dias
(
    disciplina_id BIGINT      NOT NULL,
    dia           VARCHAR(20) NOT NULL CHECK (dia IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO')),
    PRIMARY KEY (disciplina_id, dia),
    FOREIGN KEY (disciplina_id) REFERENCES disciplinas (id) ON DELETE CASCADE
);

CREATE INDEX idx_asistencias_diarias_fecha_alumno ON asistencias_diarias (fecha, alumno_id);
CREATE INDEX idx_asistencias_mensuales_mes_anio ON asistencias_mensuales (mes, anio);
CREATE INDEX idx_disciplinas_dias ON disciplina_dias (dia);

CREATE INDEX idx_inscripciones_alumno_id ON inscripciones (alumno_id);
CREATE INDEX idx_asistencias_diarias_fecha_disciplina ON asistencias_diarias (fecha, asistencia_mensual_id);
CREATE INDEX idx_asistencias_mensuales_inscripcion ON asistencias_mensuales (inscripcion_id);
CREATE INDEX idx_pagos_inscripcion_id ON pagos (inscripcion_id);
CREATE INDEX idx_reportes_usuario_id ON reportes (usuario_id);
