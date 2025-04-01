"use client";

import React from "react";
import InfiniteScroll from "./InfiniteScroll";

export interface ListaConInfiniteScrollProps {
  onLoadMore: () => void;
  hasMore: boolean;
  loading: boolean;
  className?: string;
  children: React.ReactNode;
  /** Si es true, el contenedor ocupará todo el espacio disponible */
  fillAvailable?: boolean;
  /** Altura máxima del contenedor (solo se usa si fillAvailable es false) */
  maxHeight?: string;
}

const ListaConInfiniteScroll: React.FC<ListaConInfiniteScrollProps> = ({
  onLoadMore,
  hasMore,
  loading,
  className = "",
  children,
  fillAvailable = true,
  maxHeight,
}) => {
  return (
    <div className={`fade-in ${className}`}>
      <InfiniteScroll
        onLoadMore={onLoadMore}
        hasMore={hasMore}
        loading={loading}
        fillAvailable={fillAvailable}
        maxHeight={maxHeight}
      >
        {children}
      </InfiniteScroll>
    </div>
  );
};

export default ListaConInfiniteScroll;