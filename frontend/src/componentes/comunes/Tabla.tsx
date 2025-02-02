interface TablaProps<T extends Record<string, any>> {
  encabezados: string[];
  datos: T[];
  acciones?: (fila: T) => JSX.Element;
  extraRender?: (fila: T) => (string | number | JSX.Element)[];
}

const Tabla = <T extends Record<string, any>>({
  encabezados,
  datos,
  acciones,
  extraRender,
}: TablaProps<T>) => {
  return (
    <div className="overflow-x-auto w-full">
      <table
        className="w-full border-collapse bg-background rounded-lg shadow-md"
        role="table"
      >
        <thead role="rowgroup">
          <tr className="bg-primary text-primary-foreground text-left">
            {encabezados.map((encabezado, idx) => (
              <th key={idx} className="p-3 font-semibold border border-border">
                {encabezado}
              </th>
            ))}
            {acciones && (
              <th className="p-3 font-semibold border border-border">
                Acciones
              </th>
            )}
          </tr>
        </thead>
        <tbody role="rowgroup">
          {datos.length > 0 ? (
            datos.map((fila, index) => (
              <tr
                key={index}
                className="border-t border-border hover:bg-accent transition-colors"
              >
                {extraRender
                  ? extraRender(fila).map((valor, idx) => (
                      <td
                        key={idx}
                        className="p-3 border border-border text-foreground"
                      >
                        {valor}
                      </td>
                    ))
                  : Object.values(fila).map((valor, idx) => (
                      <td
                        key={idx}
                        className="p-3 border border-border text-foreground"
                      >
                        {valor}
                      </td>
                    ))}
                {acciones && (
                  <td className="p-3 border border-border">
                    <div className="flex gap-2">{acciones(fila)}</div>
                  </td>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td
                colSpan={encabezados.length + (acciones ? 1 : 0)}
                className="p-4 text-center text-muted-foreground"
              >
                No hay datos disponibles.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default Tabla;
