interface TablaProps {
  encabezados: string[];
  datos: any[];
  acciones?: (fila: any) => JSX.Element;
  extraRender?: (fila: any) => (string | number | JSX.Element)[]; // Aseguramos que solo devuelva valores válidos
}

const Tabla: React.FC<TablaProps> = ({
  encabezados,
  datos,
  acciones,
  extraRender,
}) => {
  return (
    <div className="overflow-x-auto w-full">
      <table className="w-full border-collapse bg-white dark:bg-gray-800 rounded-lg shadow-md">
        <thead>
          <tr className="bg-blue-500 text-white text-left">
            {encabezados.map((encabezado, idx) => (
              <th
                key={idx}
                className="p-3 text-left font-semibold border border-gray-300 dark:border-gray-700"
              >
                {encabezado}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
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
                        {typeof valor === "string" || typeof valor === "number"
                          ? valor
                          : JSON.stringify(valor)}
                      </td>
                    ))
                  : Object.values(fila).map((valor, idx) => (
                      <td
                        key={idx}
                        className="p-3 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300"
                      >
                        {typeof valor === "string" || typeof valor === "number"
                          ? valor
                          : JSON.stringify(valor)}
                      </td>
                    ))}
                {acciones && (
                  <td className="p-3 border border-gray-300 dark:border-gray-700">
                    <div className="flex flex-wrap gap-2">{acciones(fila)}</div>
                  </td>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td
                colSpan={encabezados.length}
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
