import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Asistencia {
  id: number;
  alumno: string;
  disciplina: string;
  fecha: string;
  presente: boolean;
}

const Asistencias = () => {
  const [asistencias, setAsistencias] = useState<Asistencia[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const itemsPerPage = 5; // 🔄 Cantidad de asistencias por página
  const navigate = useNavigate();

  // 🔄 Fetch optimizado
  const fetchAsistencias = useCallback(async () => {
    try {
      const response = await api.get<Asistencia[]>("/api/asistencias");
      setAsistencias(response.data);
    } catch (error) {
      console.error("Error al cargar asistencias:", error);
    }
  }, []);

  useEffect(() => {
    fetchAsistencias();
  }, [fetchAsistencias]);

  // 🔄 Paginación segura
  const pageCount = Math.ceil(asistencias.length / itemsPerPage);
  const currentItems = asistencias.slice(
    currentPage * itemsPerPage,
    (currentPage + 1) * itemsPerPage
  );

  const handlePageClick = ({ selected }: { selected: number }) => {
    if (selected < pageCount) {
      setCurrentPage(selected);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>

      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/asistencias/formulario")}>
          Registrar Nueva Asistencia
        </Boton>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={[
            "ID",
            "Alumno",
            "Disciplina",
            "Fecha",
            "Presente",
            "Acciones",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/asistencias/formulario?id=${fila.id}`)
                }
                secondary
                aria-label={`Editar asistencia de ${fila.alumno} en ${fila.disciplina}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar asistencia de ${fila.alumno}`}
              >
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.alumno,
            fila.disciplina,
            fila.fecha,
            fila.presente ? "Sí" : "No",
          ]}
        />
      </div>

      {/* 🔄 Paginación Mejorada */}
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

export default Asistencias;
