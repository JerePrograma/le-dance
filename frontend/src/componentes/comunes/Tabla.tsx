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
    <table className="w-full border-collapse rounded-lg overflow-hidden bg-white dark:bg-gray-800">
      <thead>
        <tr className="bg-blue-500 text-white">
          {encabezados.map((encabezado, idx) => (
            <th key={idx} className="p-4 text-left font-medium">
              {encabezado}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {datos.map((fila, index) => (
          <tr
            key={index}
            className="border-t border-gray-200 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            {extraRender
              ? extraRender(fila).map((valor, idx) => (
                  <td key={idx} className="p-4">
                    {typeof valor === "string" || typeof valor === "number"
                      ? valor
                      : JSON.stringify(valor)}{" "}
                    {/* ✅ Si es un objeto lo convierte en string */}
                  </td>
                ))
              : Object.values(fila).map((valor, idx) => (
                  <td key={idx} className="p-4">
                    {typeof valor === "string" || typeof valor === "number"
                      ? valor
                      : JSON.stringify(valor)}
                  </td>
                ))}
            {acciones && <td className="p-4 flex gap-2">{acciones(fila)}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default Tabla;
