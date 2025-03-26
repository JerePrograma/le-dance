"use client";

import { useCallback } from "react";
import Boton from "../../componentes/comunes/Boton";
import mensualidadesApi from "../../api/mensualidadesApi";
import { toast } from "react-toastify";

const MensualidadesPagina = () => {
  // Función placeholder para refrescar las inscripciones
  const fetchInscripciones = useCallback(() => {
    // Aquí iría la lógica para obtener las inscripciones
  }, []);

  const handleGenerarCuotas = async () => {
    try {
      const respuestas =
        await mensualidadesApi.generarMensualidadesParaMesVigente();
      toast.success(
        `Se generaron/actualizaron ${respuestas.length} cuota(s) para el mes vigente.`
      );
      fetchInscripciones();
    } catch (error) {
      toast.error("Error al generar cuotas del mes.");
    }
  };

  return (
    <div className="container mx-auto p-6">
      <Boton
        onClick={handleGenerarCuotas}
        className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
        aria-label="Generar cuotas del mes"
      >
        Generar Cuotas del Mes
      </Boton>
    </div>
  );
};

export default MensualidadesPagina;
