import { useEffect, useState, useCallback, useMemo } from "react";
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
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchDisciplinas = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get<Disciplina[]>("/api/disciplinas");
      setDisciplinas(response.data);
    } catch (error) {
      console.error("Error al cargar disciplinas:", error);
      setError("Error al cargar disciplinas.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDisciplinas();
  }, [fetchDisciplinas]);

  const pageCount = useMemo(
    () => Math.ceil(disciplinas.length / itemsPerPage),
    [disciplinas.length, itemsPerPage]
  );
  const currentItems = useMemo(
    () =>
      disciplinas.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [disciplinas, currentPage, itemsPerPage]
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
