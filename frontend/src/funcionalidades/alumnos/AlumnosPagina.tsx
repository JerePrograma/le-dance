import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import alumnosApi from "../../utilidades/alumnosApi";

interface AlumnoListado {
  id: number;
  nombre: string;
  apellido: string;
}

const Alumnos = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListado[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchAlumnos = async () => {
      try {
        const response = await alumnosApi.listarAlumnos(); // 📌 Obtiene listado simplificado
        console.log("Datos recibidos de la API:", response); // 🛠️ Log para verificar los datos
        setAlumnos(response);
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
          encabezados={["ID", "Nombre", "Apellido", "Acciones"]}
          datos={alumnos}
          acciones={(fila) => (
            <div className="flex gap-2">
              <button
                onClick={() => handleEditarAlumno(fila.id)}
                className="page-button bg-blue-500 hover:bg-blue-600"
              >
                Editar
              </button>
              <button className="page-button bg-red-500 hover:bg-red-600">
                Eliminar
              </button>
            </div>
          )}
          extraRender={(fila) => [fila.id, fila.nombre, fila.apellido]} // 📌 Asegura que se rendericen los valores
        />
      </div>
    </div>
  );
};

export default Alumnos;
