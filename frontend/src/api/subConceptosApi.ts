import api from "./axiosConfig";
import type { SubConceptoResponse } from "../types/types";

const obtenerSubConceptoPorDescripcion = async (
  descripcion: string
): Promise<SubConceptoResponse | null> => {
  try {
    // Se asume que existe un endpoint para obtener subconcepto por descripción
    // Puedes ajustar la URL y la lógica según tu backend.
    const { data } = await api.get<SubConceptoResponse[]>(
      `/api/sub-conceptos?descripcion=${encodeURIComponent(descripcion)}`
    );
    // Suponemos que se devuelve un array y tomamos el primero (si existe)
    return data.length > 0 ? data[0] : null;
  } catch (error) {
    console.error("Error al obtener subconcepto por descripción:", error);
    return null;
  }
};

const registrarSubConcepto = async (
  descripcion: string
): Promise<SubConceptoResponse> => {
  // Se asume que el endpoint para crear subconceptos es POST /api/sub-conceptos
  const { data } = await api.post<SubConceptoResponse>("/api/sub-conceptos", {
    descripcion,
  });
  return data;
};

const subConceptosApi = {
  obtenerSubConceptoPorDescripcion,
  registrarSubConcepto,
};

export default subConceptosApi;
