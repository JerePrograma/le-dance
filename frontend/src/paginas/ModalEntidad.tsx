import { useState, useEffect } from "react";
import api from "../utilidades/axiosConfig";

interface ModalEntidadProps<T> {
  entidad: string;
  cerrarModal: () => void;
  mapearColumnas?: (item: T) => Record<string, unknown>; // Personalización de columnas
}

const ModalEntidad = <T extends object>({
  entidad,
  cerrarModal,
  mapearColumnas,
}: ModalEntidadProps<T>) => {
  const [datos, setDatos] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await api.get(`/api/${entidad.toLowerCase()}`);
        setDatos(response.data);
      } catch (error) {
        console.error("Error al obtener datos:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [entidad]);

  // Obtener nombres de columnas desde los datos o usando mapearColumnas
  const obtenerColumnas = (item: T): string[] =>
    mapearColumnas ? Object.keys(mapearColumnas(item)) : Object.keys(item);

  // Obtener valores de filas desde los datos o usando mapearColumnas
  const obtenerValores = (item: T): unknown[] =>
    mapearColumnas ? Object.values(mapearColumnas(item)) : Object.values(item);

  return (
    <div className="modal">
      <div className="modal-contenido">
        <h2>{`Gestión de ${entidad}`}</h2>
        <button className="modal-cerrar" onClick={cerrarModal}>
          Cerrar
        </Boton>
        {loading ? (
          <p>Cargando...</p>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                {datos.length > 0 &&
                  obtenerColumnas(datos[0]).map((columna) => (
                    <th key={columna}>{columna}</th>
                  ))}
              </tr>
            </thead>
            <tbody>
              {datos.map((fila, index) => (
                <tr key={index}>
                  {obtenerValores(fila).map((valor, i) => (
                    <td key={i}>{String(valor)}</td> // Convertimos valores desconocidos a string
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default ModalEntidad;
