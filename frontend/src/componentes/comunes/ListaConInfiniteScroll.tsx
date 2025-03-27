// ListaConInfiniteScroll.tsx
import React from "react";
import InfiniteScroll from "./InfiniteScroll"; // Tu componente base de infinite scroll

export interface ListaConInfiniteScrollProps {
  onLoadMore: () => void;
  hasMore: boolean;
  loading: boolean;
  className?: string;
  children: React.ReactNode;
}

const ListaConInfiniteScroll: React.FC<ListaConInfiniteScrollProps> = ({
  onLoadMore,
  hasMore,
  loading,
  className,
  children,
}) => {
  // Simplemente renderiza el InfiniteScroll pasándole las props
  return (
    <InfiniteScroll
      onLoadMore={onLoadMore}
      hasMore={hasMore}
      loading={loading}
      className={className}
    >
      {children}
    </InfiniteScroll>
  );
};

export default ListaConInfiniteScroll;
