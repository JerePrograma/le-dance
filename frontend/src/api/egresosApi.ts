// src/egresoApi.ts
import api from "./axiosConfig";
import type { EgresoResponse, EgresoRegistroRequest } from "../types/types";

// Función para registrar un nuevo egreso
const registrarEgreso = async (
  egreso: EgresoRegistroRequest
): Promise<EgresoResponse> => {
  const { data } = await api.post<EgresoResponse>("/egresos", egreso);
  return data;
};

// Actualizar un egreso
const actualizarEgreso = async (
  id: number,
  egreso: EgresoRegistroRequest
): Promise<EgresoResponse> => {
  const { data } = await api.put<EgresoResponse>(`/egresos/${id}`, egreso);
  return data;
};

// Eliminar (baja lógica) un egreso
const eliminarEgreso = async (id: number): Promise<void> => {
  await api.delete(`/egresos/${id}`);
};

// Obtener un egreso por ID
const obtenerEgreso = async (id: number): Promise<EgresoResponse> => {
  const { data } = await api.get<EgresoResponse>(`/egresos/${id}`);
  return data;
};

// Listar todos los egresos activos
const listarEgresos = async (): Promise<EgresoResponse[]> => {
  const { data } = await api.get<EgresoResponse[]>("/egresos");
  return data;
};

// Listar egresos de tipo DEBITO
const listarEgresosDebito = async (): Promise<EgresoResponse[]> => {
  const { data } = await api.get<EgresoResponse[]>("/egresos/debito");
  console.log("Respuesta de egresos:", data);
  return Array.isArray(data) ? data : [];
};

// Listar egresos de tipo EFECTIVO
const listarEgresosEfectivo = async (): Promise<EgresoResponse[]> => {
  const { data } = await api.get<EgresoResponse[]>("/egresos/efectivo");
  return data;
};

const egresoApi = {
  registrarEgreso,
  actualizarEgreso,
  eliminarEgreso,
  obtenerEgreso,
  listarEgresos,
  listarEgresosDebito,
  listarEgresosEfectivo,
};

export default egresoApi;
