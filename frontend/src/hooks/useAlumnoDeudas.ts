// src/hooks/useAlumnoDeudas.ts
import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import deudasApi from "../api/deudasApi";
import { toast } from "react-toastify";
import type {
  DeudasPendientesResponse,
  InscripcionResponse,
} from "../types/types";

export const useAlumnoDeudas = (alumnoId: number) => {
  const query = useQuery<DeudasPendientesResponse, Error>({
    queryKey: ["deudas", alumnoId],
    queryFn: () => deudasApi.obtenerDeudasPendientes(alumnoId),
    enabled: Boolean(alumnoId),
  });

  useEffect(() => {
    if (query.error) {
      toast.error(query.error.message || "Error al cargar deudas");
    }
  }, [query.error]);

  // Extraer desde la data la inscripción si existe en el primer detalle pendiente
  const dataWithAlumno = useMemo(() => {
    if (query.data) {
      const {
        alumnoId: debtAlumnoId,
        alumnoNombre,
        detallePagosPendientes,
      } = query.data;
      const inscripcion =
        detallePagosPendientes && detallePagosPendientes.length > 0
          ? detallePagosPendientes[0].inscripcion
          : null;
      return {
        ...query.data,
        alumnoId: debtAlumnoId,
        alumnoNombre,
        inscripcion,
      };
    }
    return query.data;
  }, [query.data]);

  // Si necesitas un estado local para la inscripción (opcional)
  const [inscripcion, setInscripcion] = useState<InscripcionResponse | null>(
    dataWithAlumno?.inscripcion || null
  );

  useEffect(() => {
    if (
      dataWithAlumno?.detallePagosPendientes &&
      dataWithAlumno.detallePagosPendientes.length > 0
    ) {
      setInscripcion(dataWithAlumno.detallePagosPendientes[0].inscripcion);
    }
  }, [dataWithAlumno]);

  // Se devuelve la data ya enriquecida, y opcionalmente la inscripción separada
  return { ...query, data: dataWithAlumno, inscripcion };
};
