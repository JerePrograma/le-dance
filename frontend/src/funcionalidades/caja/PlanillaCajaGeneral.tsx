import React, { useState } from "react";
import cajaApi from "../../api/cajaApi";  // ← Simula tus llamadas a CajaServicio
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";

interface CajaDiariaDTO {
    fecha: string;             // "2025-02-26"
    rangoRecibos: string;      // "19270-19282", etc.
    totalEfectivo: number;
    totalDebito: number;
    totalEgresos: number;
    totalNeto: number;
}

const PlanillaCajaGeneral: React.FC = () => {
    // Estados para manejar fechas de filtro
    const [startDate, setStartDate] = useState<string>(new Date().toISOString().split("T")[0]);
    const [endDate, setEndDate] = useState<string>(new Date().toISOString().split("T")[0]);
    const [lista, setLista] = useState<CajaDiariaDTO[]>([]);
    const [loading, setLoading] = useState(false);

    // Calculos de totales (Efectivo, Transferencia) al final
    const totalEfectivo = lista.reduce((sum, item) => sum + item.totalEfectivo, 0);
    const totalTransf = lista.reduce((sum, item) => sum + item.totalDebito, 0);
    const totalFinal = totalEfectivo + totalTransf  // o neto, segun tu gusto

    // Funcion para consultar la API al hacer click en "Ver"
    const handleVer = async () => {
        try {
            setLoading(true);
            const data: CajaDiariaDTO[] = await cajaApi.obtenerPlanillaGeneral(startDate, endDate);
            setLista(data);
        } catch (err) {
            toast.error("Error al cargar Planilla de Caja General");
        } finally {
            setLoading(false);
        }
    };

    const handleImprimir = () => {
        // Tu logica de impresion
        toast.info("Funcionalidad de imprimir (no implementada)");
    };

    const handleExportar = () => {
        // Tu logica de exportar a Excel/CSV
        toast.info("Funcionalidad de exportar (no implementada)");
    };

    return (
        <div className="page-container p-4">
            <h1 className="text-2xl font-bold mb-4">Planilla de Caja General</h1>

            {/* Filtros de Fechas */}
            <div className="flex gap-4 items-center mb-4">
                <div>
                    <label className="block font-medium">Desde</label>
                    <input
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        className="border p-2"
                    />
                </div>
                <div>
                    <label className="block font-medium">Hasta</label>
                    <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        className="border p-2"
                    />
                </div>
                <Boton onClick={handleVer} className="bg-green-500 text-white p-2 rounded">
                    Ver
                </Boton>
            </div>

            {/* Tabla principal */}
            <div className="border p-2">
                {loading ? (
                    <p>Cargando...</p>
                ) : (
                    <Tabla
                        headers={["Fecha", "R.Desde-Hasta", "Efectivo", "Transferencia", "Total"]}
                        data={lista}
                        customRender={(item: CajaDiariaDTO) => [
                            // Convertir "AAAA-MM-DD" a "DD/MM/AAAA" si deseas
                            new Date(item.fecha).toLocaleDateString("es-AR"),
                            item.rangoRecibos,
                            item.totalEfectivo.toLocaleString(),
                            item.totalDebito.toLocaleString(),
                            item.totalNeto.toLocaleString()
                        ]}
                    />
                )}
            </div>

            {/* Footer con botones y totales */}
            <div className="mt-4 flex justify-between items-center">
                <div className="flex gap-2">
                    <Boton onClick={handleImprimir} className="bg-pink-400 text-white p-2 rounded">
                        Imprimir
                    </Boton>
                    <Boton onClick={handleExportar} className="bg-green-400 text-white p-2 rounded">
                        Exportar
                    </Boton>
                </div>
                <div className="text-right">
                    <p>Total Efectivo: ${totalEfectivo.toLocaleString()}</p>
                    <p>Total Transferencia: ${totalTransf.toLocaleString()}</p>
                    <p><strong>Total:</strong> ${totalFinal.toLocaleString()}</p>
                </div>
            </div>
        </div>
    );
};

export default PlanillaCajaGeneral;
