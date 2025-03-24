-- ========================================
-- Script Unificado de Creacion Final de la Base de Datos
-- ========================================

-- 1. Crear tipo enumerado (aunque ya no se use en la tabla stocks, no se solicito eliminarlo)
CREATE TYPE tipo_egreso AS ENUM ('VENTA', 'DEVOLUCION');

-- ========================================
-- CREACION DE TABLAS (VERSION FINAL)
-- ========================================

-- Tabla: roles
CREATE TABLE roles
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(50) NOT NULL UNIQUE,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE
);

INSERT INTO roles (descripcion, activo) VALUES ('ADMINISTRADOR', TRUE);

-- Tabla: usuarios
CREATE TABLE usuarios
(
    id             SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(100) NOT NULL UNIQUE,
    contrasena     VARCHAR(255) NOT NULL,
    rol_id         INTEGER REFERENCES roles (id) ON DELETE RESTRICT,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tabla: tipo_stocks
CREATE TABLE tipo_stocks
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tabla: stocks (sin la columna "tipo_egreso", y con "fecha_ingreso"/"fecha_egreso" renombradas)
CREATE TABLE stocks
(
    id                        SERIAL PRIMARY KEY,
    nombre                    VARCHAR(100)   NOT NULL,
    precio                    NUMERIC(10, 2) NOT NULL,
    tipo_stocks_id            INTEGER        REFERENCES tipo_stocks (id) ON DELETE SET NULL,
    stock                     INTEGER        NOT NULL CHECK (stock >= 0),
    requiere_control_de_stock BOOLEAN        NOT NULL DEFAULT TRUE,
    codigo_barras             VARCHAR(100),
    activo                    BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_ingreso             DATE,
    fecha_egreso              DATE
);

-- Tabla: recargos
CREATE TABLE recargos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL
);

-- Tabla: recargo_detalles
CREATE TABLE recargo_detalles
(
    id         SERIAL PRIMARY KEY,
    recargo_id INTEGER       NOT NULL REFERENCES recargos (id) ON DELETE CASCADE,
    dia_desde  INTEGER       NOT NULL CHECK (dia_desde BETWEEN 1 AND 31),
    porcentaje NUMERIC(5, 2) NOT NULL CHECK (porcentaje >= 0),
    valor_fijo NUMERIC(10, 2)
);

