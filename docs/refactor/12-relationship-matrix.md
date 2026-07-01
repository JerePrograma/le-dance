# Matriz de relaciones canónicas

| Propietario | Relación | Cardinalidad/constraint | Borrado | Autoridad |
| --- | --- | --- | --- | --- |
| Usuario | Rol | N:1, no nulo | RESTRICT | seguridad persistida |
| Inscripción | Alumno + Disciplina | N:1; única si ACTIVA | RESTRICT | vínculo académico |
| Mensualidad | Inscripción | N:1; unique período | RESTRICT | origen mensual |
| Matrícula | Alumno | N:1; unique alumno/año | RESTRICT | origen anual |
| Cargo | origen | exactamente una FK de origen | RESTRICT | obligación/dinero original |
| Aplicación | Pago + Cargo + Usuario | N:1; unique pago/cargo | RESTRICT | distribución histórica |
| Movimiento caja | Pago/Egreso/Reverso | checks por tipo; reverso unique | RESTRICT | saldo de caja |
| Movimiento crédito | Pago/Cargo/Reverso | checks por tipo; reverso unique | RESTRICT | saldo de crédito |
| Venta stock | Alumno + Stock | N:1; key/hash | RESTRICT | venta histórica |
| Movimiento stock | Stock + Venta/Reverso | checks por tipo; reverso unique | RESTRICT | cantidad actual derivable |
| Recibo | Pago | 1:1 | RESTRICT | documento histórico |
| Recibo pendiente | Pago | unique pago/tipo y key | RESTRICT | trabajo técnico |
| Asistencia diaria | Asistencia alumno/mes | unique fecha | RESTRICT | presencia histórica |
| Horario disciplina | Disciplina | N:1, unique día/hora | CASCADE | configuración reemplazable |

No hay `cascade=ALL` u `orphanRemoval` sobre historia financiera. La única
cascada física está limitada a horarios de disciplina y no cruza un límite de
auditoría.
