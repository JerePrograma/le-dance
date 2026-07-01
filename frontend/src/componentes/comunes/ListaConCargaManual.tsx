"use client";

import type { ReactNode } from "react";

interface ListaConCargaManualProps {
  onLoadMore: () => void;
  hasMore: boolean;
  loading: boolean;
  children: ReactNode;
  className?: string;
  fillAvailable?: boolean;
  maxHeight?: string;
}

const ListaConCargaManual = ({
  onLoadMore,
  hasMore,
  loading,
  children,
  className = "",
  fillAvailable = false,
  maxHeight = "auto",
}: ListaConCargaManualProps) => (
  <div className={`fade-in ${className}`} style={{ maxHeight, overflowY: fillAvailable ? "auto" : "visible" }}>
    {children}
    {hasMore && (
      <button type="button" onClick={onLoadMore} disabled={loading} className="page-button-secondary mt-4">
        {loading ? "Cargando..." : "Mostrar más"}
      </button>
    )}
  </div>
);

export default ListaConCargaManual;
