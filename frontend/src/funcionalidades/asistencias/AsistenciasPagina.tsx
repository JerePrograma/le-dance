import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Asistencia {
  id: number;
  alumno: string;
  disciplina: string;
  fecha: string;
  presente: boolean;
}

const Asistencias = () => {
  const [asistencias, setAsistencias] = useState<Asistencia[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchAsistencias = async () => {
      try {
        const response = await api.get("/api/asistencias");
        setAsistencias(response.data);
      } catch (error) {
        console.error("Error al cargar asistencias:", error);
      }
    };
    fetchAsistencias();
  }, []);

  const encabezados = [
    "ID",
    "Alumno",
    "Disciplina",
    "Fecha",
    "Presente",
    "Acciones",
  ];

  const handleNuevaAsistencia = () => {
    navigate("/asistencias/formulario");
  };

  const handleEditarAsistencia = (id: number) => {
    navigate(`/asistencias/formulario?id=${id}`);
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleNuevaAsistencia} className="page-button">
          Registrar Nueva Asistencia
        </button>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={encabezados}
          datos={asistencias}
          acciones={(fila) => (
            <div className="flex gap-2">
              <button
                onClick={() => handleEditarAsistencia(fila.id)}
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

export default Asistencias;
