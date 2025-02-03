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
      <table className="table" role="table">
        <thead role="rowgroup">
          <tr>
            {encabezados.map((encabezado, idx) => (
              <th key={idx}>{encabezado}</th>
            ))}
            {acciones && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody role="rowgroup">
          {datos.length > 0 ? (
            datos.map((fila, index) => (
              <tr key={index}>
                {extraRender
                  ? extraRender(fila).map((valor, idx) => (
                      <td key={idx}>{valor}</td>
                    ))
                  : Object.values(fila).map((valor, idx) => (
                      <td key={idx}>{valor}</td>
                    ))}
                {acciones && (
                  <td>
                    <div className="flex gap-2">{acciones(fila)}</div>
                  </td>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td
                colSpan={encabezados.length + (acciones ? 1 : 0)}
                className="text-center text-[color:var(--foreground)]/60 dark:text-text-dark/60"
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
