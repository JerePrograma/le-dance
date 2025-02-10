import { useState, useEffect } from "react";
import api from "../api/axiosConfig";
import Tabla from "./comunes/Tabla";
import Boton from "./comunes/Boton";
import { X } from "lucide-react";

interface ModalEntidadProps<T> {
  entidad: string;
  cerrarModal: () => void;
  mapearColumnas?: (item: T) => Record<string, unknown>;
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

  const obtenerColumnas = (item: T): string[] =>
    mapearColumnas ? Object.keys(mapearColumnas(item)) : Object.keys(item);

  const obtenerValores = (item: T): unknown[] =>
    mapearColumnas ? Object.values(mapearColumnas(item)) : Object.values(item);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-background rounded-lg shadow-lg max-w-4xl w-full max-h-[90vh] overflow-auto">
        <div className="p-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-2xl font-bold text-foreground">{`Gestión de ${entidad}`}</h2>
            <Boton
              onClick={cerrarModal}
              className="p-2 rounded-full hover:bg-accent hover:text-accent-foreground"
              aria-label="Cerrar"
            >
              <X className="h-6 w-6" />
            </Boton>
          </div>
          {loading ? (
            <p className="text-center py-4">Cargando...</p>
          ) : (
            datos.length > 0 && (
              <Tabla
                encabezados={obtenerColumnas(datos[0])}
                datos={datos}
                extraRender={(fila) => obtenerValores(fila).map(String)}
              />
            )
          )}
        </div>
      </div>
    </div>
  );
};

export default ModalEntidad;
