/*import { useEffect, useState } from "react";
import Tarjeta from "./Tarjeta";
import api from "../axiosConfig";

interface TarjetaData {
  titulo: string;
  valor: number;
  descripcion: string;
}

const Tablero = () => {
  const [tarjetas, setTarjetas] = useState<TarjetaData[]>([]);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const response = await api.get("/metricas"); // Endpoint para obtener metricas
        setTarjetas(response.data);
      } catch (error) {
        console.error("Error al cargar las metricas:", error);
      }
    };

    fetchMetrics();
  }, []);

  return (
    <div className="tablero">
      {tarjetas.map((tarjeta, index) => (
        <Tarjeta
          key={index}
          titulo={tarjeta.titulo}
          valor={tarjeta.valor}
          descripcion={tarjeta.descripcion}
        />
      ))}
    </div>
  );
};

export default Tablero;
*/
