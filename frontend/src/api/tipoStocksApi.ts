// src/tipoStocksApi.ts
import api from "./axiosConfig";
import type {
  TipoStockRegistroRequest,
  TipoStockModificacionRequest,
  TipoStockResponse,
} from "../types/types";

const registrarTipoStock = async (
  tipo: TipoStockRegistroRequest
): Promise<TipoStockResponse> => {
  const { data } = await api.post<TipoStockResponse>("/tipo-stocks", tipo);
  return data;
};

const obtenerTipoStockPorId = async (
  id: number
): Promise<TipoStockResponse> => {
  const { data } = await api.get<TipoStockResponse>(`/tipo-stocks/${id}`);
  return data;
};

const listarTiposStock = async (): Promise<TipoStockResponse[]> => {
  const { data } = await api.get<TipoStockResponse[]>("/tipo-stocks");
  return data;
};

const listarTiposStockActivos = async (): Promise<TipoStockResponse[]> => {
  const { data } = await api.get<TipoStockResponse[]>("/tipo-stocks/activos");
  return data;
};

const actualizarTipoStock = async (
  id: number,
  tipo: TipoStockModificacionRequest
): Promise<TipoStockResponse> => {
  const { data } = await api.put<TipoStockResponse>(`/tipo-stocks/${id}`, tipo);
  return data;
};

const eliminarTipoStock = async (id: number): Promise<void> => {
  await api.delete(`/tipo-stocks/${id}`);
};

const tipoStocksApi = {
  registrarTipoStock,
  obtenerTipoStockPorId,
  listarTiposStock,
  listarTiposStockActivos,
  actualizarTipoStock,
  eliminarTipoStock,
};

export default tipoStocksApi;
