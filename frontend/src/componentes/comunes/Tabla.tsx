import type { ReactNode } from "react"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableFooter } from "../ui/table"

interface TablaProps<T extends Record<string, any>> {
  encabezados: string[]
  datos: T[]
  acciones?: (fila: T) => ReactNode
  extraRender?: (fila: T) => (string | number | ReactNode)[]
  tfoot?: ReactNode
}

const Tabla = <T extends Record<string, any>>({ encabezados, datos, acciones, extraRender, tfoot }: TablaProps<T>) => {
  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            {encabezados.map((encabezado, idx) => (
              <TableHead key={idx}>{encabezado}</TableHead>
            ))}
            {acciones && <TableHead>Acciones</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {datos.length > 0 ? (
            datos.map((fila, index) => (
              <TableRow key={index}>
                {extraRender
                  ? extraRender(fila).map((valor, idx) => <TableCell key={idx}>{valor}</TableCell>)
                  : Object.values(fila).map((valor, idx) => <TableCell key={idx}>{valor}</TableCell>)}
                {acciones && (
                  <TableCell>
                    <div className="flex items-center gap-2">{acciones(fila)}</div>
                  </TableCell>
                )}
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell
                colSpan={encabezados.length + (acciones ? 1 : 0)}
                className="h-24 text-center text-muted-foreground"
              >
                No hay datos disponibles.
              </TableCell>
            </TableRow>
          )}
        </TableBody>
        {tfoot && (
          <TableFooter>
            {tfoot}
          </TableFooter>
        )}
      </Table>
    </div>
  )
}

export default Tabla
