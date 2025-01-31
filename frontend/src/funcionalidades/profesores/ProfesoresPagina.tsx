import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Profesor {
  id: number;
  nombre: string;
  apellido: string;
  especialidad: string;
  aniosExperiencia: number;
}

const Profesores = () => {
  const [profesores, setProfesores] = useState<Profesor[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchProfesores = useCallback(async () => {
    try {
      const response = await api.get<Profesor[]>("/api/profesores");
      setProfesores(response.data);
    } catch (error) {
      console.error("Error al cargar profesores:", error);
    }
  }, []);

  useEffect(() => {
    fetchProfesores();
  }, [fetchProfesores]);

  const pageCount = Math.ceil(profesores.length / itemsPerPage);
  const currentItems = profesores.slice(
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
      <h1 className="page-title">Profesores</h1>

      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/profesores/formulario")}>
          Registrar Nuevo Profesor
        </Boton>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={[
            "ID",
            "Nombre",
            "Apellido",
            "Especialidad",
            "Años de Experiencia",
            "Acciones",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/profesores/formulario?id=${fila.id}`)}
                secondary
                aria-label={`Editar profesor ${fila.nombre} ${fila.apellido}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar profesor ${fila.nombre}`}
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

export default Profesores;
