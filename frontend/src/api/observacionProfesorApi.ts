// observacionProfesorApi.ts
import api from "./axiosConfig";
import type {
  ObservacionProfesorResponse,
  ObservacionProfesorRequest,
} from "../types/types";

// Crea una nueva ObservacionProfesor
const crearObservacionProfesor = async (
  observacion: ObservacionProfesorRequest
): Promise<ObservacionProfesorResponse> => {
  const { data } = await api.post<ObservacionProfesorResponse>(
    "/observaciones-profesores",
    observacion
  );
  return data;
};

// Obtiene una ObservacionProfesor por su ID
const obtenerObservacionProfesorPorId = async (
  id: number
): Promise<ObservacionProfesorResponse> => {
  const { data } = await api.get<ObservacionProfesorResponse>(
    `/observaciones-profesores/${id}`
  );
  return data;
};

// Actualiza una ObservacionProfesor existente
const actualizarObservacionProfesor = async (
  id: number,
  observacion: ObservacionProfesorRequest
): Promise<ObservacionProfesorResponse> => {
  const { data } = await api.put<ObservacionProfesorResponse>(
    `/observaciones-profesores/${id}`,
    observacion
  );
  return data;
};

// Elimina una ObservacionProfesor por su ID
const eliminarObservacionProfesor = async (id: number): Promise<void> => {
  await api.delete(`/observaciones-profesores/${id}`);
};

// Lista todas las ObservacionesProfesor
const listarObservacionesProfesores = async (): Promise<
  ObservacionProfesorResponse[]
> => {
  const { data } = await api.get<ObservacionProfesorResponse[]>(
    "/observaciones-profesores"
  );
  return data;
};

const listarObservacionesPorProfesor = async (
  profesorId: number
): Promise<ObservacionProfesorResponse[]> => {
  const { data } = await api.get<ObservacionProfesorResponse[]>(
    `/observaciones-profesores/profesor/${profesorId}`
  );
  return data;
};

const observacionProfesorApi = {
  crearObservacionProfesor,
  obtenerObservacionProfesorPorId,
  actualizarObservacionProfesor,
  eliminarObservacionProfesor,
  listarObservacionesProfesores,
  listarObservacionesPorProfesor,
};

export default observacionProfesorApi;
