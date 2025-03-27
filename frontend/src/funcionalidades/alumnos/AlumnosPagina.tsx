"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import alumnosApi from "../../api/alumnosApi";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, CreditCard, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import InfiniteScroll from "../../componentes/comunes/InfiniteScroll";

interface AlumnoListado {
  id: number;
  nombre: string;
  apellido: string;
}

const Alumnos = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListado[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Estados para búsqueda y ordenación
  const [searchTerm, setSearchTerm] = useState("");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");
  const itemsPerLoad = 5;
  // visibleCount controla cuántos elementos se muestran
  const [visibleCount, setVisibleCount] = useState<number>(itemsPerLoad);
  const navigate = useNavigate();

  const fetchAlumnos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await alumnosApi.listar();
      setAlumnos(response);
    } catch (error) {
      toast.error("Error al cargar alumnos:");
      setError("Error al cargar alumnos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlumnos();
  }, [fetchAlumnos]);

  // Filtrar y ordenar alumnos según la búsqueda y orden seleccionado
  const alumnosFiltradosYOrdenados = useMemo(() => {
    const filtrados = alumnos.filter((alumno) => {
      const nombreCompleto =
        `${alumno.nombre} ${alumno.apellido}`.toLowerCase();
      return nombreCompleto.includes(searchTerm.toLowerCase());
    });
    return filtrados.sort((a, b) => {
      const nombreA = `${a.nombre} ${a.apellido}`.toLowerCase();
      const nombreB = `${b.nombre} ${b.apellido}`.toLowerCase();
      return sortOrder === "asc"
        ? nombreA.localeCompare(nombreB)
        : nombreB.localeCompare(nombreA);
    });
  }, [alumnos, searchTerm, sortOrder]);

  // Cada vez que cambian los filtros, reiniciamos la cantidad visible
  useEffect(() => {
    setVisibleCount(itemsPerLoad);
  }, [alumnosFiltradosYOrdenados, itemsPerLoad]);

  const currentItems = useMemo(
    () => alumnosFiltradosYOrdenados.slice(0, visibleCount),
    [alumnosFiltradosYOrdenados, visibleCount]
  );

  const hasMore = visibleCount < alumnosFiltradosYOrdenados.length;

  // Función para incrementar la cantidad de elementos visibles
  const loadMore = useCallback(() => {
    setVisibleCount((prev) =>
      Math.min(prev + itemsPerLoad, alumnosFiltradosYOrdenados.length)
    );
  }, [itemsPerLoad, alumnosFiltradosYOrdenados.length]);

  const eliminarAlumno = async (id: number) => {
    try {
      await alumnosApi.eliminar(id);
      toast.success("Alumno eliminado correctamente.");
      // Actualizamos la lista después de eliminar
      fetchAlumnos();
    } catch (error) {
      toast.error("Error al eliminar el Alumno.");
    }
  };

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  // Opciones únicas para el datalist de búsqueda
  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(
      alumnos.map((alumno) => `${alumno.nombre} ${alumno.apellido}`)
    );
    return Array.from(nombresSet);
  }, [alumnos]);

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Alumnos</h1>
        <Boton
          onClick={() => navigate("/alumnos/formulario")}
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          aria-label="Ficha de Alumnos"
        >
          <PlusCircle className="w-5 h-5" />
          Ficha de Alumnos
        </Boton>
      </div>

      {/* Controles de búsqueda y orden */}
      <div className="flex flex-col sm:flex-row sm:justify-between gap-4">
        <div>
          <label htmlFor="search" className="mr-2 font-medium">
            Buscar por nombre:
          </label>
          <input
            id="search"
            list="nombres"
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Escribe o selecciona un nombre..."
            className="border rounded px-2 py-1"
          />
          <datalist id="nombres">
            {nombresUnicos.map((nombre) => (
              <option key={nombre} value={nombre} />
            ))}
          </datalist>
        </div>
        <div>
          <label htmlFor="sortOrder" className="mr-2 font-medium">
            Orden:
          </label>
          <select
            id="sortOrder"
            value={sortOrder}
            onChange={(e) => setSortOrder(e.target.value as "asc" | "desc")}
            className="border rounded px-2 py-1"
          >
            <option value="asc">Ascendente</option>
            <option value="desc">Descendente</option>
          </select>
        </div>
      </div>

      <div className="rounded-lg border bg-card text-card-foreground shadow-sm">
        <Tabla
          headers={["ID", "Nombre", "Apellido", "Acciones"]}
          data={currentItems}
          customRender={(fila: AlumnoListado) => [
            fila.id,
            fila.nombre,
            fila.apellido,
          ]}
          actions={(fila: AlumnoListado) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/alumnos/formulario?id=${fila.id}`)}
                className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                aria-label={`Editar alumno ${fila.nombre} ${fila.apellido}`}
              >
                <Pencil className="w-4 h-4" />
                Editar
              </Boton>
              <Boton
                onClick={() => navigate(`/cobranza/${fila.id}`)}
                className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                aria-label={`Ver cobranza consolidada de ${fila.nombre} ${fila.apellido}`}
              >
                <CreditCard className="w-4 h-4" />
                Cobranza
              </Boton>
              <Boton
                onClick={() => eliminarAlumno(fila.id)}
                className="inline-flex items-center bg-destructive text-destructive-foreground hover:bg-destructive/90"
                aria-label={`Eliminar alumno ${fila.nombre} ${fila.apellido}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
        />

        {hasMore && (
          <div className="py-4 border-t">
            <InfiniteScroll
              onLoadMore={loadMore}
              hasMore={hasMore}
              loading={loading}
              className="justify-center w-full"
              children={undefined}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default Alumnos;
