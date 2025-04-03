"use client";

import React, { useState } from "react";
import cajaApi from "../../api/cajaApi";
import egresoApi from "../../api/egresosApi";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import { useAuth } from "../../hooks/context/authContext";

// Interfaces (pueden moverse a un archivo .d.ts o adaptarse)
interface MetodoPago {
  id: number;
  descripcion: string; // "EFECTIVO", "DEBITO", etc.
}

interface PagoDelDia {
  id: number;
  alumno: {
    id: number;
    nombre: string;
    apellido: string;
  };
  observaciones: string;
  monto: number;
  metodoPago?: MetodoPago | null;
  usuarioId: number; // Identificador del usuario que realizó el pago
}

interface EgresoDelDia {
  id: number;
  fecha: string;
  monto: number;
  observaciones?: string;
}

export interface CajaDetalleDTO {
  pagosDelDia: PagoDelDia[];
  egresosDelDia: EgresoDelDia[];
}

export interface EgresoRegistroRequest {
  id?: number;
  fecha: string; // Formato ISO (ej: "2025-03-17")
  monto: number;
  observaciones?: string;
  metodoPagoId: number;
}

const ConsultaCajaDiaria: React.FC = () => {
  // Obtenemos el usuario autenticado a través del contexto.
  // Recordá que el objeto usuario se envía con la propiedad "nombreUsuario"
  // según el login, por lo que usaremos user.nombreUsuario al mostrarlo.
  const { user } = useAuth();
  const currentUserId = user?.id || 0;

  // Obtener la fecha actual en formato "YYYY-MM-DD" en GMT-3 (zona de Buenos Aires)
  const fechaAuto = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date());
  const [fecha, setFecha] = useState<string>(fechaAuto);

  const [data, setData] = useState<CajaDetalleDTO | null>(null);
  const [loading, setLoading] = useState(false);

  // Estado para el filtro de pagos: "mis" o "todos"
  // Ahora, en el select mostraremos el nombre del usuario para "mis pagos"
  const [filtroPago, setFiltroPago] = useState<"mis" | "todos">("mis");

  // Estados del Modal de Egreso
  const [showModalEgreso, setShowModalEgreso] = useState(false);
  const [montoEgreso, setMontoEgreso] = useState<number>(0);
  const [obsEgreso, setObsEgreso] = useState<string>("");

  // --------------------------------------------------------------------------
  // Función para cargar la caja diaria (pagos y egresos) en la fecha seleccionada
  // --------------------------------------------------------------------------
  const handleVer = async () => {
    try {
      setLoading(true);
      const detalle = await cajaApi.obtenerCajaDiaria(fecha);
      // Asegurarse que cada objeto tenga la estructura esperada
      const mappedDetalle: CajaDetalleDTO = {
        ...detalle,
        pagosDelDia: detalle.pagosDelDia.map((p: any) => ({
          ...p,
          alumno: p.alumno ?? { id: 0, nombre: "", apellido: "" },
        })),
        egresosDelDia: detalle.egresosDelDia.map((egreso: any) => ({
          ...egreso,
          observaciones: egreso.observaciones ?? "",
        })),
      };
      setData(mappedDetalle);
    } catch (err) {
      toast.error("Error al consultar la caja diaria.");
    } finally {
      setLoading(false);
    }
  };

  // --------------------------------------------------------------------------
  // Filtrar pagos para excluir aquellos con monto = 0
  // --------------------------------------------------------------------------
  const pagos: PagoDelDia[] = data?.pagosDelDia || [];
  const pagosFiltrados = pagos.filter((p) => p.monto !== 0);

  // Ordenar los pagos (los más recientes primero)
  const sortedPagos = [...pagosFiltrados].sort((a, b) => b.id - a.id);

  // Aplicar filtro según opción seleccionada: "mis" pagos o "todos"
  const pagosFiltradosPorUsuario =
    filtroPago === "mis"
      ? sortedPagos.filter((p) => p.usuarioId === currentUserId)
      : sortedPagos;

  // --------------------------------------------------------------------------
  // Calcular totales por método de pago y totales generales en base a pagos filtrados
  // --------------------------------------------------------------------------
  const totalEfectivo = pagosFiltradosPorUsuario
    .filter((p) => p.metodoPago?.descripcion?.toUpperCase() === "EFECTIVO")
    .reduce((sum, p) => sum + p.monto, 0);

  const totalDebito = pagosFiltradosPorUsuario
    .filter((p) => p.metodoPago?.descripcion?.toUpperCase() === "DEBITO")
    .reduce((sum, p) => sum + p.monto, 0);

  const totalCobrado = totalEfectivo + totalDebito;

  // Egresos
  const egresos: EgresoDelDia[] = data?.egresosDelDia || [];
  const sortedEgresos = [...egresos].sort((a, b) => b.id - a.id);
  const totalEgresos = egresos.reduce((sum, e) => sum + e.monto, 0);

  // --------------------------------------------------------------------------
  // Acciones extra (por ejemplo, Imprimir)
  // --------------------------------------------------------------------------
  const handleImprimir = () => {
    toast.info("Funcionalidad de imprimir no implementada");
  };

  // --------------------------------------------------------------------------
  // Abrir y Cerrar Modal de Egreso
  // --------------------------------------------------------------------------
  const handleAbrirModalEgreso = () => {
    setMontoEgreso(0);
    setObsEgreso("");
    setShowModalEgreso(true);
  };

  const handleCerrarModal = () => {
    setShowModalEgreso(false);
  };

  // --------------------------------------------------------------------------
  // Guardar (crear) Egreso
  // --------------------------------------------------------------------------
  const handleGuardarEgreso = async () => {
    try {
      if (!montoEgreso) {
        toast.error("El monto del egreso no puede ser 0.");
        return;
      }
      const egresoRequest: EgresoRegistroRequest = {
        fecha,
        monto: montoEgreso,
        observaciones: obsEgreso,
        metodoPagoId: 1, // Hardcodeado a EFECTIVO
      };
      await egresoApi.registrarEgreso(egresoRequest);
      toast.success("Egreso agregado correctamente.");
      setShowModalEgreso(false);
      // Refrescar la caja diaria
      handleVer();
    } catch (err) {
      toast.error("Error al agregar Egreso.");
    }
  };

  // --------------------------------------------------------------------------
  // Renderizado
  // --------------------------------------------------------------------------
  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Consulta de Caja</h1>
      <div className="flex gap-4 items-end mb-4">
        <div>
          <label className="block font-medium">Fecha:</label>
          <input
            type="date"
            className="border p-2"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
        </div>
        <div>
          <label className="block font-medium">Mostrar:</label>
          <select
            className="border p-2"
            value={filtroPago}
            onChange={(e) => setFiltroPago(e.target.value as "mis" | "todos")}
          >
            {/* Si es "mis", mostramos el nombre del usuario autenticado */}
            <option value="mis">
              {user ? `Pagos de ${user.nombreUsuario}` : "Mis pagos"}
            </option>
            <option value="todos">Todos</option>
          </select>
        </div>
        <Boton
          onClick={handleVer}
          className="bg-green-500 text-white p-2 rounded"
        >
          Ver
        </Boton>
      </div>

      {/* Tabla de Pagos */}
      <div className="border p-2">
        {loading ? (
          <p>Cargando...</p>
        ) : (
          <Tabla
            headers={["Recibo", "Código", "Alumno", "Observaciones", "Importe"]}
            data={pagosFiltradosPorUsuario}
            customRender={(p: PagoDelDia) => [
              p.id,
              p.alumno?.id || "",
              p.alumno ? `${p.alumno.nombre} ${p.alumno.apellido}` : "",
              p.observaciones,
              p.monto.toLocaleString(),
            ]}
            emptyMessage="No hay pagos para el día"
          />
        )}
      </div>

      {/* Tabla de Egresos */}
      {egresos.length > 0 && (
        <div className="border p-2 mt-4">
          <h2 className="font-semibold mb-2">Egresos del día</h2>
          <Tabla
            headers={["ID", "Observaciones", "Monto"]}
            data={sortedEgresos}
            customRender={(e: EgresoDelDia) => [
              e.id,
              e.observaciones,
              e.monto.toLocaleString(),
            ]}
            emptyMessage="No hay egresos para el día"
          />
        </div>
      )}

      {/* Botones finales */}
      <div className="mt-4 flex gap-2">
        <Boton
          onClick={handleImprimir}
          className="bg-pink-400 text-white p-2 rounded"
        >
          Imprimir
        </Boton>
        <Boton
          onClick={handleAbrirModalEgreso}
          className="bg-yellow-400 text-white p-2 rounded"
        >
          Agregar Egreso
        </Boton>
      </div>

      {/* Totales */}
      <div className="text-right mt-2">
        <p>
          Efectivo: ${" "}
          {pagosFiltradosPorUsuario
            .filter(
              (p) => p.metodoPago?.descripcion?.toUpperCase() === "EFECTIVO"
            )
            .reduce((sum, p) => sum + p.monto, 0)
            .toLocaleString()}
        </p>
        <p>
          Débito: ${" "}
          {pagosFiltradosPorUsuario
            .filter(
              (p) => p.metodoPago?.descripcion?.toUpperCase() === "DEBITO"
            )
            .reduce((sum, p) => sum + p.monto, 0)
            .toLocaleString()}
        </p>
        <p>Total neto: $ {(totalCobrado - totalEgresos).toLocaleString()}</p>
        <p>Egresos en efectivo: $ {totalEgresos.toLocaleString()}</p>
        <p>
          Total efectivo: $ {(totalEfectivo - totalEgresos).toLocaleString()}
        </p>
      </div>

      {/* Modal para Agregar Egreso */}
      {showModalEgreso && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-30">
          <div className="bg-black p-4 rounded shadow-md w-[300px]">
            <h2 className="text-xl font-bold mb-4">Nuevo Egreso</h2>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full"
                value={montoEgreso}
                onChange={(e) => setMontoEgreso(parseFloat(e.target.value))}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Observaciones:</label>
              <input
                type="text"
                className="border p-2 w-full"
                value={obsEgreso}
                onChange={(e) => setObsEgreso(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Boton onClick={handleCerrarModal}>Cancelar</Boton>
              <Boton onClick={handleGuardarEgreso}>Guardar</Boton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ConsultaCajaDiaria;
