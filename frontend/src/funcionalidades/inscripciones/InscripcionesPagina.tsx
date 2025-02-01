import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import { InscripcionResponse } from "../../types/types";
import Boton from "../../componentes/comunes/Boton";

const InscripcionesPagina: React.FC = () => {
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
            "Alumno",
            "Disciplina",
            "Bonificación",
            "Costo",
            "Notas",
            "Acciones",
          ]}
          datos={inscripciones}
          extraRender={(fila) => [
            fila.id,
            fila.alumno.nombre,
            fila.disciplina.nombre,
            fila.bonificacion ? fila.bonificacion.descripcion : "N/A",
            fila.costoParticular ? `$${fila.costoParticular.toFixed(2)}` : "-",
            fila.notas || "-",
          ]}
          acciones={(fila) => (
            <div className="flex flex-col sm:flex-row gap-2">
              <Boton
                onClick={() =>
                  navigate(`/inscripciones/formulario?id=${fila.id}`)
                }
                secondary
              >
                Editar
              </Boton>
              <Boton
                onClick={() => handleEliminarInscripcion(fila.id)}
                className="bg-red-500 text-white hover:bg-red-600"
              >
                Eliminar
              </Boton>
            </div>
          )}
        />
      </div>
    </div>
  );
};

export default InscripcionesPagina;
