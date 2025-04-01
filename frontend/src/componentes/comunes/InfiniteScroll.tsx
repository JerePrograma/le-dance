"use client";

import React, { useRef, useEffect, useState } from "react";

interface InfiniteScrollProps {
  /** Función a ejecutar para cargar más elementos */
  onLoadMore: () => void;
  /** Bandera que indica si aún quedan elementos por cargar */
  hasMore: boolean;
  /** Bandera que indica si se están cargando elementos */
  loading: boolean;
  /** Contenido a renderizar (lista de elementos) */
  children: React.ReactNode;
  /** Clases opcionales para el contenedor */
  className?: string;
  /** Umbral para el IntersectionObserver (por defecto 0.2) */
  threshold?: number;
  /** Margen del root para el IntersectionObserver (por defecto '300px') */
  rootMargin?: string;
  /** Altura máxima del contenedor (por defecto 'auto') */
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

  // Ajuste de altura si fillAvailable es true
  useEffect(() => {
    if (fillAvailable && containerRef.current) {
      const updateHeight = () => {
        const rect = containerRef.current!.getBoundingClientRect();
        const availableHeight = window.innerHeight - rect.top - 20;
        const newHeight = Math.max(300, availableHeight);
        setContainerHeight(`${newHeight}px`);
      };

      updateHeight();
      window.addEventListener("resize", updateHeight);
      return () => window.removeEventListener("resize", updateHeight);
    } else {
      setContainerHeight(maxHeight);
    }
  }, [fillAvailable, maxHeight]);

  // Configuración del IntersectionObserver
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
        // Si el contenedor es scrollable, usamos éste como raíz
        root: fillAvailable ? containerRef.current : null,
        rootMargin,
        threshold,
      }
    );

    observer.observe(sentinelRef.current);

    return () => {
      if (sentinelRef.current) observer.unobserve(sentinelRef.current);
    };
  }, [loading, hasMore, onLoadMore, threshold, rootMargin, fillAvailable]);

  return (
    <div
      ref={containerRef}
      className={`relative rounded-lg ${className}`}
      style={{
        height: containerHeight,
        overflowY: fillAvailable ? "auto" : "visible",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div className="flex-grow">{children}</div>

      {/* Sentinel para detectar el final */}
      <div
        ref={sentinelRef}
        className="w-full h-4"
        style={{ visibility: hasMore ? "visible" : "hidden" }}
      />

      {/* Indicador de carga */}
      {loading && (
        <div className="py-fluid-2 text-center text-sm text-muted-foreground flex items-center justify-center gap-fluid-sm sticky bottom-0 bg-background/80 backdrop-blur-sm border-t border-border/30">
          <div className="w-4 h-4 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
          <span>Cargando...</span>
        </div>
      )}
    </div>
  );
};

export default InfiniteScroll;