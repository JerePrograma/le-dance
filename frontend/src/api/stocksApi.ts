// src/stocksApi.ts
import api from "./axiosConfig";
import type {
  StockRegistroRequest,
  StockModificacionRequest,
  StockResponse,
} from "../types/types";

const registrarStock = async (
  stock: StockRegistroRequest
): Promise<StockResponse> => {
  const { data } = await api.post<StockResponse>("/stocks", stock);
  return data;
};

const obtenerStockPorId = async (id: number): Promise<StockResponse> => {
  const { data } = await api.get<StockResponse>(`/stocks/${id}`);
  return data;
};

const actualizarStock = async (
  id: number,
  stock: StockModificacionRequest
): Promise<StockResponse> => {
  const { data } = await api.put<StockResponse>(`/stocks/${id}`, stock);
  return data;
};

const eliminarStock = async (id: number): Promise<void> => {
  await api.delete(`/stocks/${id}`);
};

const listarStocks = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/stocks");
  return data;
};

const listarStocksActivos = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/stocks/activos");
  return data;
};

const listarStocksConceptos = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/stocks/conceptos");
  return data;
};

const stocksApi = {
  registrarStock,
  obtenerStockPorId,
  listarStocks,
  listarStocksActivos,
  actualizarStock,
  eliminarStock,
  listarStocksConceptos,
};

export default stocksApi;
