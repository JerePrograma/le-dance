CREATE TABLE observaciones_profesores
(
    id          SERIAL PRIMARY KEY,
    profesor_id BIGINT NOT NULL,
    fecha       DATE   NOT NULL,
    observacion TEXT,
    CONSTRAINT fk_profesor
        FOREIGN KEY (profesor_id)
            REFERENCES profesores (id)
);
