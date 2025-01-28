import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Alumno {
  id: number;
  nombre: string;
  apellido?: string;
  edad?: number;
  activo?: boolean;
}

const Alumnos = () => {
  const [alumnos, setAlumnos] = useState<Alumno[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchAlumnos = async () => {
      try {
        const response = await api.get("/api/alumnos");
        setAlumnos(response.data);
      } catch (error) {
        console.error("Error al cargar alumnos:", error);
      }
    };
    fetchAlumnos();
  }, []);

  const handleNuevoAlumno = () => navigate("/alumnos/formulario");
  const handleEditarAlumno = (id: number) =>
    navigate(`/alumnos/formulario?id=${id}`);

  return (
    <div className="page-container">
      <h1 className="page-title">Alumnos</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleNuevoAlumno} className="page-button">
          Ficha de Alumnos
        </button>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={[
            "ID",
            "Nombre",
            "Apellido",
            "Edad",
            "Activo",
            "Acciones",
          ]}
          datos={alumnos}
          acciones={(fila) => (
            <>
              <button
                onClick={() => handleEditarAlumno(fila.id)}
                className="page-button bg-blue-500 hover:bg-blue-600 ml-0"
              >
                Editar
              </button>
              <button className="page-button bg-red-500 hover:bg-red-600 ml-2">
                Eliminar
              </button>
            </>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.nombre,
            fila.apellido ?? "",
            fila.edad ?? "",
            fila.activo ? "Sí" : "No",
          ]}
        />
      </div>
    </div>
  );
};

export default Alumnos;
