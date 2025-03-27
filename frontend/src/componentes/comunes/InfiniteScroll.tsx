"use client";

import type React from "react";
import { useRef, useEffect, useState } from "react";

interface InfiniteScrollProps {
  /** Función a ejecutar para cargar más elementos */
  onLoadMore: () => void;
  /** Bandera que indica si aún quedan elementos por cargar */
  hasMore: boolean;
  /** Bandera que indica si se están cargando elementos */
  loading: boolean;
  /** Contenido que se desea renderizar (lista de elementos) */
  children: React.ReactNode;
  /** Clases opcionales para el contenedor */
  className?: string;
  /** Umbral para el IntersectionObserver (por defecto 0.2) */
  threshold?: number;
  /** Margen del root para el IntersectionObserver (por defecto '300px' para cargar con antelación) */
  rootMargin?: string;
  /** Altura máxima del contenedor (por defecto 'auto' para adaptarse al contenido) */
  maxHeight?: string;
  /** Si es true, el contenedor ocupará todo el espacio disponible */
  fillAvailable?: boolean;
}

const InfiniteScroll: React.FC<InfiniteScrollProps> = ({
  onLoadMore,
  hasMore,
  loading,
  children,
  className = "",
  threshold = 0.2,
  rootMargin = "300px",
  maxHeight = "auto",
  fillAvailable = false,
}) => {
  const sentinelRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [containerHeight, setContainerHeight] = useState<string>(maxHeight);

  // Efecto para ajustar la altura del contenedor si fillAvailable es true
  useEffect(() => {
    if (fillAvailable && containerRef.current) {
      const updateHeight = () => {
        const container = containerRef.current;
        if (!container) return;

        // Calculamos la posición del contenedor respecto al viewport
        const rect = container.getBoundingClientRect();
        const topPosition = rect.top;

        // Calculamos el espacio disponible hasta el final de la ventana
        // Restamos un pequeño margen para evitar scroll innecesario
        const availableHeight = window.innerHeight - topPosition - 20;

        // Aseguramos que la altura mínima sea razonable
        const newHeight = Math.max(300, availableHeight);
        setContainerHeight(`${newHeight}px`);
      };

      // Actualizamos la altura inicialmente y en cada resize
      updateHeight();
      window.addEventListener("resize", updateHeight);

      return () => {
        window.removeEventListener("resize", updateHeight);
      };
    } else {
      setContainerHeight(maxHeight);
    }
  }, [fillAvailable, maxHeight]);

  // Efecto para el IntersectionObserver
  useEffect(() => {
    if (!sentinelRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore) {
          onLoadMore();
        }
      },
      {
        root: fillAvailable ? containerRef.current : null, // Usar el contenedor como root si es scrollable
        rootMargin,
        threshold,
      }
    );

    observer.observe(sentinelRef.current);

    return () => {
      if (sentinelRef.current) {
        observer.unobserve(sentinelRef.current);
      }
    };
  }, [loading, hasMore, onLoadMore, threshold, rootMargin, fillAvailable]);

  return (
    <div
      ref={containerRef}
      className={`relative ${className}`}
      style={{
        height: containerHeight,
        overflowY: fillAvailable ? "auto" : "visible",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div className="flex-grow">{children}</div>

      {/* Elemento sentinel para detectar el final del scroll */}
      <div
        ref={sentinelRef}
        className="w-full h-4"
        style={{ visibility: hasMore ? "visible" : "hidden" }}
      />

      {/* Indicador de carga */}
      {loading && (
        <div className="py-3 text-center text-sm text-muted-foreground flex items-center justify-center gap-2 sticky bottom-0 bg-background/80 backdrop-blur-sm">
          <div className="w-4 h-4 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
          <span>Cargando...</span>
        </div>
      )}
    </div>
  );
};

export default InfiniteScroll;
