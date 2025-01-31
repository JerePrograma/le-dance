import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Disciplina {
  id: number;
  nombre: string;
  horario: string;
}

const Disciplinas = () => {
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const itemsPerPage = 5; // 🔄 Cantidad de disciplinas por página
  const navigate = useNavigate();

  // 🔄 Fetch optimizado
  const fetchDisciplinas = useCallback(async () => {
    try {
      const response = await api.get<Disciplina[]>("/api/disciplinas");
      setDisciplinas(response.data);
    } catch (error) {
      console.error("Error al cargar disciplinas:", error);
    }
  }, []);

  useEffect(() => {
    fetchDisciplinas();
  }, [fetchDisciplinas]);

  // 🔄 Paginación segura
  const pageCount = Math.ceil(disciplinas.length / itemsPerPage);
  const currentItems = disciplinas.slice(
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
      <h1 className="page-title">Disciplinas</h1>

      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/disciplinas/formulario")}>
          Registrar Nueva Disciplina
        </Boton>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Nombre", "Horario", "Acciones"]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/disciplinas/formulario?id=${fila.id}`)
                }
                secondary
                aria-label={`Editar disciplina ${fila.nombre}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar disciplina ${fila.nombre}`}
              >
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [fila.id, fila.nombre, fila.horario]}
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

export default Disciplinas;
