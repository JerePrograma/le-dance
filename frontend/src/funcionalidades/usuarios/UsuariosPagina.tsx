import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
}

const UsuariosPagina = () => {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUsuarios = async () => {
      try {
        const response = await api.get("/api/usuarios");
        setUsuarios(response.data);
      } catch (error) {
        console.error("Error al cargar usuarios:", error);
      }
    };
    fetchUsuarios();
  }, []);

  const handleNuevoUsuario = () => navigate("/usuarios/formulario");
  const handleEliminarUsuario = async (id: number) => {
    const confirmacion = window.confirm(
      "¿Seguro que deseas eliminar este usuario?"
    );
    if (confirmacion) {
      try {
        await api.delete(`/api/usuarios/${id}`);
        setUsuarios((prev) => prev.filter((u) => u.id !== id));
      } catch (error) {
        console.error("Error al eliminar usuario:", error);
      }
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Usuarios</h1>

      {/* Grupo de botones en la parte superior */}
      <div className="page-button-group">
        <button onClick={handleNuevoUsuario} className="page-button">
          Registrar Nuevo Usuario
        </button>
      </div>

      {/* Tabla encapsulada en un contenedor con overflow-x */}
      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Nombre", "Correo", "Rol", "Acciones"]}
          datos={usuarios}
          acciones={(fila) => (
            <button
              onClick={() => handleEliminarUsuario(fila.id)}
              className="bg-red-500 text-white font-semibold rounded-lg px-4 py-2 hover:bg-red-600"
            >
              Eliminar
            </button>
          )}
        />
      </div>
    </div>
  );
};

export default UsuariosPagina;
