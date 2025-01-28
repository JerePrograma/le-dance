import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Profesor {
  id: number;
  nombre: string;
  apellido: string;
  especialidad: string;
  aniosExperiencia: number;
}

const Profesores = () => {
  const [profesores, setProfesores] = useState<Profesor[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfesores = async () => {
      try {
        const response = await api.get("/api/profesores");
        setProfesores(response.data);
      } catch (error) {
        console.error("Error al cargar profesores:", error);
      }
    };
    fetchProfesores();
  }, []);

  const handleNuevoProfesor = () => navigate("/profesores/formulario");
  const handleEditarProfesor = (id: number) =>
    navigate(`/profesores/formulario?id=${id}`);

  return (
    <div className="page-container">
      <h1 className="page-title">Profesores</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleNuevoProfesor} className="page-button">
          Registrar Nuevo Profesor
        </button>
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
          datos={profesores}
          acciones={(fila) => (
            <div className="flex gap-2">
              <button
                onClick={() => handleEditarProfesor(fila.id)}
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

export default Profesores;
