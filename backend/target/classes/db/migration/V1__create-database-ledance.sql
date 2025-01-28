-- ========================================
-- CREACIÓN DE TABLAS BÁSICAS
-- ========================================

-- 1) ROLES
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       descripcion VARCHAR(50) NOT NULL UNIQUE,
                       activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2) USUARIOS
CREATE TABLE usuarios (
                          id SERIAL PRIMARY KEY,
                          nombre_usuario VARCHAR(100) NOT NULL,
                          email VARCHAR(100) NOT NULL UNIQUE,
                          contrasena VARCHAR(255) NOT NULL,
                          rol_id INTEGER REFERENCES roles(id) ON DELETE SET NULL,
                          activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3) PROFESORES
CREATE TABLE profesores (
                            id SERIAL PRIMARY KEY,
                            nombre VARCHAR(100) NOT NULL,
                            apellido VARCHAR(100) NOT NULL,
                            especialidad VARCHAR(100),
                            anios_experiencia INTEGER,
                            usuario_id INTEGER UNIQUE REFERENCES usuarios(id) ON DELETE SET NULL,
                            activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4) DISCIPLINAS
CREATE TABLE disciplinas (
                             id SERIAL PRIMARY KEY,
                             nombre VARCHAR(100) NOT NULL,
                             horario VARCHAR(100),
                             frecuencia_semanal INTEGER,
                             duracion VARCHAR(50),
                             salon VARCHAR(100),
                             valor_cuota NUMERIC(10, 2) NOT NULL,
                             matricula NUMERIC(10, 2) NOT NULL,
                             profesor_id INTEGER REFERENCES profesores(id) ON DELETE SET NULL,
                             activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 5) BONIFICACIONES
CREATE TABLE bonificaciones (
                                id SERIAL PRIMARY KEY,
                                descripcion VARCHAR(100) NOT NULL,     -- Ej.: "1/2 BECA"
                                porcentaje_descuento INTEGER NOT NULL, -- Ej.: 50%
                                activo BOOLEAN NOT NULL DEFAULT TRUE,
                                observaciones TEXT
);

-- 6) ALUMNOS
CREATE TABLE alumnos (
                         id SERIAL PRIMARY KEY,
                         nombre VARCHAR(100) NOT NULL,
                         apellido VARCHAR(100),
                         fecha_nacimiento DATE,
                         edad INTEGER,
                         celular1 VARCHAR(20),
                         celular2 VARCHAR(20),
                         telefono VARCHAR(20),
                         email1 VARCHAR(100),
                         email2 VARCHAR(100),
                         documento VARCHAR(50),
                         cuit VARCHAR(50),
                         fecha_incorporacion DATE NOT NULL,
                         nombre_padres VARCHAR(200),
                         autorizado_para_salir_solo BOOLEAN,
                         otras_notas TEXT,
                         cuota_total NUMERIC(10, 2),
                         activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- ========================================
-- ENTIDAD INTERMEDIA: INSCRIPCIONES
-- ========================================
-- Reemplaza las antiguas "alumno_disciplina" / "alumno_bonificacion".
-- Cada registro vincula UN alumno, UNA disciplina y (opcional) UNA bonificación.
CREATE TABLE inscripciones (
                               id SERIAL PRIMARY KEY,
                               alumno_id INTEGER NOT NULL REFERENCES alumnos(id) ON DELETE CASCADE,
                               disciplina_id INTEGER NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
                               bonificacion_id INTEGER REFERENCES bonificaciones(id) ON DELETE SET NULL,
                               costo_particular NUMERIC(10, 2),
                               notas TEXT
);

-- ========================================
-- TABLAS DE ASISTENCIAS, PAGOS, ETC.
-- ========================================

-- 7) ASISTENCIAS
CREATE TABLE asistencias (
                             id SERIAL PRIMARY KEY,
                             fecha DATE NOT NULL,
                             presente BOOLEAN NOT NULL,
                             observacion TEXT,
                             alumno_id INTEGER REFERENCES alumnos(id) ON DELETE CASCADE,
                             disciplina_id INTEGER REFERENCES disciplinas(id) ON DELETE CASCADE,
                             profesor_id INTEGER REFERENCES profesores(id) ON DELETE SET NULL,
                             activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8) METODO_PAGOS (nuevo, para reemplazar la antigua columna 'metodo_pago')
