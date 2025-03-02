"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import inscripcionesApi from "../../api/inscripcionesApi";
import type { InscripcionResponse } from "../../types/types";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

const InscripcionesPagina = () => {
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchInscripciones = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await inscripcionesApi.listar();
      setInscripciones(data);
    } catch (error) {
      console.error("Error al cargar inscripciones:", error);
      setError("Error al cargar inscripciones.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchInscripciones();
  }, [fetchInscripciones]);

  const handleCrearInscripcion = useCallback(() => {
    navigate("/inscripciones/formulario");
  }, [navigate]);

  const handleEliminarInscripcion = useCallback(async (id: number) => {
    try {
      await inscripcionesApi.eliminar(id);
      setInscripciones((prev) => prev.filter((ins) => ins.id !== id));
    } catch (error) {
      console.error("Error al eliminar inscripción:", error);
    }
  }, []);

  const pageCount = useMemo(
    () => Math.ceil(inscripciones.length / itemsPerPage),
    [inscripciones.length]
  );
  const currentItems = useMemo(
    () =>
      inscripciones.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [inscripciones, currentPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Inscripciones</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={handleCrearInscripcion}
          className="page-button"
          aria-label="Crear nueva inscripción"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Nueva Inscripción
        </Boton>
      </div>
      <div className="page-card">
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
          datos={currentItems}
          extraRender={(fila) => [
            fila.id,
            fila.alumno.id,
            fila.disciplina.id,
            fila.bonificacion ? fila.bonificacion.descripcion : "N/A",
            fila.notas || "-",
          ]}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/inscripciones/formulario?id=${fila.id}`)
                }
                className="page-button-secondary"
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                onClick={() => handleEliminarInscripcion(fila.id)}
                className="page-button-danger"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
        />
      </div>
      {pageCount > 1 && (
        <ReactPaginate
          previousLabel={"← Anterior"}
          nextLabel={"Siguiente →"}
          breakLabel={"..."}
          pageCount={pageCount}
          onPageChange={handlePageClick}
          containerClassName={"pagination"}
          activeClassName={"active"}
          disabledClassName={"disabled"}
        />
      )}
    </div>
  );
};

export default InscripcionesPagina;
