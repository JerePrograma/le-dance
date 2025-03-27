"use client";

// components/comunes/Tabla.tsx
import type { ReactNode } from "react";
import { useRef, useEffect } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableFooter,
} from "../ui/table";

interface TablaProps<T extends Record<string, any>> {
  headers: string[];
  data: T[];
  actions?: (row: T) => ReactNode;
  customRender?: (row: T) => (string | number | ReactNode)[];
  footer?: ReactNode;
  emptyMessage?: string;
  // Props para infinite scroll
  hasMore?: boolean;
  onLoadMore?: () => void;
  loading?: boolean;
  // Prop para altura personalizada
  className?: string;
}

const Tabla = <T extends Record<string, any>>({
  headers,
  data,
  actions,
  customRender,
  footer,
  emptyMessage = "No hay datos disponibles",
  hasMore = false,
  onLoadMore,
  loading = false,
  className = "",
}: TablaProps<T>) => {
  const sentinelRef = useRef<HTMLDivElement>(null);

  // Configurar IntersectionObserver para infinite scroll si es necesario
  useEffect(() => {
    if (!hasMore || !onLoadMore || !sentinelRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore) {
          onLoadMore();
        }
      },
      {
        root: null,
        rootMargin: "200px",
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
    <div className={`w-full h-full flex flex-col ${className}`}>
      {/* Versión para pantallas medianas y grandes */}
      <div className="hidden sm:block rounded-md border h-full overflow-auto">
        <Table>
          <TableHeader className="sticky top-0 bg-background z-10">
            <TableRow>
              {headers.map((header, idx) => (
                <TableHead
                  key={idx}
                  className="text-center whitespace-nowrap min-w-max px-2 py-1"
                >
                  {header}
                </TableHead>
              ))}
              {actions && (
                <TableHead className="text-center whitespace-nowrap min-w-max px-2 py-1">
                  Acciones
                </TableHead>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.length > 0 ? (
              data.map((row, index) => (
                <TableRow key={index}>
                  {customRender
                    ? customRender(row).map((value, idx) => (
                        <TableCell
                          key={idx}
                          className="text-center whitespace-nowrap px-2 py-1"
                        >
                          {value}
                        </TableCell>
                      ))
                    : Object.values(row).map((value, idx) => (
                        <TableCell
                          key={idx}
                          className="text-center whitespace-nowrap px-2 py-1"
                        >
                          {typeof value === "object" ? value : String(value)}
                        </TableCell>
                      ))}
                  {actions && (
                    <TableCell className="text-center whitespace-nowrap px-2 py-1">
                      <div className="flex items-center justify-center gap-2">
                        {actions(row)}
                      </div>
                    </TableCell>
                  )}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={headers.length + (actions ? 1 : 0)}
                  className="h-24 text-center text-muted-foreground"
                >
                  {emptyMessage}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
          {footer && <TableFooter>{footer}</TableFooter>}
        </Table>

        {/* Elemento sentinel para infinite scroll */}
        {hasMore && <div ref={sentinelRef} className="h-4 w-full" />}

        {/* Indicador de carga */}
        {loading && hasMore && (
          <div className="py-3 text-center text-sm text-muted-foreground flex items-center justify-center gap-2">
            <div className="w-4 h-4 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
            <span>Cargando más...</span>
          </div>
        )}
      </div>

      {/* Versión para móviles (cards en lugar de tabla) */}
      <div className="sm:hidden space-y-4 overflow-auto h-full">
        {data.length > 0 ? (
          data.map((row, rowIndex) => (
            <div
              key={rowIndex}
              className="bg-card rounded-lg border border-border p-4 space-y-3"
            >
              {headers.map((header, headerIndex) => {
                const value = customRender
                  ? customRender(row)[headerIndex]
                  : Object.values(row)[headerIndex];
                return (
                  <div
                    key={headerIndex}
                    className="flex flex-col items-center justify-center whitespace-nowrap"
                  >
                    <span className="text-xs font-medium text-muted-foreground">
                      {header}
                    </span>
                    <div className="mt-1 text-center">
                      {typeof value === "object" ? (
                        value
                      ) : (
                        <span className="text-sm">{String(value)}</span>
                      )}
                    </div>
                  </div>
                );
              })}
              {actions && (
                <div className="pt-2 mt-2 border-t border-border">
                  <span className="text-xs font-medium text-muted-foreground block text-center">
                    Acciones
                  </span>
                  <div className="mt-2 flex flex-wrap justify-center gap-2">
                    {actions(row)}
                  </div>
                </div>
              )}
            </div>
          ))
        ) : (
          <div className="text-center p-6 bg-card rounded-lg border border-border text-muted-foreground">
            {emptyMessage}
          </div>
        )}

        {/* Elemento sentinel para infinite scroll en móvil */}
        {hasMore && <div ref={sentinelRef} className="h-4 w-full" />}

        {/* Indicador de carga en móvil */}
        {loading && hasMore && (
          <div className="py-3 text-center text-sm text-muted-foreground flex items-center justify-center gap-2">
            <div className="w-4 h-4 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
            <span>Cargando más...</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default Tabla;
