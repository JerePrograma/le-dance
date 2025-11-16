# Gym & Academy Manager

Sistema de gestión de membresías, inscripciones, facturación y caja para gimnasios, academias y espacios de formación.

Permite administrar clientes (alumnos/socios), controlar las cuotas mensuales, emitir comprobantes y llevar el control de caja diaria.

---

## Tabla de contenidos

- [Descripción general](#descripción-general)
- [Características principales](#características-principales)
- [Modelo de dominio](#modelo-de-dominio)
- [Arquitectura sugerida](#arquitectura-sugerida)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos previos](#requisitos-previos)
- [Configuración y ejecución](#configuración-y-ejecución)
  - [Backend](#backend)
  - [Frontend (opcional)](#frontend-opcional)
- [Casos de uso principales](#casos-de-uso-principales)
- [Roadmap](#roadmap)
- [Licencia](#licencia)

---

## Descripción general

**Gym & Academy Manager** es un sistema pensado para pequeños y medianos negocios que trabajan con clientes recurrentes y cuotas mensuales, como:

- Gimnasios
- Academias de danza, música, idiomas
- Centros deportivos y culturales

El foco está puesto en:

- Tener control claro de **quién está inscripto a qué disciplina/plan**.
- Saber **quién está al día y quién adeuda cuotas**.
- Mantener un **registro simple de caja** (ingresos/egresos).
- Contar con una **base para reportes** de facturación y morosidad.

---

## Características principales

- **Gestión de clientes (alumnos/socios)**
  - Alta, baja lógica y modificación de datos básicos.
  - Estado del cliente (activo/inactivo).

- **Gestión de disciplinas / planes**
  - Definición de disciplinas (ej: CrossFit, Yoga, Guitarra, Inglés).
  - Configuración del valor de cuota vigente por disciplina/plan.
  - Historial de valores de cuota (no se pisan montos históricos).

- **Inscripciones**
  - Alta de inscripciones Cliente + Disciplina/Plan.
  - Fecha de alta, fecha de baja y estado de la inscripción.
  - Política de baja lógica para mantener historial.

- **Mensualidades**
  - Generación de mensualidades por período (ej: mes/año).
  - Asociación de cada mensualidad a una inscripción y al valor de cuota vigente.
  - Registro de estado: pendiente, pagado, vencido.

- **Facturación y cobranzas**
  - Registro de pagos de mensualidades.
  - Emisión de comprobantes simples (recibos o facturas, según alcance del proyecto).
  - Búsqueda por cliente, período, disciplina, estado de pago.

- **Control de caja**
  - Movimientos de caja: ingresos (pagos de cuotas) y egresos (gastos).
  - Saldo diario / por rango de fechas.
  - Referencia a la fuente del movimiento (ej: cuota, gasto operativo).

- **Seguridad y usuarios**
  - Usuarios del sistema (administradores, recepción, etc.).
  - Roles y permisos básicos (lectura/escritura por módulo).

---

## Modelo de dominio

Modelo sugerido (ajustable):

- **Cliente**
  - Datos personales (nombre, contacto).
  - Estado (activo/inactivo).
  - Relación con inscripciones, mensualidades y pagos.

- **Disciplina**
  - Nombre, descripción.
  - Cupo opcional.
  - Valor de cuota vigente (relación con `ValorCuota`).

- **ValorCuota**
  - Disciplina.
  - Monto.
  - Vigencia: `fechaDesde` y `fechaHasta` (nullable para “vigente”).

- **Inscripcion**
  - Cliente.
  - Disciplina.
  - Fecha de alta, fecha de baja (nullable).
  - Estado (activa/inactiva).
  - Baja lógica (no se borra de la base).

- **Mensualidad**
  - Inscripción.
  - Período (mes/año).
  - Monto (copiado desde `ValorCuota` al momento de generar).
  - Estado de pago.

- **Pago**
  - Mensualidad.
  - Fecha y forma de pago.
  - Importe pagado.

- **MovimientoCaja**
  - Tipo (ingreso/egreso).
  - Fecha, descripción.
  - Importe.
  - Referencia opcional a `Pago` u otro concepto.

- **Usuario**
  - Credenciales de acceso.
  - Relación con roles.

- **Rol**
  - Nombre del rol (ADMIN, CAJA, CONSULTA, etc.).
  - Permisos asociados.

---

## Arquitectura sugerida

> Nota: esta sección es orientativa. Ajusta las tecnologías según tu implementación real.

- **Backend**
  - Lenguaje: Java 17+
  - Framework: Spring Boot
  - Persistencia: Spring Data JPA / Hibernate
  - Base de datos: PostgreSQL o MySQL
  - Autenticación: JWT o sesión con Spring Security
  - Exposición: API REST

- **Frontend (opcional)**
  - React / Next.js o cualquier framework SPA
  - UI para:
    - ABM de clientes y disciplinas.
    - Pantallas de inscripción.
    - Módulos de caja y facturación.
    - Tableros simples de métricas.

- **Infraestructura**
  - Docker / Docker Compose para levantar servicios.
  - Perfiles de configuración: `dev` y `prod`.

---

## Estructura del proyecto

Ejemplo de estructura monorepo:

```text
gym-academy-manager/
├─ backend/
│  ├─ src/
│  ├─ pom.xml / build.gradle
│  └─ README.md (específico de backend)
├─ frontend/           # opcional
│  ├─ src/
│  ├─ package.json
│  └─ README.md (específico de frontend)
├─ docs/
│  ├─ modelo-dominio.md
│  ├─ decisiones-arquitectura.md
│  └─ casos-de-uso.md
└─ README.md           # este archivo
```

---

## Requisitos previos

Dependiendo del stack que utilices, una configuración típica podría requerir:

- **Backend**
  - JDK 17+
  - Maven o Gradle
- **Frontend (si aplica)**
  - Node.js 20+ y npm/pnpm/yarn
- **Base de datos**
  - PostgreSQL / MySQL en local
  - O bien un servicio orquestado con Docker

---

## Configuración y ejecución

### Backend

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/TU_USUARIO/gym-academy-manager.git
   cd gym-academy-manager/backend
   ```

2. Configurar variables de entorno o `application.yml`:

   Por ejemplo:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/gym_academy
       username: TU_USUARIO_DB
       password: TU_PASSWORD_DB
     jpa:
       hibernate:
         ddl-auto: update
   jwt:
     secret: CAMBIAR_POR_UN_SECRETO_SEGURO
   ```

   Ajustar nombres de propiedades según tu configuración.

3. Ejecutar la aplicación:

   ```bash
   # Con Maven
   mvn spring-boot:run

   # O con Gradle
   ./gradlew bootRun
   ```

4. La API debería levantarse, por ejemplo, en:

   ```text
   http://localhost:8080
   ```

   Ajustar puerto y rutas según tu configuración.

### Frontend (opcional)

1. Ir al directorio del frontend:

   ```bash
   cd ../frontend
   ```

2. Instalar dependencias:

   ```bash
   npm install
   # o
   pnpm install
   ```

3. Configurar variables (por ejemplo `.env.local`):

   ```env
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
   ```

4. Levantar servidor de desarrollo:

   ```bash
   npm run dev
   ```

   Por defecto:

   ```text
   http://localhost:3000
   ```

---

## Casos de uso principales

Algunos flujos clave que el sistema cubre:

1. **Alta de cliente**
   - Registrar nuevo cliente.
   - Asignar datos de contacto y estado inicial.

2. **Inscripción a disciplina**
   - Seleccionar cliente + disciplina.
   - Registrar inscripción con fecha de alta.
   - Generar mensualidades futuras según la configuración.

3. **Cobro de mensualidad**
   - Buscar cliente o disciplina.
   - Ver mensualidades pendientes.
   - Registrar pago y reflejarlo en caja.

4. **Cierre de caja diario**
   - Listar ingresos y egresos del día.
   - Ver saldo final de caja.

5. **Consulta de deuda**
   - Ver deudores por disciplina o en general.
   - Exportar o visualizar en un listado.

---

## Roadmap

Ideas para futuras mejoras:

- Registro de asistencias por clase.
- Envío de recordatorios de pago (email / WhatsApp).
- Integración con pasarelas de pago.
- Panel de métricas (facturación mensual, morosidad, retención de clientes).
- Multi-sucursal / multi-sede.
- Soporte para diferentes tipos de planes (mensual, trimestral, anual).

---

## Licencia

Define acá la licencia que quieras usar (por ejemplo, MIT):

```text
MIT License

Copyright (c) AÑO TU_NOMBRE
```

Ajusta nombre y año según corresponda.
