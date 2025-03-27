"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import conceptosApi from "../../api/conceptosApi";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import type { ConceptoResponse } from "../../types/types";
import { toast } from "react-toastify";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";

const itemsPerPage = 5;

const ConceptosPagina = () => {
  const [conceptos, setConceptos] = useState<ConceptoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Se utiliza visibleCount en lugar de currentPage para determinar cuántos elementos se muestran
  const [visibleCount, setVisibleCount] = useState(itemsPerPage);
  const navigate = useNavigate();

  const fetchConceptos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await conceptosApi.listarConceptos();
      setConceptos(response);
    } catch (error) {
      toast.error("Error al cargar conceptos:");
      setError("Error al cargar conceptos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConceptos();
  }, [fetchConceptos]);

  // Se obtiene un subconjunto de los conceptos a mostrar
  const currentItems = useMemo(
    () => conceptos.slice(0, visibleCount),
    [conceptos, visibleCount]
  );

  // Determina si hay más elementos para cargar
  const hasMore = useMemo(
    () => visibleCount < conceptos.length,
    [visibleCount, conceptos.length]
  );

  // Incrementa la cantidad visible en bloques
  const onLoadMore = useCallback(() => {
    if (hasMore) {
      setVisibleCount((prev) => prev + itemsPerPage);
    }
  }, [hasMore]);

  const handleEliminarConcepto = async (id: number) => {
    try {
      await conceptosApi.eliminarConcepto(id);
      toast.success("Concepto eliminado correctamente.");
      fetchConceptos();
    } catch (error) {
      toast.error("Error al eliminar el concepto.");
    }
  };

  if (loading && conceptos.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Conceptos</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/conceptos/formulario-concepto")}
          className="page-button"
          aria-label="Registrar nuevo concepto"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Concepto
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          headers={["ID", "Descripcion", "Precio", "SubConcepto", "Acciones"]}
          data={currentItems}
          actions={(fila: ConceptoResponse) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/conceptos/formulario-concepto?id=${fila.id}`)
                }
                className="page-button-secondary"
                aria-label={`Editar concepto ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar concepto ${fila.descripcion}`}
                onClick={() => handleEliminarConcepto(fila.id)}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          customRender={(fila: ConceptoResponse) => [
            fila.id,
            fila.descripcion,
            fila.precio,
            fila.subConcepto.descripcion,
          ]}
        />
      </div>
      {hasMore && (
        <div className="mt-4">
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

export default ConceptosPagina;
