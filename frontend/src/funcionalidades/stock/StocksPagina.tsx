import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, PlusCircle, Trash2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import stocksApi from "../../api/stocksApi";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import { queryKeys } from "../../hooks/queryKeys";

const StocksPagina = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const stocks = useQuery({ queryKey: queryKeys.stocks(page), queryFn: () => stocksApi.listarStocks(page) });
  const eliminar = async (id: number) => {
    try {
      await stocksApi.eliminarStock(id);
      await queryClient.invalidateQueries({ queryKey: ["stocks"] });
      toast.success("Stock dado de baja.");
    } catch {
      toast.error("No se pudo dar de baja el stock.");
    }
  };

  if (stocks.isLoading) return <div className="text-center py-4">Cargando...</div>;
  if (stocks.isError) return <div className="text-center py-4 text-destructive">No se pudo cargar stock.</div>;
  return <div className="page-container">
    <div className="flex justify-between"><h1 className="page-title">Stocks</h1><Boton onClick={() => navigate("/stocks/formulario")} className="page-button"><PlusCircle className="w-4 h-4" /> Nuevo</Boton></div>
    <div className="page-card"><Tabla headers={["ID", "Nombre", "Precio", "Stock", "Activo"]} data={stocks.data?.content ?? []}
      customRender={(row) => [row.id, row.nombre, row.precio, row.stock, row.activo ? "Sí" : "No"]}
      actions={(row) => <div className="flex gap-2"><Boton onClick={() => navigate(`/stocks/formulario?id=${row.id}`)} className="page-button-secondary"><Pencil className="w-4 h-4" /> Editar</Boton><Boton onClick={() => eliminar(row.id)} className="page-button-danger"><Trash2 className="w-4 h-4" /> Baja</Boton></div>} /></div>
    <div className="mt-4"><Boton disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="page-button-secondary">Anterior</Boton><span> Página {page + 1} de {Math.max(stocks.data?.totalPages ?? 1, 1)} </span><Boton disabled={!stocks.data || page + 1 >= stocks.data.totalPages} onClick={() => setPage((value) => value + 1)} className="page-button-secondary">Siguiente</Boton></div>
  </div>;
};

export default StocksPagina;
