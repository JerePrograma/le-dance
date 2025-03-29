"use client";

import React, {
  useEffect,
  useState,
  useCallback,
  useMemo,
  useRef,
} from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import alumnosApi from "../../api/alumnosApi";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, CreditCard, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";

interface AlumnoListado {
  id: number;
  nombre: string;
  apellido: string;
}

const itemsPerPage = 15;
const estimatedRowHeight = 60;

const Alumnos: React.FC = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListado[]>([]);
  const [loading, setLoading] = useState(false);
  const [, setError] = useState<string | null>(null);
  const [visibleCount, setVisibleCount] = useState<number>(itemsPerPage);

  const [searchTerm, setSearchTerm] = useState("");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");

  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const fetchAlumnos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await alumnosApi.listar();
      setAlumnos(response);
      setVisibleCount(itemsPerPage);
    } catch (error) {
      toast.error("Error al cargar alumnos.");
      setError("Error al cargar alumnos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlumnos();
  }, [fetchAlumnos]);

  const alumnosFiltrados = useMemo(() => {
    return alumnos
      .filter((alumno) =>
        `${alumno.nombre} ${alumno.apellido}`
          .toLowerCase()
          .includes(searchTerm.toLowerCase())
      )
      .sort((a, b) => {
        const nombreA = `${a.nombre} ${a.apellido}`.toLowerCase();
        const nombreB = `${b.nombre} ${b.apellido}`.toLowerCase();
        return sortOrder === "asc"
          ? nombreA.localeCompare(nombreB)
          : nombreB.localeCompare(nombreA);
      });
  }, [alumnos, searchTerm, sortOrder]);

  const currentItems = useMemo(
    () => alumnosFiltrados.slice(0, visibleCount),
    [alumnosFiltrados, visibleCount]
  );

  const hasMore = visibleCount < alumnosFiltrados.length;

  const loadMore = useCallback(() => {
    setVisibleCount((prev) =>
      Math.min(prev + itemsPerPage, alumnosFiltrados.length)
    );
  }, [alumnosFiltrados.length]);

  useEffect(() => {
    const adjustVisibleCount = () => {
      if (containerRef.current) {
        const containerHeight =
          containerRef.current.getBoundingClientRect().height;
        const itemsThatFit = Math.ceil(containerHeight / estimatedRowHeight);
        setVisibleCount(itemsThatFit);
      }
    };

    adjustVisibleCount();
    window.addEventListener("resize", adjustVisibleCount);
    return () => window.removeEventListener("resize", adjustVisibleCount);
  }, []);

  const eliminarAlumno = async (id: number) => {
    try {
      await alumnosApi.eliminar(id);
      toast.success("Alumno eliminado correctamente.");
      fetchAlumnos();
    } catch (error) {
      toast.error("Error al eliminar alumno.");
    }
  };

  return (
    <div ref={containerRef} className="flex flex-col h-screen overflow-hidden">
      <div className="flex-none p-6 pb-2 flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">Alumnos</h1>
        <Boton
          onClick={() => navigate("/alumnos/formulario")}
          className="inline-flex gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
        >
          <PlusCircle className="w-5 h-5" />
          Ficha de Alumnos
        </Boton>
      </div>

      <div className="flex-none px-6 pb-4">
        <div className="page-card flex gap-4">
          <input
            type="text"
            placeholder="Buscar por nombre..."
            className="border rounded p-2"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <select
            className="border rounded p-2"
            value={sortOrder}
            onChange={(e) => setSortOrder(e.target.value as "asc" | "desc")}
          >
            <option value="asc">Ascendente</option>
            <option value="desc">Descendente</option>
          </select>
        </div>
      </div>

      <div className="flex-grow px-6 overflow-hidden">
        <div className="page-card h-full">
          <ListaConInfiniteScroll
            onLoadMore={loadMore}
            hasMore={hasMore}
            loading={loading}
            fillAvailable={true}
          >
            <Tabla
              headers={["ID", "Nombre", "Apellido"]}
              data={currentItems}
              customRender={(fila) => [fila.id, fila.nombre, fila.apellido]}
              actions={(fila) => (
                <div className="flex gap-2">
                  <Boton
                    onClick={() =>
                      navigate(`/alumnos/formulario?id=${fila.id}`)
                    }
                    className="bg-secondary"
                  >
                    <Pencil className="w-4 h-4" /> Editar
                  </Boton>
                  <Boton
                    onClick={() => navigate(`/cobranza/${fila.id}`)}
                    className="bg-secondary"
                  >
                    <CreditCard className="w-4 h-4" /> Cobranza
                  </Boton>
                  <Boton
                    onClick={() => eliminarAlumno(fila.id)}
                    className="bg-destructive"
                  >
                    <Trash2 className="w-4 h-4" /> Eliminar
                  </Boton>
                </div>
              )}
            />
          </ListaConInfiniteScroll>
        </div>
      </div>
    </div>
  );
};

export default Alumnos;
