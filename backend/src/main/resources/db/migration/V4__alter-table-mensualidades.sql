-- 1. Eliminar las columnas `recargo` y `bonificacion` de la tabla `mensualidades`
ALTER TABLE mensualidades
    DROP COLUMN recargo;
ALTER TABLE mensualidades
    DROP COLUMN bonificacion;

-- 2. Agregar claves foráneas a `Recargo` y `Bonificacion`
ALTER TABLE mensualidades
    ADD COLUMN recargo_id INT NULL;
ALTER TABLE mensualidades
    ADD COLUMN bonificacion_id INT NULL;

-- 3. Agregar las claves foráneas a las tablas correspondientes
ALTER TABLE mensualidades
    ADD CONSTRAINT fk_mensualidad_recargo
        FOREIGN KEY (recargo_id) REFERENCES recargos (id) ON DELETE SET NULL;

ALTER TABLE mensualidades
    ADD CONSTRAINT fk_mensualidad_bonificacion
        FOREIGN KEY (bonificacion_id) REFERENCES bonificaciones (id) ON DELETE SET NULL;
