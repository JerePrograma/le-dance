import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Bonificacion {
  id: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

const Bonificaciones = () => {
  const [bonificaciones, setBonificaciones] = useState<Bonificacion[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const itemsPerPage = 5; // 🔄 Cantidad de bonificaciones por página
  const navigate = useNavigate();

  // 🔄 Fetch optimizado
  const fetchBonificaciones = useCallback(async () => {
    try {
      const response = await api.get<Bonificacion[]>("/api/bonificaciones");
      setBonificaciones(response.data);
    } catch (error) {
      console.error("Error al cargar bonificaciones:", error);
    }
  }, []);

  useEffect(() => {
    fetchBonificaciones();
  }, [fetchBonificaciones]);

  // 🔄 Paginación segura
  const pageCount = Math.ceil(bonificaciones.length / itemsPerPage);
  const currentItems = bonificaciones.slice(
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
      <h1 className="page-title">Bonificaciones</h1>

      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/bonificaciones/formulario")}>
          Registrar Nueva Bonificación
        </Boton>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={[
            "ID",
            "Descripción",
            "Descuento (%)",
            "Activo",
            "Acciones",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/bonificaciones/formulario?id=${fila.id}`)
                }
                secondary
                aria-label={`Editar bonificación ${fila.descripcion}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar bonificación ${fila.descripcion}`}
              >
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.descripcion,
            fila.porcentajeDescuento,
            fila.activo ? "Sí" : "No",
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

export default Bonificaciones;
