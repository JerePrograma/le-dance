CREATE TABLE disciplina_horarios
(
    id             SERIAL PRIMARY KEY,
    disciplina_id  BIGINT           NOT NULL REFERENCES disciplinas (id) ON DELETE CASCADE,
    dia            VARCHAR(20)      NOT NULL,
    horario_inicio TIME             NOT NULL,
    duracion       DOUBLE PRECISION NOT NULL
);
