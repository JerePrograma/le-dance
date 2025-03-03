import { createContext, useContext, useState, ReactNode } from "react";
import asistenciasApi from "../../api/asistenciasApi";
import { toast } from "react-toastify";
import type {
  AsistenciaMensualDetalleResponse,
  AsistenciaDiariaResponse,
  AsistenciaDiariaRegistroRequest,
} from "../../types/types";
import { EstadoAsistencia } from "../../types/types";

interface AsistenciaContextProps {
  asistenciaMensual: AsistenciaMensualDetalleResponse | null;
  asistenciasDiarias: AsistenciaDiariaResponse[];
  cargarAsistenciaMensual: (id: number) => Promise<void>;
  actualizarAsistenciaDiaria: (
    id: number,
    estado: EstadoAsistencia
  ) => Promise<void>;
}

const AsistenciaContext = createContext<AsistenciaContextProps | undefined>(
  undefined
);

export const useAsistencia = (): AsistenciaContextProps => {
  const context = useContext(AsistenciaContext);
  if (!context)
    throw new Error("useAsistencia debe usarse dentro de AsistenciaProvider");
  return context;
};

export const AsistenciaProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [asistenciaMensual, setAsistenciaMensual] =
    useState<AsistenciaMensualDetalleResponse | null>(null);
  const [asistenciasDiarias, setAsistenciasDiarias] = useState<
    AsistenciaDiariaResponse[]
  >([]);

  const cargarAsistenciaMensual = async (id: number) => {
    try {
      const data = await asistenciasApi.obtenerAsistenciaMensualDetalle(id);
      if (!data) {
        toast.warn("No se encontro la asistencia mensual.");
        return;
      }
      setAsistenciaMensual(data);
      setAsistenciasDiarias(data.asistenciasDiarias || []);
    } catch (error) {
      console.error("Error al cargar asistencia mensual:", error);
      toast.error("Error al cargar la asistencia mensual.");
    }
  };

  const actualizarAsistenciaDiaria = async (
    id: number,
    estado: EstadoAsistencia
  ) => {
    try {
      const asistenciaExistente = asistenciasDiarias.find((a) => a.id === id);
      if (!asistenciaExistente)
        throw new Error("No se encontro la asistencia diaria.");
      const asistenciaActualizada: AsistenciaDiariaRegistroRequest = {
        id: asistenciaExistente.id,
        fecha: asistenciaExistente.fecha,
        alumnoId: asistenciaExistente.alumnoId,
        asistenciaMensualId: asistenciaExistente.asistenciaMensualId,
        estado,
        observacion: asistenciaExistente.observacion || "",
      };
      await asistenciasApi.registrarAsistenciaDiaria(asistenciaActualizada);
      setAsistenciasDiarias((prev) =>
        prev.map((a) => (a.id === id ? { ...a, estado } : a))
      );
      toast.success("Asistencia actualizada correctamente.");
    } catch (error) {
      console.error("Error al actualizar asistencia:", error);
      toast.error("Error al actualizar la asistencia.");
    }
  };

  return (
    <AsistenciaContext.Provider
      value={{
        asistenciaMensual,
        asistenciasDiarias,
        cargarAsistenciaMensual,
        actualizarAsistenciaDiaria,
      }}
    >
      {children}
    </AsistenciaContext.Provider>
  );
};
