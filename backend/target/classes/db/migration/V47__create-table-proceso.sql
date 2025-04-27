CREATE TABLE procesos_ejecutados
(
    id               BIGSERIAL PRIMARY KEY,
    proceso          VARCHAR(255) NOT NULL,
    ultima_ejecucion DATE
);