CREATE TABLE metodo_pagos (
                              id SERIAL PRIMARY KEY,
                              descripcion VARCHAR(50) NOT NULL,
                              activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 9) PAGOS
CREATE TABLE pagos (
                       id SERIAL PRIMARY KEY,
                       fecha DATE NOT NULL,
                       monto NUMERIC(10, 2) NOT NULL,
    -- Eliminado 'metodo_pago' CHECK; en su lugar, 'metodo_pago_id'
                       metodo_pago_id INT REFERENCES metodo_pagos(id),
                       alumno_id INTEGER REFERENCES alumnos(id) ON DELETE CASCADE,
                       recargo_aplicado BOOLEAN,
                       bonificacion_aplicada BOOLEAN,
                       activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 10) CAJA
CREATE TABLE caja (
                      id SERIAL PRIMARY KEY,
                      fecha DATE NOT NULL,
                      total_efectivo NUMERIC(10, 2) NOT NULL CHECK (total_efectivo >= 0),
                      total_transferencia NUMERIC(10, 2) NOT NULL CHECK (total_transferencia >= 0),
                      rango_desde_hasta VARCHAR(100),
                      observaciones TEXT,
                      activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 11) TIPO_PRODUCTOS
CREATE TABLE tipo_productos (
                                id SERIAL PRIMARY KEY,
                                descripcion VARCHAR(100) NOT NULL,
                                activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 12) PRODUCTOS
CREATE TABLE productos (
                           id SERIAL PRIMARY KEY,
                           nombre VARCHAR(100) NOT NULL,
                           precio NUMERIC(10, 2) NOT NULL,
                           tipo_producto_id INTEGER REFERENCES tipo_productos(id) ON DELETE SET NULL,
                           stock INTEGER NOT NULL CHECK (stock >= 0),
                           requiere_control_de_stock BOOLEAN NOT NULL DEFAULT TRUE,
                           activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 13) REPORTES
CREATE TABLE reportes (
                          id SERIAL PRIMARY KEY,
                          tipo VARCHAR(100) NOT NULL CHECK (tipo IN ('Recaudacion', 'Asistencia', 'Pagos', 'Otro')),
                          descripcion TEXT NOT NULL,
                          fecha_generacion DATE NOT NULL,
                          activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- ========================================
-- INSERT DE DATOS INICIALES O ÍNDICES
-- ========================================

-- Insertar rol "ADMINISTRADOR"
INSERT INTO roles (descripcion) VALUES ('ADMINISTRADOR');

-- Crear índices (para rendimiento)
CREATE INDEX idx_asistencias_fecha ON asistencias(fecha);
CREATE INDEX idx_asistencias_profesor ON asistencias(profesor_id);
CREATE INDEX idx_pagos_alumno ON pagos(alumno_id);
CREATE INDEX idx_productos_tipo ON productos(tipo_producto_id);
CREATE INDEX idx_caja_fecha ON caja(fecha);
CREATE INDEX idx_disciplinas_profesor ON disciplinas(profesor_id);
CREATE INDEX idx_profesores_usuario ON profesores(usuario_id);
CREATE INDEX idx_usuarios_rol ON usuarios(rol_id);

-- Índices adicionales para inscripciones, si lo deseas
CREATE INDEX idx_inscripciones_alumno ON inscripciones(alumno_id);
CREATE INDEX idx_inscripciones_disciplina ON inscripciones(disciplina_id);

-- Fin del script unificado
