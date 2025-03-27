"use client";

import React, { useRef, useEffect } from "react";

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
  /** Umbral para el IntersectionObserver (por defecto 0.1) */
  threshold?: number;
  /** Margen del root para el IntersectionObserver (por defecto '200px' para cargar con antelación) */
  rootMargin?: string;
}

const InfiniteScroll: React.FC<InfiniteScrollProps> = ({
  onLoadMore,
  hasMore,
  loading,
  children,
  className,
  threshold = 0.1,
  rootMargin = "200px",
}) => {
  const sentinelRef = useRef<HTMLDivElement>(null);

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
        root: null,
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
  }, [loading, hasMore, onLoadMore, threshold, rootMargin]);

  return (
    <div className={className}>
      {children}
      {loading && (
        <div className="py-4 text-center text-sm text-muted-foreground">
          Cargando...
        </div>
      )}
      {/* Elemento sentinel para detectar el final del scroll */}
      <div ref={sentinelRef} />
    </div>
  );
};

export default InfiniteScroll;
