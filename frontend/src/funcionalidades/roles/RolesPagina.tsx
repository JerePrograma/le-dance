import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Rol {
  id: number;
  descripcion: string;
}

const RolesPagina = () => {
  const [roles, setRoles] = useState<Rol[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const response = await api.get("/api/roles");
        setRoles(response.data);
      } catch (error) {
        console.error("Error al cargar roles:", error);
      }
    };
    fetchRoles();
  }, []);

  const handleNuevoRol = () => navigate("/roles/formulario");
  const handleEditarRol = (id: number) =>
    navigate(`/roles/formulario?id=${id}`);

  return (
    <div className="page-container">
      <h1 className="page-title">Roles</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleNuevoRol} className="page-button">
          Registrar Nuevo Rol
        </button>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Descripción", "Acciones"]}
          datos={roles}
          acciones={(fila) => (
            <div className="flex gap-2">
              <button
                onClick={() => handleEditarRol(fila.id)}
                className="page-button bg-blue-500 hover:bg-blue-600"
              >
                Editar
              </button>
              <button className="page-button bg-red-500 hover:bg-red-600">
                Eliminar
              </button>
            </div>
          )}
        />
      </div>
    </div>
  );
};

export default RolesPagina;
