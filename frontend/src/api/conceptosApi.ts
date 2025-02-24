import api from "./axiosConfig";
import type {
  ConceptoRegistroRequest,
  ConceptoModificacionRequest,
  ConceptoResponse,
} from "../types/types";

const registrarConcepto = async (
  concepto: ConceptoRegistroRequest
): Promise<ConceptoResponse> => {
  const { data } = await api.post<ConceptoResponse>("/api/conceptos", concepto);
  return data;
};

const obtenerConceptoPorId = async (id: number): Promise<ConceptoResponse> => {
  const { data } = await api.get<ConceptoResponse>(`/api/conceptos/${id}`);
  return data;
};

const listarConceptos = async (): Promise<ConceptoResponse[]> => {
  const { data } = await api.get<ConceptoResponse[]>("/api/conceptos");
  return data;
};

const actualizarConcepto = async (
  id: number,
  concepto: ConceptoModificacionRequest
): Promise<ConceptoResponse> => {
  const { data } = await api.put<ConceptoResponse>(
    `/api/conceptos/${id}`,
    concepto
  );
  return data;
};

const eliminarConcepto = async (id: number): Promise<void> => {
  await api.delete(`/api/conceptos/${id}`);
};

const conceptosApi = {
  registrarConcepto,
  obtenerConceptoPorId,
  listarConceptos,
  actualizarConcepto,
  eliminarConcepto,
};

export default conceptosApi;
