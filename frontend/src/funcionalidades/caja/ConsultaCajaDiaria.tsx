import React, { useState } from "react";
import cajaApi from "../../api/cajaApi";
import egresoApi from "../../api/egresosApi";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";

// Interfaces (puedes moverlas a un archivo .d.ts o adaptarlas)
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
  metodoPago?: MetodoPago | null; // ahora es opcional
}

interface EgresoDelDia {
  id: number;
  fecha: string;
  monto: number;
  observaciones?: string; // Ahora es opcional
}

interface CajaDetalleDTO {
  pagosDelDia: PagoDelDia[];
  egresosDelDia: EgresoDelDia[];
}

// EgresoRegistroRequest según lo definido
export interface EgresoRegistroRequest {
  id?: number; // Opcional, ya que se genera automáticamente
  fecha: string; // en formato ISO (ej. "2025-03-17")
  monto: number;
  observaciones?: string;
  metodoPagoId: number;
}

const ConsultaCajaDiaria: React.FC = () => {
  const fechaAuto = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date());
  const [fecha, setFecha] = useState<string>(fechaAuto);

  const [data, setData] = useState<CajaDetalleDTO | null>(null);
  const [loading, setLoading] = useState(false);

  // Estados del Modal de Egreso
  const [showModalEgreso, setShowModalEgreso] = useState(false);
  const [montoEgreso, setMontoEgreso] = useState<number>(0);
  const [obsEgreso, setObsEgreso] = useState<string>("");

  // --------------------------------------------------------------------------
  // Función para cargar la info del día (Pagos y Egresos)
  // --------------------------------------------------------------------------
  const handleVer = async () => {
    try {
      setLoading(true);
      const detalle = await cajaApi.obtenerCajaDiaria(fecha);

      // Mapear pagosDelDia para asegurarse que cada objeto tenga la estructura esperada
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
  // Calcular totales por método de pago
  // --------------------------------------------------------------------------
  const pagos = data?.pagosDelDia || [];

  const totalEfectivo = pagos
    .filter((p) => p.metodoPago?.descripcion?.toUpperCase() === "EFECTIVO")
    .reduce((sum, p) => sum + p.monto, 0);

  const totalDebito = pagos
    .filter((p) => p.metodoPago?.descripcion?.toUpperCase() === "DEBITO")
    .reduce((sum, p) => sum + p.monto, 0);

  const totalCobrado = totalEfectivo + totalDebito;

  // Egresos
  const egresos = data?.egresosDelDia || [];
  const totalEgresos = egresos.reduce((sum, e) => sum + e.monto, 0);

  // --------------------------------------------------------------------------
  // Acciones extra (Imprimir, etc.)
  // --------------------------------------------------------------------------
  const handleImprimir = () => {
    toast.info("Funcionalidad de imprimir no implementada");
  };

  // --------------------------------------------------------------------------
  // Abrir y Cerrar el modal para Agregar Egreso
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
  // Guardar (crear) Egreso en backend usando el endpoint registrarEgreso
  // --------------------------------------------------------------------------
  const handleGuardarEgreso = async () => {
    try {
      if (!montoEgreso) {
        toast.error("El monto del egreso no puede ser 0.");
        return;
      }

      // Se crea el objeto de egreso, fijando el metodoPagoId a 1 (EFECTIVO)
      const egresoRequest: EgresoRegistroRequest = {
        fecha,
        monto: montoEgreso,
        observaciones: obsEgreso,
        metodoPagoId: 1, // Hardcodeado a EFECTIVO
      };

      // Llamar al endpoint registrarEgreso
      await egresoApi.registrarEgreso(egresoRequest);

      toast.success("Egreso agregado correctamente.");

      // Cerrar el modal
      setShowModalEgreso(false);

      // Refrescar la caja diaria
      handleVer();
    } catch (err) {
      toast.error("Error al agregar Egreso.");
    }
  };

  const sortedPagos = [...pagos].sort((a, b) => b.id - a.id);
  const sortedEgresos = [...egresos].sort((a, b) => b.id - a.id);

  // --------------------------------------------------------------------------
  // Render
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
            headers={["Recibo", "Codigo", "Alumno", "Observaciones", "Importe"]}
            data={sortedPagos}
            customRender={(p: PagoDelDia) => [
              p.id, // Recibo
              p.alumno?.id || "",
              p.alumno ? `${p.alumno.nombre} ${p.alumno.apellido}` : "",
              p.observaciones,
              p.monto.toLocaleString(),
            ]}
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

        {/* Botón para Agregar Egreso */}
        <Boton
          onClick={handleAbrirModalEgreso}
          className="bg-yellow-400 text-white p-2 rounded"
        >
          Agregar Egreso
        </Boton>
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
      {/* Totales al final */}
      <div className="text-right mt-2">
        <p>Efectivo: $ {totalEfectivo.toLocaleString()}</p>
        <p>Débito: $ {totalDebito.toLocaleString()}</p>
        <p>Total neto: $ {(totalCobrado - totalEgresos).toLocaleString()}</p>
        <p>Egresos en efectivo: $ {totalEgresos.toLocaleString()}</p>
        <p>
          Total efectivo: $ {(totalEfectivo - totalEgresos).toLocaleString()}
        </p>
      </div>
    </div>
  );
};

export default ConsultaCajaDiaria;
