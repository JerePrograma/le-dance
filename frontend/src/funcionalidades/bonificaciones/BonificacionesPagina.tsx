import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Bonificacion {
  id: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

const Bonificaciones = () => {
  const [bonificaciones, setBonificaciones] = useState<Bonificacion[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchBonificaciones = async () => {
      try {
        const response = await api.get("/api/bonificaciones");
        setBonificaciones(response.data);
      } catch (error) {
        console.error("Error al cargar bonificaciones:", error);
      }
    };
    fetchBonificaciones();
  }, []);

  const handleNuevaBonificacion = () => navigate("/bonificaciones/formulario");
  const handleEditarBonificacion = (id: number) =>
    navigate(`/bonificaciones/formulario?id=${id}`);

  return (
    <div className="page-container">
      <h1 className="page-title">Bonificaciones</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleNuevaBonificacion} className="page-button">
          Registrar Nueva Bonificación
        </button>
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
          datos={bonificaciones}
          acciones={(fila) => (
            <div className="flex gap-2">
              <button
                onClick={() => handleEditarBonificacion(fila.id)}
                className="page-button bg-blue-500 hover:bg-blue-600"
              >
                Editar
              </button>
              <button className="page-button bg-red-500 hover:bg-red-600">
                Eliminar
              </button>
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
    </div>
  );
};

export default Bonificaciones;
