-- 1. Crear el tipo enum
CREATE TYPE estado_pago AS ENUM ('ACTIVO', 'HISTORICO', 'ANULADO');

-- 2. Renombrar la columna 'activo' a 'estado_pago'
ALTER TABLE pagos
    RENAME COLUMN activo TO estado_pago;

-- 3. Eliminar el valor por omisión existente (default)
ALTER TABLE pagos
    ALTER COLUMN estado_pago DROP DEFAULT;

-- 4. Convertir la columna al tipo enum usando un cast explícito
ALTER TABLE pagos
    ALTER COLUMN estado_pago TYPE estado_pago
        USING (
        CASE
            WHEN estado_pago THEN 'ACTIVO'::estado_pago
            ELSE 'HISTORICO'::estado_pago
            END
        );

-- 5. Establecer el valor por defecto correcto
ALTER TABLE pagos
    ALTER COLUMN estado_pago SET DEFAULT 'ACTIVO'::estado_pago;