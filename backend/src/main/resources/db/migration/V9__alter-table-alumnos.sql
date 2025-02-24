CREATE TABLE matriculas
(
    id         SERIAL PRIMARY KEY,
    anio       INTEGER        NOT NULL,
    pagada     BOOLEAN        NOT NULL DEFAULT false,
    fecha_pago DATE,
    alumno_id  INTEGER        NOT NULL,
    valor      NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_alumno
        FOREIGN KEY (alumno_id) REFERENCES alumnos (id)
);
