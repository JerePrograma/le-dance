import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api from "../../api/axiosConfig";
import { CobranzaDTO, DetalleCobranzaDTO } from "../../types/types";
import { toast } from "react-toastify";

const CobranzaPagina: React.FC = () => {
    const { alumnoId } = useParams<{ alumnoId: string }>();
    const [cobranza, setCobranza] = useState<CobranzaDTO | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!alumnoId) return;
        const fetchCobranza = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await api.get<CobranzaDTO>(`/pagos/alumno/${alumnoId}/cobranza`);
                setCobranza(response.data);
            } catch (err) {
                toast.error("Error al cargar la cobranza:");
                setError("Error al cargar la cobranza.");
            } finally {
                setLoading(false);
            }
        };

        fetchCobranza();
    }, [alumnoId]);

    if (loading) return <div className="text-center py-4">Cargando cobranza...</div>;
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Cobranza Consolidada</h1>
            {cobranza ? (
                <div className="page-card">
                    <h2>
                        Alumno: {cobranza.alumnoNombre} (ID: {cobranza.alumnoId})
                    </h2>
                    <p>
                        <strong>Total Pendiente:</strong> {cobranza.totalPendiente}
                    </p>
                    <table className="min-w-full border mt-4">
                        <thead>
                            <tr>
                                <th className="border p-2">Concepto</th>
                                <th className="border p-2">Pendiente</th>
                            </tr>
                        </thead>
                        <tbody>
                            {cobranza.detalles.map((detalle: DetalleCobranzaDTO, index: number) => (
                                <tr key={index}>
                                    <td className="border p-2">{detalle.concepto}</td>
                                    <td className="border p-2">{detalle.pendiente}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <p className="text-center py-4">No se encontro informacion de cobranza.</p>
            )}
        </div>
    );
};

export default CobranzaPagina;
