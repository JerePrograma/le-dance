/*import { useEffect, useState } from "react";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../axiosConfig";

interface Pago {
  id: number;
  alumno: string;
  fecha: string;
  monto: number;
  metodo: string;
}

const Pagos = () => {
  const [pagos, setPagos] = useState<Pago[]>([]);
  const encabezados = ["ID", "Alumno", "Fecha", "Monto", "Metodo", "Acciones"];

  useEffect(() => {
    const fetchPagos = async () => {
      try {
        const response = await api.get("/pagos"); // Endpoint para obtener pagos
        setPagos(response.data);
      } catch (error) {
        console.error("Error al cargar los pagos:", error);
      }
    };

    fetchPagos();
  }, []);

  return (
    <div>
      <h1 className="titulo-principal">Gestion de Pagos</h1>
      <button className="boton">Registrar Nuevo Pago</Boton>
      <Tabla
        encabezados={encabezados}
        datos={pagos.map((pago) => ({
          id: pago.id,
          alumno: pago.alumno,
          fecha: pago.fecha,
          monto: pago.monto,
          metodo: pago.metodo,
          acciones: <button className="boton">Detalles</Boton>,
        }))}
      />
    </div>
  );
};

export default Pagos;
*/
