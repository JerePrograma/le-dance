export const queryKeys = {
  alumnos: ["alumnos"] as const,
  cargosPendientes: (alumnoId: number, page = 0) => ["cargos", "pendientes", alumnoId, page] as const,
  caja: (desde: string, hasta: string) => ["caja", desde, hasta] as const,
  egresos: (page: number) => ["egresos", page] as const,
  metodosPago: ["metodos-pago"] as const,
  pagos: (alumnoId: number, page = 0) => ["pagos", alumnoId, page] as const,
  inscripciones: (page: number) => ["inscripciones", page] as const,
  stocks: (page: number) => ["stocks", page] as const,
};
