-- ==========================================
-- 🔄 Flyway Migration V2: Actualización de Recargos
-- ==========================================

-- 🚨 1. Modificar la tabla `recargos`
ALTER TABLE recargos
    -- 🛑 Eliminar la columna `fecha_aplicacion`
    DROP COLUMN IF EXISTS fecha_aplicacion,

    -- ✅ Agregar `dia_del_mes_aplicacion`
    ADD COLUMN dia_del_mes_aplicacion INTEGER       NOT NULL CHECK (dia_del_mes_aplicacion BETWEEN 1 AND 31),

    -- ✅ Agregar `porcentaje` (si no existía)
    ADD COLUMN porcentaje             NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (porcentaje >= 0),

    -- ✅ Agregar `valor_fijo` (si no existía)
    ADD COLUMN valor_fijo             NUMERIC(10, 2) DEFAULT 0 CHECK (valor_fijo >= 0);

-- ✅ 2. Actualizar datos existentes (opcional, si necesitas mantener valores previos)
UPDATE recargos
SET dia_del_mes_aplicacion = 1 -- ⚠ Establecer un valor por defecto
WHERE dia_del_mes_aplicacion IS NULL;

-- ==========================================
-- ✅ Fin del Script - Recargos Actualizados
-- ==========================================
