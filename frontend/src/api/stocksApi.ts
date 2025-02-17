// src/api/stocksApi.ts
import api from "./axiosConfig";
import type {
  StockRegistroRequest,
  StockModificacionRequest,
  StockResponse,
} from "../types/types";

const registrarStock = async (
  stock: StockRegistroRequest
): Promise<StockResponse> => {
  const { data } = await api.post<StockResponse>("/api/stocks", stock);
  return data;
};

const obtenerStockPorId = async (id: number): Promise<StockResponse> => {
  const { data } = await api.get<StockResponse>(`/api/stocks/${id}`);
  return data;
};

const listarStocks = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/api/stocks");
  return data;
};

const listarStocksActivos = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/api/stocks/activos");
  return data;
};

const actualizarStock = async (
  id: number,
  stock: StockModificacionRequest
): Promise<StockResponse> => {
  const { data } = await api.put<StockResponse>(`/api/stocks/${id}`, stock);
  return data;
};

const eliminarStock = async (id: number): Promise<void> => {
  await api.delete(`/api/stocks/${id}`);
};

const stocksApi = {
  registrarStock,
  obtenerStockPorId,
  listarStocks,
  listarStocksActivos,
  actualizarStock,
  eliminarStock,
};

export default stocksApi;
