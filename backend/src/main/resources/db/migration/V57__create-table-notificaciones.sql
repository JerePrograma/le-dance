-- 1. Crear la tabla notificaciones
CREATE TABLE notificaciones
(
    id         SERIAL PRIMARY KEY,
    usuario_id INTEGER     NOT NULL,                   -- Id del usuario destinatario; ajusta si usas otro tipo o quieres FK.
    tipo       VARCHAR(50) NOT NULL,                   -- Tipo de notificación, por ejemplo: 'CUMPLEANOS', 'ALERTA', etc.
    mensaje    TEXT        NOT NULL,                   -- Contenido o descripción de la notificación.
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- Fecha y hora de creación.
    leida      BOOLEAN     NOT NULL DEFAULT false      -- Indica si la notificación ya fue leída.
);

-- (Opcional) Si tienes una tabla de usuarios, puedes agregar una restricción FOREIGN KEY:
-- ALTER TABLE notificaciones
--     ADD CONSTRAINT fk_usuario
--     FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

-- 2. Modificar la tabla procesos_ejecutados para eliminar la columna resultado
ALTER TABLE procesos_ejecutados
    DROP COLUMN IF EXISTS resultado;
