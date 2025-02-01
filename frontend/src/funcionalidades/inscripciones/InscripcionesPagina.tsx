import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import { InscripcionResponse } from "../../types/types";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

const InscripcionesPagina: React.FC = () => {
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
      const data = await inscripcionesApi.listarInscripciones();
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
      await inscripcionesApi.eliminarInscripcion(id);
      setInscripciones((prev) => prev.filter((ins) => ins.id !== id));
    } catch (error) {
      console.error("Error al eliminar inscripción:", error);
    }
  }, []);

  const pageCount = useMemo(
    () => Math.ceil(inscripciones.length / itemsPerPage),
    [inscripciones.length, itemsPerPage]
  );
  const currentItems = useMemo(
    () =>
      inscripciones.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [inscripciones, currentPage, itemsPerPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  if (loading) return <div>Cargando...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Inscripciones</h1>
      <div className="flex justify-end mb-4">
        <Boton onClick={handleCrearInscripcion}>Nueva Inscripción</Boton>
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
          datos={currentItems}
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
