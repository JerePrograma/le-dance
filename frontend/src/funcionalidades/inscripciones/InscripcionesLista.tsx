import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import { InscripcionResponse } from "../../types/types";

const InscripcionesLista: React.FC = () => {
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchInscripciones = async () => {
      try {
        const data = await inscripcionesApi.listarInscripciones();
        setInscripciones(data);
      } catch (error) {
        console.error("Error al cargar inscripciones:", error);
      }
    };
    fetchInscripciones();
  }, []);

  const handleCrearInscripcion = () => {
    navigate("/inscripciones/formulario");
  };

  const handleEditarInscripcion = (id: number) => {
    navigate(`/inscripciones/formulario?id=${id}`);
  };

  const handleEliminarInscripcion = async (id: number) => {
    try {
      await inscripcionesApi.eliminarInscripcion(id);
      setInscripciones((prev) => prev.filter((ins) => ins.id !== id));
    } catch (error) {
      console.error("Error al eliminar inscripción:", error);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Inscripciones</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleCrearInscripcion} className="page-button">
          Nueva Inscripción
        </button>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={[
            "ID",
            "AlumnoID",
            "DisciplinaID",
            "BonificaciónID",
            "Costo Particular",
            "Notas",
            "Acciones",
          ]}
          datos={inscripciones}
          acciones={(fila) => (
            <div className="flex gap-2">
              <button
                onClick={() => handleEditarInscripcion(fila.id)}
                className="page-button bg-blue-500 hover:bg-blue-600"
              >
                Editar
              </button>
              <button
                onClick={() => handleEliminarInscripcion(fila.id)}
                className="page-button bg-red-500 hover:bg-red-600"
              >
                Eliminar
              </button>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.alumnoId,
            fila.disciplinaId,
            fila.bonificacionId ?? "N/A",
            fila.costoParticular ?? 0,
            fila.notas ?? "",
          ]}
        />
      </div>
    </div>
  );
};

export default InscripcionesLista;
