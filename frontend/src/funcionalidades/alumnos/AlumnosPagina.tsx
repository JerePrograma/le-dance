import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CreditCard, Pencil, PlusCircle, Trash2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import alumnosApi from "../../api/alumnosApi";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import { queryKeys } from "../../hooks/queryKeys";

const PAGE_SIZE = 50;

const AlumnosPagina = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const alumnos = useQuery({
    queryKey: queryKeys.alumnos(page, PAGE_SIZE, search),
    queryFn: () => search.trim() ? alumnosApi.buscarPorNombre(search.trim(), page, PAGE_SIZE) : alumnosApi.listar(page, PAGE_SIZE),
  });
  const baja = async (id: number) => {
    try {
      await alumnosApi.darBaja(id);
      await queryClient.invalidateQueries({ queryKey: ["alumnos"] });
      toast.success("Alumno dado de baja.");
    } catch {
      toast.error("No se pudo dar de baja el alumno.");
    }
  };

  if (alumnos.isLoading) return <div className="text-center py-4">Cargando...</div>;
  if (alumnos.isError) return <div className="text-center py-4 text-destructive">No se pudieron cargar alumnos.</div>;
  return <div className="page-container">
    <div className="flex justify-between"><div><h1 className="page-title">Alumnos</h1><p>{alumnos.data?.totalElements ?? 0} registros</p></div><Boton onClick={() => navigate("/alumnos/formulario")} className="page-button"><PlusCircle className="w-4 h-4" /> Nuevo</Boton></div>
    <input className="form-input max-w-md my-4" placeholder="Buscar por nombre" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} />
    <div className="page-card"><Tabla headers={["ID", "Nombre", "Apellido", "Estado"]} data={alumnos.data?.content ?? []}
      customRender={(row) => [row.id, row.nombre, row.apellido, row.activo ? "Activo" : "Baja"]}
      actions={(row) => <div className="flex gap-2"><Boton onClick={() => navigate(`/alumnos/formulario?id=${row.id}`)} className="page-button-secondary"><Pencil className="w-4 h-4" /> Editar</Boton><Boton onClick={() => navigate(`/cobranza/${row.id}`)} className="page-button-secondary"><CreditCard className="w-4 h-4" /> Cobranza</Boton>{row.activo && <Boton onClick={() => baja(row.id)} className="page-button-danger"><Trash2 className="w-4 h-4" /> Baja</Boton>}</div>} /></div>
    <div className="mt-4"><Boton disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="page-button-secondary">Anterior</Boton><span> Página {page + 1} de {Math.max(alumnos.data?.totalPages ?? 1, 1)} </span><Boton disabled={!alumnos.data || page + 1 >= alumnos.data.totalPages} onClick={() => setPage((value) => value + 1)} className="page-button-secondary">Siguiente</Boton></div>
  </div>;
};

export default AlumnosPagina;
