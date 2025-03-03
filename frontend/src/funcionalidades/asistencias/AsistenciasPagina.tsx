import React, { useState, useEffect, useCallback } from "react";
import { Field } from "formik";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import { toast } from "react-toastify";
import { Search } from "lucide-react";
import asistenciasApi from "../../api/asistenciasApi";
import {
  type AsistenciaMensualDetalleResponse,
  type AsistenciaDiariaRegistroRequest,
  EstadoAsistencia,
} from "../../types/types";
import { debounce } from "lodash";

const AsistenciasPage: React.FC = () => {
  const [selectedPD, setSelectedPD] = useState<number>(0);
  const [mes, setMes] = useState(new Date().getMonth() + 1);
  const [anio, setAnio] = useState(new Date().getFullYear());
  const [asistencia, setAsistencia] =
    useState<AsistenciaMensualDetalleResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [observaciones, setObservaciones] = useState<Record<number, string>>({});

  const meses = [
    { value: 1, label: "Enero" },
    { value: 2, label: "Febrero" },
    { value: 3, label: "Marzo" },
    { value: 4, label: "Abril" },
    { value: 5, label: "Mayo" },
    { value: 6, label: "Junio" },
    { value: 7, label: "Julio" },
    { value: 8, label: "Agosto" },
    { value: 9, label: "Septiembre" },
    { value: 10, label: "Octubre" },
    { value: 11, label: "Noviembre" },
    { value: 12, label: "Diciembre" },
  ];

  const getSabadosDelMes = useCallback((mes: number, anio: number): Date[] => {
    const sabados: Date[] = [];
    const fecha = new Date(anio, mes - 1, 1);

    while (fecha.getMonth() === mes - 1) {
      if (fecha.getDay() === 6) {
        sabados.push(new Date(fecha));
      }
      fecha.setDate(fecha.getDate() + 1);
    }
    return sabados;
  }, []);

  const cargarAsistencia = useCallback(async () => {
    if (!selectedPD) return;
    setLoading(true);
    try {
      const data = await asistenciasApi.obtenerAsistenciaMensualDetalle(selectedPD);
      if (!data) {
        toast.error("No se encontro la asistencia mensual.");
        setLoading(false);
        return;
      }
      setAsistencia(data);
      // Convertir el arreglo de observaciones en un objeto record: { [alumnoId]: observacion }
      setObservaciones(
        data.observaciones.reduce((acc, obs) => {
          acc[obs.alumnoId] = obs.observacion;
          return acc;
        }, {} as Record<number, string>)
      );
    } catch (error) {
      console.error("Error al cargar asistencia:", error);
      toast.error("Error al cargar la asistencia");
    } finally {
      setLoading(false);
    }
  }, [selectedPD]);

  useEffect(() => {
    cargarAsistencia();
  }, [cargarAsistencia]);

  const toggleAsistencia = async (alumnoId: number, fecha: string) => {
    if (!asistencia) return;

    // Buscar la asistencia diaria existente
    const asistenciaDiaria = asistencia.asistenciasDiarias.find(
      (ad) => ad.alumnoId === alumnoId && ad.fecha === fecha
    );

    if (!asistenciaDiaria) {
      console.error("No se encontro una asistencia diaria para este alumno en esta fecha.");
      toast.error("Error al actualizar la asistencia.");
      return;
    }

    try {
      const asistenciaDiariaRequest: AsistenciaDiariaRegistroRequest = {
        id: asistenciaDiaria.id, // ✅ Ahora usa el ID correcto
        asistenciaMensualId: asistencia.id,
        alumnoId,
        fecha,
        estado: asistenciaDiaria.estado === EstadoAsistencia.PRESENTE ? EstadoAsistencia.AUSENTE : EstadoAsistencia.PRESENTE,
      };

      await asistenciasApi.registrarAsistenciaDiaria(asistenciaDiariaRequest);

      // Actualizar el estado en el frontend
      setAsistencia((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          asistenciasDiarias: prev.asistenciasDiarias.map(ad =>
            ad.id === asistenciaDiaria.id
              ? { ...ad, estado: asistenciaDiariaRequest.estado }
              : ad
          ),
        };
      });

      toast.success("Asistencia actualizada");
    } catch (error) {
      console.error("Error al registrar asistencia:", error);
      toast.error("Error al registrar la asistencia");
    }
  };

  const debouncedActualizarObservacion = useCallback(
    debounce(async (alumnoId: number, obs: string) => {
      if (!asistencia) return;
      try {
        await asistenciasApi.actualizarAsistenciaMensual(asistencia.id, {
          observaciones: Object.entries({
            ...observaciones,
            [alumnoId]: obs,
          }).map(([id, observacion]) => ({
            alumnoId: Number(id),
            observacion,
          })),
        });
        toast.success("Observacion actualizada");
      } catch (error) {
        console.error("Error al guardar observacion:", error);
        toast.error("Error al guardar la observacion");
      }
    }, 500),
    [asistencia, observaciones]
  );

  const handleObservacionChange = (alumnoId: number, obs: string) => {
    setObservaciones((prev) => ({
      ...prev,
      [alumnoId]: obs,
    }));
    debouncedActualizarObservacion(alumnoId, obs);
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Control de Asistencias</h1>
      <div className="form-grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div>
          <label htmlFor="disciplina" className="auth-label">
            Disciplina:
          </label>
          <Field
            as="select"
            id="disciplina"
            name="disciplina"
            className="form-input"
            value={selectedPD}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              setSelectedPD(Number(e.target.value))
            }
          >
            <option value="0">Seleccione disciplina...</option>
            <option value="1">BALLET PROFESORADO CIAD</option>
            {/* Agregar mas opciones segun tus datos */}
          </Field>
        </div>
        <div>
          <label htmlFor="mes" className="auth-label">
            Mes:
          </label>
          <Field
            as="select"
            id="mes"
            name="mes"
            className="form-input"
            value={mes}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              setMes(Number(e.target.value))
            }
          >
            {meses.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </Field>
        </div>
        <div>
          <label htmlFor="anio" className="auth-label">
            Año:
          </label>
          <Field
            as="select"
            id="anio"
            name="anio"
            className="form-input"
            value={anio}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              setAnio(Number(e.target.value))
            }
          >
            {Array.from({ length: 5 }, (_, i) => {
              const year = new Date().getFullYear() - 2 + i;
              return (
                <option key={year} value={year}>
                  {year}
                </option>
              );
            })}
          </Field>
        </div>
      </div>

      <Boton onClick={cargarAsistencia} className="page-button mb-6">
        <Search className="w-5 h-5 mr-2" />
        Buscar Asistencias
      </Boton>

      {loading ? (
        <div className="text-center py-4">Cargando...</div>
      ) : asistencia ? (
        <div className="page-card">
          <h2 className="text-xl font-bold mb-4">
            {asistencia.disciplina} -{" "}
            {new Date(asistencia.anio, asistencia.mes - 1).toLocaleDateString("es", {
              month: "long",
              year: "numeric",
            })}
          </h2>
          <Tabla
            encabezados={[
              "Nombre",
              ...getSabadosDelMes(mes, anio).map((sabado) =>
                sabado.getDate().toString()
              ),
              "Observaciones",
            ]}
            datos={asistencia.alumnos}
            acciones={() => <></>}
            extraRender={(alumno) => [
              `${alumno.apellido}, ${alumno.nombre}`,
              ...getSabadosDelMes(mes, anio).map((sabado, index) => {
                const fecha = sabado.toISOString().split("T")[0];
                const asistenciaDiaria = asistencia.asistenciasDiarias.find(
                  (ad) => ad.alumnoId === alumno.id && ad.fecha === fecha
                );
                return (
                  <input
                    key={index}
                    type="checkbox"
                    checked={
                      asistenciaDiaria?.estado === EstadoAsistencia.PRESENTE
                    }
                    onChange={() => toggleAsistencia(alumno.id, fecha)}
                    className="form-checkbox"
                  />
                );
              }),
              <input
                type="text"
                key={alumno.id}
                value={observaciones[alumno.id] || ""}
                onChange={(e) =>
                  handleObservacionChange(alumno.id, e.target.value)
                }
                className="form-input"
                placeholder="Observaciones..."
              />,
            ]}
          />
        </div>
      ) : selectedPD > 0 ? (
        <div className="text-center py-4">
          No se encontraron datos para el periodo seleccionado
        </div>
      ) : null}
    </div>
  );
};

export default AsistenciasPage;
