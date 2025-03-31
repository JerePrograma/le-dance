-- Para la tabla 'pagos'
ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS usuario_id BIGINT NULL DEFAULT 1;
ALTER TABLE pagos
    DROP CONSTRAINT IF EXISTS fk_pago_usuario;
ALTER TABLE pagos
    ADD CONSTRAINT fk_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

-- Para la tabla 'detalle_pagos'
ALTER TABLE detalle_pagos
    ADD COLUMN IF NOT EXISTS usuario_id BIGINT NULL DEFAULT 1;
ALTER TABLE detalle_pagos
    DROP CONSTRAINT IF EXISTS fk_detalle_pago_usuario;
ALTER TABLE detalle_pagos
    ADD CONSTRAINT fk_detalle_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