-- Tabla: profesores
CREATE TABLE profesores
(
    id               SERIAL PRIMARY KEY,
    nombre           VARCHAR(100)   NOT NULL,
    apellido         VARCHAR(100)   NOT NULL,
    fecha_nacimiento DATE,
    edad             INTEGER,
    telefono         VARCHAR(20),
    usuario_id       INTEGER UNIQUE REFERENCES usuarios (id) ON DELETE SET NULL,
    activo           BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Tabla: salones
CREATE TABLE salones
(
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT
);

-- Tabla: disciplinas (sin columnas recargo_id ni matricula)
CREATE TABLE disciplinas
(
    id                 SERIAL PRIMARY KEY,
    nombre             VARCHAR(100)   NOT NULL,
    horario_inicio     TIME           NOT NULL,
    duracion           NUMERIC(5, 2)  NOT NULL,
    frecuencia_semanal INTEGER,
    profesor_id        INTEGER        REFERENCES profesores (id) ON DELETE SET NULL,
    salon_id           INTEGER        REFERENCES salones (id) ON DELETE SET NULL,
    valor_cuota        NUMERIC(10, 2) NOT NULL,
    clase_suelta       NUMERIC(10, 2),
    clase_prueba       NUMERIC(10, 2),
    activo             BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Tabla: bonificaciones (agregada columna valor_fijo)
CREATE TABLE bonificaciones
(
    id                   SERIAL PRIMARY KEY,
    descripcion          VARCHAR(100) NOT NULL,
    porcentaje_descuento INTEGER      NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    observaciones        TEXT,
    valor_fijo           NUMERIC(10, 2)
);

-- Tabla: alumnos
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

-- Tabla: inscripciones
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

-- Tabla: asistencias_mensuales
CREATE TABLE asistencias_mensuales
(
    id             SERIAL PRIMARY KEY,
    mes            INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    anio           INTEGER NOT NULL CHECK (anio >= 2000),
    inscripcion_id INTEGER NOT NULL REFERENCES inscripciones (id) ON DELETE CASCADE
);

-- Tabla: observaciones_mensuales
CREATE TABLE observaciones_mensuales
(
    id                    SERIAL PRIMARY KEY,
    asistencia_mensual_id INTEGER NOT NULL REFERENCES asistencias_mensuales (id) ON DELETE CASCADE,
    alumno_id             INTEGER NOT NULL REFERENCES alumnos (id) ON DELETE CASCADE,
    observacion           TEXT
);

CREATE INDEX idx_observaciones_mensuales_asistencia_mensual_id
    ON observaciones_mensuales (asistencia_mensual_id);

CREATE INDEX idx_observaciones_mensuales_alumno_id
    ON observaciones_mensuales (alumno_id);

-- Tabla: asistencias_diarias
CREATE TABLE asistencias_diarias
(
    id                    SERIAL PRIMARY KEY,
    fecha                 DATE        NOT NULL,
    estado                VARCHAR(10) NOT NULL CHECK (estado IN ('PRESENTE', 'AUSENTE')),
    alumno_id             INTEGER     NOT NULL REFERENCES alumnos (id) ON DELETE CASCADE,
    asistencia_mensual_id INTEGER     NOT NULL REFERENCES asistencias_mensuales (id) ON DELETE CASCADE,
    observacion           VARCHAR(255)
);

CREATE INDEX idx_asistencias_diarias_fecha_alumno
    ON asistencias_diarias (fecha, alumno_id);

CREATE INDEX idx_asistencias_diarias_fecha_disciplina
    ON asistencias_diarias (fecha, asistencia_mensual_id);

-- Tabla: metodo_pagos (con columna "recargo" agregada)
CREATE TABLE metodo_pagos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(50)    NOT NULL,
    activo      BOOLEAN        NOT NULL DEFAULT TRUE,
    recargo     NUMERIC(10, 2) NOT NULL DEFAULT 0
);

-- Tabla: pagos (incluyendo alumno_id, saldo_a_favor y bonificacion_aplicada como boolean)
CREATE TABLE pagos
(
    id                    SERIAL PRIMARY KEY,
    fecha                 DATE           NOT NULL,
    monto                 NUMERIC(10, 2) NOT NULL CHECK (monto >= 0),
    fecha_vencimiento     DATE           NOT NULL,
    inscripcion_id        INTEGER        NOT NULL REFERENCES inscripciones (id) ON DELETE CASCADE,
    metodo_pago_id        INTEGER REFERENCES metodo_pagos (id),
    recargo_aplicado      BOOLEAN,
    bonificacion_aplicada BOOLEAN        NOT NULL DEFAULT false,
    saldo_restante        NUMERIC(10, 2) CHECK (saldo_restante >= 0),
    activo                BOOLEAN        NOT NULL DEFAULT TRUE,
    observaciones         TEXT,
    saldo_a_favor         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    alumno_id             INTEGER        NOT NULL,
    CONSTRAINT fk_pagos_alumno
        FOREIGN KEY (alumno_id) REFERENCES alumnos (id) ON DELETE CASCADE
);

CREATE INDEX idx_pagos_inscripcion_id
    ON pagos (inscripcion_id);

-- Tabla: caja
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

-- Tabla: reportes
CREATE TABLE reportes
(
    id               SERIAL PRIMARY KEY,
    tipo             VARCHAR(100) NOT NULL CHECK (tipo IN ('Recaudacion', 'Asistencia', 'Pagos', 'Otro')),
    descripcion      TEXT         NOT NULL,
    fecha_generacion DATE         NOT NULL,
    usuario_id       INTEGER      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_reportes_usuario_id
    ON reportes (usuario_id);

-- Tabla: disciplina_dias
CREATE TABLE disciplina_dias
(
    disciplina_id BIGINT      NOT NULL,
    dia           VARCHAR(20) NOT NULL CHECK (dia IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO')),
    PRIMARY KEY (disciplina_id, dia),
    FOREIGN KEY (disciplina_id) REFERENCES disciplinas (id) ON DELETE CASCADE
);

CREATE INDEX idx_disciplinas_dias
    ON disciplina_dias (dia);

-- Tabla: detalle_pagos (sin la columna "recargo" numerica, con "recargo_id" y "abono")
CREATE TABLE detalle_pagos
(
    id              SERIAL PRIMARY KEY,
    codigo_concepto VARCHAR(50),
    concepto        VARCHAR(100)   NOT NULL,
    cuota           VARCHAR(20),
    valor_base      NUMERIC(10, 2) NOT NULL CHECK (valor_base >= 0),
    a_favor         NUMERIC(10, 2) DEFAULT 0 CHECK (a_favor >= 0),
    importe         NUMERIC(10, 2) CHECK (importe >= 0),
    a_cobrar        NUMERIC(10, 2) CHECK (a_cobrar >= 0),
    pago_id         INTEGER        NOT NULL REFERENCES pagos (id) ON DELETE CASCADE,
    bonificacion_id BIGINT REFERENCES bonificaciones (id),
    recargo_id      INTEGER        REFERENCES recargos (id) ON DELETE SET NULL,
    abono           NUMERIC(10, 2)
);

CREATE INDEX idx_detalle_pagos_pago_id
    ON detalle_pagos (pago_id);

-- Tabla: pago_medios
CREATE TABLE pago_medios
(
    id             SERIAL PRIMARY KEY,
    monto          NUMERIC(10, 2) NOT NULL CHECK (monto >= 0),
    metodo_pago_id INTEGER        NOT NULL REFERENCES metodo_pagos (id) ON DELETE CASCADE,
    pago_id        INTEGER        NOT NULL REFERENCES pagos (id) ON DELETE CASCADE
);

CREATE INDEX idx_pago_medios_pago_id
    ON pago_medios (pago_id);

CREATE INDEX idx_pago_medios_metodo_pago_id
    ON pago_medios (metodo_pago_id);

-- Tabla: sub_conceptos
CREATE TABLE sub_conceptos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(255) NOT NULL
);

-- Tabla: conceptos
CREATE TABLE conceptos
(
    id              SERIAL PRIMARY KEY,
    descripcion     VARCHAR(255)   NOT NULL,
    precio          NUMERIC(10, 2) NOT NULL,
    sub_concepto_id INTEGER        NOT NULL,
    CONSTRAINT fk_sub_concepto
        FOREIGN KEY (sub_concepto_id)
            REFERENCES sub_conceptos (id) ON DELETE RESTRICT
);

-- Tabla: matriculas (ya sin la columna "valor")
CREATE TABLE matriculas
(
    id         SERIAL PRIMARY KEY,
    anio       INTEGER NOT NULL,
    pagada     BOOLEAN NOT NULL DEFAULT false,
    fecha_pago DATE,
    alumno_id  INTEGER NOT NULL REFERENCES alumnos (id)
);

-- Tabla: mensualidades
CREATE TABLE mensualidades
(
    id             SERIAL PRIMARY KEY,
    fecha_cuota    DATE           NOT NULL,
    valor_base     NUMERIC(10, 2) NOT NULL,
    recargo        NUMERIC(10, 2),
    bonificacion   NUMERIC(10, 2),
    estado         VARCHAR(50)    NOT NULL,
    inscripcion_id BIGINT         NOT NULL REFERENCES inscripciones (id) ON DELETE CASCADE
);

-- Tabla: egresos (nueva)
CREATE TABLE egresos
(
    id             BIGSERIAL PRIMARY KEY,
    fecha          DATE           NOT NULL,
    monto          NUMERIC(12, 2) NOT NULL,
    observaciones  VARCHAR(255),
    metodo_pago_id BIGINT         REFERENCES metodo_pagos (id) ON UPDATE CASCADE ON DELETE SET NULL,
    activo         BOOLEAN        NOT NULL DEFAULT TRUE
);

-- ========================================
-- FIN DEL SCRIPT UNIFICADO
-- ========================================
