/*import { useEffect, useState } from "react";
import api from "../api/axiosConfig";

interface Reporte {
  id: number;
  tipo: string;
  descripcion: string;
}

const Reportes = () => {
  const [reportes, setReportes] = useState<Reporte[]>([]);

  useEffect(() => {
    const fetchReportes = async () => {
      try {
        const response = await api.get("/api/reportes"); // Endpoint para obtener reportes
        setReportes(response.data);
      } catch (error) {
        console.error("Error al cargar los reportes:", error);
      }
    };

    fetchReportes();
  }, []);

  return (
    <div>
      <h1 className="text-3xl font-bold mb-4">Reportes</h1>
      <table className="w-full border-collapse border border-gray-300">
        <thead>
          <tr>
            <th className="border border-gray-300 p-2">ID</th>
            <th className="border border-gray-300 p-2">Tipo</th>
            <th className="border border-gray-300 p-2">Descripción</th>
          </tr>
        </thead>
        <tbody>
          {reportes.map((reporte) => (
            <tr key={reporte.id} className="odd:bg-gray-100">
              <td className="border border-gray-300 p-2">{reporte.id}</td>
              <td className="border border-gray-300 p-2">{reporte.tipo}</td>
              <td className="border border-gray-300 p-2">
                {reporte.descripcion}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default Reportes;
*/
