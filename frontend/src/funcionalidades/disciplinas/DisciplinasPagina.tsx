import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";

interface Disciplina {
  id: number;
  nombre: string;
  horario: string;
}

const Disciplinas = () => {
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDisciplinas = async () => {
      try {
        const response = await api.get("/api/disciplinas");
        setDisciplinas(response.data);
      } catch (error) {
        console.error("Error al cargar disciplinas:", error);
      }
    };
    fetchDisciplinas();
  }, []);

  const handleNuevaDisciplina = () => navigate("/disciplinas/formulario");
  const handleEditarDisciplina = (id: number) =>
    navigate(`/disciplinas/formulario?id=${id}`);

  return (
    <div className="page-container">
      <h1 className="page-title">Disciplinas</h1>

      <div className="flex justify-end mb-4">
        <button onClick={handleNuevaDisciplina} className="page-button">
          Registrar Nueva Disciplina
        </button>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Nombre", "Horario", "Acciones"]}
          datos={disciplinas}
          acciones={(fila) => (
            <>
              <button
                onClick={() => handleEditarDisciplina(fila.id)}
                className="page-button bg-blue-500 hover:bg-blue-600"
              >
                Editar
              </button>
              <button className="page-button bg-red-500 hover:bg-red-600 ml-2">
                Eliminar
              </button>
            </>
          )}
          extraRender={(fila) => [fila.id, fila.nombre, fila.horario]}
        />
      </div>
    </div>
  );
};

export default Disciplinas;
