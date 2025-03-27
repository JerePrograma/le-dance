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
}

const InfiniteScroll: React.FC<InfiniteScrollProps> = ({
  onLoadMore,
  hasMore,
  loading,
  children,
  className,
}) => {
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!sentinelRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !loading && hasMore) {
          onLoadMore();
        }
      },
      {
        root: null,
        rootMargin: "300px", // Aumentamos el margen
        threshold: 0.1,
      }
    );

    observer.observe(sentinelRef.current);

    return () => {
      if (sentinelRef.current) {
        observer.unobserve(sentinelRef.current);
      }
    };
  }, [loading, hasMore, onLoadMore]);

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
