-- Primero, eliminamos las tablas si existen (en orden inverso para evitar errores de dependencia)
DROP TABLE IF EXISTS detalle_pagos CASCADE;
DROP TABLE IF EXISTS pagos CASCADE;

-- Creamos la tabla "pagos"
CREATE TABLE pagos
(
    id                    SERIAL PRIMARY KEY,
    fecha                 DATE           NOT NULL,
    fecha_vencimiento     DATE           NOT NULL,
    monto                 NUMERIC(12, 2) NOT NULL CHECK (monto >= 0),
    monto_base_pago       NUMERIC(12, 2) NOT NULL CHECK (monto_base_pago >= 0),
    alumno_id             INTEGER        NOT NULL,
    inscripcion_id        INTEGER,
    metodo_pago_id        INTEGER,
    recargo_aplicado      BOOLEAN        NOT NULL DEFAULT FALSE,
    bonificacion_aplicada BOOLEAN        NOT NULL DEFAULT FALSE,
    saldo_restante        NUMERIC(12, 2) NOT NULL,
    saldo_a_favor         NUMERIC(12, 2) NOT NULL DEFAULT 0.0 CHECK (saldo_a_favor >= 0),
    estado_pago           VARCHAR(20)    NOT NULL,
    observaciones         TEXT,
    tipo_pago             VARCHAR(20)    NOT NULL,
    monto_pagado          NUMERIC(12, 2) NOT NULL DEFAULT 0.0 CHECK (monto_pagado >= 0),
    CONSTRAINT fk_pago_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumnos (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pago_inscripcion FOREIGN KEY (inscripcion_id)
        REFERENCES inscripciones (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_metodopago FOREIGN KEY (metodo_pago_id)
        REFERENCES metodo_pagos (id) ON UPDATE CASCADE ON DELETE SET NULL
);

-- Creamos la tabla "detalle_pagos"
CREATE TABLE detalle_pagos
(
    id                   SERIAL PRIMARY KEY,
    descripcion_concepto TEXT,
    concepto_id          INTEGER,
    subconcepto_id       INTEGER,
    cuota                VARCHAR(10),
    bonificacion_id      INTEGER,
    recargo_id           INTEGER,
    a_favor              NUMERIC(12, 2)          DEFAULT 0.0,
    monto_original       NUMERIC(12, 2) NOT NULL,
    importe_inicial      NUMERIC(12, 2),
    importe_pendiente    NUMERIC(12, 2),
    a_cobrar             NUMERIC(12, 2),
    pago_id              INTEGER        NOT NULL,
    mensualidad_id       INTEGER,
    matricula_id         INTEGER,
    stock_id             INTEGER,
    alumno_id            INTEGER        NOT NULL,
    cobrado              BOOLEAN        NOT NULL DEFAULT FALSE,
    tipo                 VARCHAR(20)    NOT NULL,
    fecha_registro       DATE           NOT NULL,
    CONSTRAINT fk_detalle_pago_pago FOREIGN KEY (pago_id)
        REFERENCES pagos (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_detalle_pago_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumnos (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_detalle_pago_concepto FOREIGN KEY (concepto_id)
        REFERENCES conceptos (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_detalle_pago_subconcepto FOREIGN KEY (subconcepto_id)
        REFERENCES sub_conceptos (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_detalle_pago_bonificacion FOREIGN KEY (bonificacion_id)
        REFERENCES bonificaciones (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_detalle_pago_recargo FOREIGN KEY (recargo_id)
        REFERENCES recargos (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_detalle_pago_mensualidad FOREIGN KEY (mensualidad_id)
        REFERENCES mensualidades (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_detalle_pago_matricula FOREIGN KEY (matricula_id)
        REFERENCES matriculas (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_detalle_pago_stock FOREIGN KEY (stock_id)
        REFERENCES stocks (id) ON UPDATE CASCADE ON DELETE SET NULL
);
