import api from "./axiosConfig";
import type {
  SubConceptoResponse,
  SubConceptoRegistroRequest,
  SubConceptoModificacionRequest
} from "../types/types";

// Obtiene la lista de todos los subconceptos.
const listarSubConceptos = async (): Promise<SubConceptoResponse[]> => {
  try {
    const { data } = await api.get<SubConceptoResponse[]>("/sub-conceptos");
    return data;
  } catch (error) {
    console.error("Error al listar subconceptos:", error);
    throw error;
  }
};

// Obtiene un subconcepto por su ID.
const obtenerSubConceptoPorId = async (id: number): Promise<SubConceptoResponse> => {
  try {
    const { data } = await api.get<SubConceptoResponse>(`/sub-conceptos/${id}`);
    return data;
  } catch (error) {
    console.error(`Error al obtener subconcepto con id ${id}:`, error);
    throw error;
  }
};

// Obtiene un subconcepto por descripcion (se espera un array y se toma el primero).
const obtenerSubConceptoPorDescripcion = async (
  descripcion: string
): Promise<SubConceptoResponse | null> => {
  try {
    const { data } = await api.get<SubConceptoResponse[]>(
      `/sub-conceptos?descripcion=${encodeURIComponent(descripcion)}`
    );
    return data.length > 0 ? data[0] : null;
  } catch (error) {
    console.error("Error al obtener subconcepto por descripcion:", error);
    return null;
  }
};

// Registra un nuevo subconcepto utilizando el payload definido.
const registrarSubConcepto = async (
  request: SubConceptoRegistroRequest
): Promise<SubConceptoResponse> => {
  try {
    const { data } = await api.post<SubConceptoResponse>("/sub-conceptos", request);
    return data;
  } catch (error) {
    console.error("Error al registrar subconcepto:", error);
    throw error;
  }
};

// Funcion para buscar subconceptos por nombre.
// Se asume que el backend expone un endpoint GET en "/sub-conceptos/buscar" que recibe el parametro "nombre"
const buscarSubConceptos = async (nombre: string): Promise<SubConceptoResponse[]> => {
  try {
    const { data } = await api.get<SubConceptoResponse[]>(
      `/sub-conceptos/buscar?nombre=${encodeURIComponent(nombre)}`
    );
    return data;
  } catch (error) {
    console.error("Error al buscar subconceptos por nombre:", error);
    throw error;
  }
};

// Funcion para actualizar un subconcepto por su ID.
const actualizarSubConcepto = async (
  id: number,
  request: SubConceptoModificacionRequest
): Promise<SubConceptoResponse> => {
  try {
    const { data } = await api.put<SubConceptoResponse>(`/sub-conceptos/${id}`, request);
    return data;
  } catch (error) {
    console.error(`Error al actualizar subconcepto con id ${id}:`, error);
    throw error;
  }
};

// Funcion para eliminar un subconcepto por su ID.
const eliminarSubConcepto = async (id: number): Promise<void> => {
  try {
    await api.delete(`/sub-conceptos/${id}`);
  } catch (error) {
    console.error(`Error al eliminar subconcepto con id ${id}:`, error);
    throw error;
  }
};

const subConceptosApi = {
  listarSubConceptos,
  obtenerSubConceptoPorId,
  buscarSubConceptos,
  obtenerSubConceptoPorDescripcion,
  registrarSubConcepto,
  actualizarSubConcepto,
  eliminarSubConcepto,
};

export default subConceptosApi;
