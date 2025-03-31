"use client";

import { useEffect, useState, useCallback, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import inscripcionesApi from "../../api/inscripcionesApi";
import type { InscripcionResponse } from "../../types/types";
import Boton from "../../componentes/comunes/Boton";
import { Pencil } from "lucide-react";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";

const estimatedRowHeight = 70; // Altura estimada por fila en píxeles

const InscripcionesPagina = () => {
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");
  const [visibleCount, setVisibleCount] = useState(0);
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);

  const fetchInscripciones = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await inscripcionesApi.listar();
      setInscripciones(data);
    } catch (error) {
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchInscripciones();
  }, [fetchInscripciones]);

  // Agrupa las inscripciones por alumno
  const gruposInscripciones = useMemo(() => {
    return inscripciones.reduce((acc, ins) => {
      const alumnoId = ins.alumno.id;
      if (!acc[alumnoId]) {
        acc[alumnoId] = { alumno: ins.alumno, inscripciones: [] };
      }
      acc[alumnoId].inscripciones.push(ins);
      return acc;
    }, {} as Record<number, { alumno: InscripcionResponse["alumno"]; inscripciones: InscripcionResponse[] }>);
  }, [inscripciones]);

  const gruposArray = useMemo(
    () => Object.values(gruposInscripciones),
    [gruposInscripciones]
  );

  const gruposConCosto = useMemo(() => {
    return gruposArray.map((grupo) => {
      const costoTotal = grupo.inscripciones.reduce(
        (sum, ins) =>
          sum +
          (ins.disciplina?.valorCuota || 0) -
          (ins.bonificacion?.valorFijo || 0) -
          ((ins.disciplina?.valorCuota || 0) *
            (ins.bonificacion?.porcentajeDescuento || 0)) /
            100,
        0
      );
      return { ...grupo, costoTotal };
    });
  }, [gruposArray]);

  const gruposFiltradosYOrdenados = useMemo(() => {
    const filtrados = gruposConCosto.filter((grupo) => {
      const nombreCompleto =
        `${grupo.alumno.nombre} ${grupo.alumno.apellido}`.toLowerCase();
      return nombreCompleto.includes(searchTerm.toLowerCase());
    });
    return filtrados.sort((a, b) => {
      const nombreA = `${a.alumno.nombre} ${a.alumno.apellido}`.toLowerCase();
      const nombreB = `${b.alumno.nombre} ${b.alumno.apellido}`.toLowerCase();
      return sortOrder === "asc"
        ? nombreA.localeCompare(nombreB)
        : nombreB.localeCompare(nombreA);
    });
  }, [gruposConCosto, searchTerm, sortOrder]);

  // Se obtiene el subconjunto de los grupos a mostrar según el visibleCount
  const currentItems = useMemo(
    () => gruposFiltradosYOrdenados.slice(0, visibleCount),
    [gruposFiltradosYOrdenados, visibleCount]
  );
  const hasMore = useMemo(
    () => visibleCount < gruposFiltradosYOrdenados.length,
    [visibleCount, gruposFiltradosYOrdenados.length]
  );

  // Calcula cuántos elementos caben en el contenedor y actualiza visibleCount
  const adjustVisibleCount = useCallback(() => {
    if (containerRef.current) {
      const containerHeight =
        containerRef.current.getBoundingClientRect().height;
      const itemsThatFit = Math.ceil(containerHeight / estimatedRowHeight);
      setVisibleCount(itemsThatFit);
    }
  }, []);

  useEffect(() => {
    // Ajusta la cantidad visible cuando se monta el componente o cambia el tamaño del contenedor
    adjustVisibleCount();
    // Opcional: Puedes agregar un listener para el resize
    window.addEventListener("resize", adjustVisibleCount);
    return () => window.removeEventListener("resize", adjustVisibleCount);
  }, [adjustVisibleCount]);

  // Incrementa visibleCount para cargar más elementos
  const onLoadMore = useCallback(() => {
    if (hasMore) {
      setVisibleCount((prev) => prev + estimatedRowHeight); // O sumar en bloques del mismo tamaño
    }
  }, [hasMore]);

  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(
      gruposConCosto.map(
        (grupo) => `${grupo.alumno.nombre} ${grupo.alumno.apellido}`
      )
    );
    return Array.from(nombresSet);
  }, [gruposConCosto]);

  // Reinicia visibleCount al cambiar el filtro
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
    adjustVisibleCount();
  };

  if (loading && inscripciones.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div ref={containerRef} className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Alumnos Activos</h1>
      </div>

      <div className="flex flex-col sm:flex-row sm:justify-between gap-4">
        <div className="flex items-center gap-4">
          <div>
            <label htmlFor="search" className="mr-2 font-medium">
              Buscar por nombre:
            </label>
            <input
              id="search"
              list="nombres"
              type="text"
              value={searchTerm}
              onChange={handleSearchChange}
              placeholder="Escribe o selecciona un nombre..."
              className="border rounded px-2 py-1"
            />
            <datalist id="nombres">
              {nombresUnicos.map((nombre) => (
                <option key={nombre} value={nombre} />
              ))}
            </datalist>
          </div>
          <div className="font-medium">
            Alumnos activos = {gruposFiltradosYOrdenados.length}
          </div>
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

      {gruposFiltradosYOrdenados.length === 0 ? (
        <div className="text-center py-4">
          No hay inscripciones disponibles.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <Tabla
            headers={["ID Alumno", "Nombre Alumno", "Costo Total", "Acciones"]}
            data={currentItems}
            customRender={(grupo) => [
              grupo.alumno.id,
              `${grupo.alumno.nombre} ${grupo.alumno.apellido}`,
              grupo.costoTotal.toFixed(2),
            ]}
            actions={(grupo) => (
              <div className="flex gap-2">
                <Boton
                  onClick={() =>
                    navigate(
                      `/inscripciones/formulario?alumnoId=${grupo.alumno.id}`
                    )
                  }
                  className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                  aria-label={`Ver detalles de ${grupo.alumno.nombre} ${grupo.alumno.apellido}`}
                >
                  <Pencil className="w-4 h-4" />
                  Ver Detalles
                </Boton>
              </div>
            )}
          />
        </div>
      )}

      {hasMore && (
        <div className="py-4 border-t">
          <ListaConInfiniteScroll
            onLoadMore={onLoadMore}
            hasMore={hasMore}
            loading={loading}
            className="justify-center w-full"
          >
            {loading && <div className="text-center py-2">Cargando más...</div>}
          </ListaConInfiniteScroll>
        </div>
      )}
    </div>
  );
};

export default InscripcionesPagina;
