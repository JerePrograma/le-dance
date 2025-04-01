"use client";

import type { ReactNode } from "react";
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
  className?: string;
}

const Tabla = <T extends Record<string, any>>({
  headers,
  data,
  actions,
  customRender,
  footer,
  emptyMessage = "No hay datos disponibles",
  className = "",
}: TablaProps<T>) => {
  return (
    <div className={`w-full h-full flex flex-col ${className}`}>
      {/* Versión para pantallas medianas y grandes */}
      <div className="hidden sm:block rounded-lg border h-full overflow-auto">
        <Table>
          <TableHeader className="sticky top-0 bg-background/95 backdrop-blur-sm z-10">
            <TableRow className="border-b border-border hover:bg-transparent">
              {headers.map((header, idx) => (
                <TableHead
                  key={idx}
                  className="text-center whitespace-nowrap px-fluid-2 py-fluid-1 text-muted-foreground font-medium"
                >
                  {header}
                </TableHead>
              ))}
              {actions && (
                <TableHead className="text-center whitespace-nowrap px-fluid-2 py-fluid-1 text-muted-foreground font-medium">
                  Acciones
                </TableHead>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.length > 0 ? (
              data.map((row, index) => (
                <TableRow
                  key={index}
                  className="border-b border-border/50 transition-colors hover:bg-muted/30"
                >
                  {customRender
                    ? customRender(row).map((value, idx) => (
                        <TableCell
                          key={idx}
                          className="text-center whitespace-nowrap px-fluid-2 py-fluid-1"
                        >
                          {value}
                        </TableCell>
                      ))
                    : Object.values(row).map((value, idx) => (
                        <TableCell
                          key={idx}
                          className="text-center whitespace-nowrap px-fluid-2 py-fluid-1"
                        >
                          {typeof value === "object" ? value : String(value)}
                        </TableCell>
                      ))}
                  {actions && (
                    <TableCell className="text-center whitespace-nowrap px-fluid-2 py-fluid-1">
                      <div className="flex items-center justify-center gap-fluid-sm">
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
      </div>

      {/* Versión para móviles (cards en lugar de tabla) */}
      <div className="sm:hidden space-y-fluid-md overflow-auto h-full">
        {data.length > 0 ? (
          data.map((row, rowIndex) => (
            <div key={rowIndex} className="card card-hover">
              {headers.map((header, headerIndex) => {
                const value = customRender
                  ? customRender(row)[headerIndex]
                  : Object.values(row)[headerIndex];
                return (
                  <div
                    key={headerIndex}
                    className="flex flex-col items-center justify-center whitespace-nowrap mb-fluid-2 last:mb-0"
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
                <div className="pt-fluid-2 mt-fluid-2 border-t border-border">
                  <span className="text-xs font-medium text-muted-foreground block text-center">
                    Acciones
                  </span>
                  <div className="mt-2 flex flex-wrap justify-center gap-fluid-sm">
                    {actions(row)}
                  </div>
                </div>
              )}
            </div>
          ))
        ) : (
          <div className="card text-center p-fluid text-muted-foreground">
            {emptyMessage}
          </div>
        )}
      </div>
    </div>
  );
};

export default Tabla;
