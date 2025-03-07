"use client";
import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
// Se asume que tienes un API para Caja (cajaApi) que incluye la función para generar la rendición mensual
import cajaApi from "../../api/cajaApi";
import { RendicionDTO } from "../../types/types";

// Interfaz esperada en el componente
interface RendicionMensualDTO {
    mes: string; // Ejemplo: "Marzo 2025"
    totalIngresos: number;
    totalEgresos: number;
    totalNeto: number;
    detalles: Array<{
        concepto: string;
        monto: number;
    }>;
}

const RendicionMensual: React.FC = () => {
    const [rendicion, setRendicion] = useState<RendicionMensualDTO | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    // Función para obtener y transformar la rendición mensual desde el backend
    const fetchRendicion = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            // Se llama al endpoint para generar/obtener la rendición mensual (devuelve un objeto RendicionDTO)
            const data: RendicionDTO = await cajaApi.generarRendicionMensual();

            // Transformar el objeto recibido a RendicionMensualDTO:
            const mes = new Date().toLocaleString("default", { month: "long", year: "numeric" });
            const totalIngresos = data.totalEfectivo + data.totalDebito;
            const totalNeto = totalIngresos - data.totalEgresos;

            // Combinar pagos y egresos en un arreglo de detalles
            const detalles = [
                ...data.pagos.map((p) => ({
                    concepto: p.descripcion ? `Ingreso: ${p.descripcion}` : "Ingreso",
                    monto: p.monto,
                })),
                ...data.egresos.map((e) => ({
                    concepto: e.observaciones ? `Egreso: ${e.observaciones}` : "Egreso",
                    monto: e.monto,
                })),
            ];

            const transformed: RendicionMensualDTO = {
                mes,
                totalIngresos,
                totalEgresos: data.totalEgresos,
                totalNeto,
                detalles,
            };

            setRendicion(transformed);
            toast.success("Rendición mensual generada exitosamente.");
        } catch (err) {
            toast.error("Error al generar rendición mensual:");
            setError("Error al generar rendición mensual.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRendicion();
    }, [fetchRendicion]);

    if (loading)
        return <div className="text-center py-4">Generando rendición mensual...</div>;
    if (error)
        return <div className="text-center py-4 text-destructive">{error}</div>;
    if (!rendicion) return null;

    return (
        <div className="page-container p-6">
            <h1 className="text-3xl font-bold mb-4">Rendición Mensual</h1>
            <div className="mb-4">
                <p className="text-xl">Mes: {rendicion.mes}</p>
                <p>Total Ingresos: ${rendicion.totalIngresos.toLocaleString()}</p>
                <p>Total Egresos: ${rendicion.totalEgresos.toLocaleString()}</p>
                <p className="font-bold">
                    Total Neto: ${rendicion.totalNeto.toLocaleString()}
                </p>
            </div>
            <div className="overflow-x-auto">
                <Tabla
                    headers={["Concepto", "Monto"]}
                    data={rendicion.detalles}
                    customRender={(detalle) => [
                        detalle.concepto,
                        detalle.monto.toLocaleString(),
                    ]}
                />
            </div>
            <div className="mt-4 flex gap-4">
                <Boton
                    onClick={fetchRendicion}
                    className="bg-primary text-primary-foreground hover:bg-primary/90"
                >
                    Actualizar Rendición
                </Boton>
                <Boton
                    onClick={() => navigate("/caja")}
                    className="bg-secondary text-secondary-foreground hover:bg-secondary/90"
                >
                    Volver a Caja
                </Boton>
            </div>
        </div>
    );
};

export default RendicionMensual;
