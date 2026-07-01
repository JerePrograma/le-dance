export const queryKeys = {
  alumnos: (page: number, size: number, filtro: string) =>
    ["alumnos", page, size, filtro.trim(), "id,asc"] as const,
  cargosPendientes: (alumnoId: number, page: number, size: number) =>
    ["cargos", "pendientes", alumnoId, page, size, "fechaVencimiento,asc;id,asc"] as const,
  caja: (desde: string, hasta: string, page: number, size: number) =>
    ["caja", desde, hasta, page, size, "fecha,asc;id,asc"] as const,
  egresos: (page: number, size: number) => ["egresos", page, size, "fecha,desc;id,desc"] as const,
  metodosPago: ["metodos-pago"] as const,
  pagos: (alumnoId: number, page: number, size: number) =>
    ["pagos", alumnoId, page, size, "fecha,desc;id,desc"] as const,
  inscripciones: (page: number, size: number, filtro: string) =>
    ["inscripciones", page, size, filtro.trim(), "id,desc"] as const,
  stocks: (page: number, size: number) => ["stocks", page, size, "nombre,asc;id,asc"] as const,
};
