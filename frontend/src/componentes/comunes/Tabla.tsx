// src/componentes/comunes/Tabla.tsx

// Se restringe T para que sea un objeto con claves de tipo string.
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
        className="w-full border-collapse bg-white dark:bg-gray-800 rounded-lg shadow-md"
        role="table"
      >
        <thead role="rowgroup">
          <tr className="bg-blue-500 text-white text-left">
            {encabezados.map((encabezado, idx) => (
              <th
                key={idx}
                className="p-3 font-semibold border border-gray-300 dark:border-gray-700"
              >
                {encabezado}
              </th>
            ))}
            {acciones && (
              <th className="p-3 font-semibold border border-gray-300 dark:border-gray-700">
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
                className="border-t border-gray-200 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              >
                {extraRender
                  ? extraRender(fila).map((valor, idx) => (
                      <td
                        key={idx}
                        className="p-3 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300"
                      >
                        {valor}
                      </td>
                    ))
                  : Object.values(fila).map((valor, idx) => (
                      <td
                        key={idx}
                        className="p-3 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300"
                      >
                        {valor}
                      </td>
                    ))}
                {acciones && (
                  <td className="p-3 border border-gray-300 dark:border-gray-700">
                    <div className="flex gap-2">{acciones(fila)}</div>
                  </td>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td
                colSpan={encabezados.length + (acciones ? 1 : 0)}
                className="p-4 text-center text-gray-500 dark:text-gray-400"
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
