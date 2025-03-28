// src/hooks/useSyncDetalles.ts
import { useEffect, useRef } from "react";
import { useFormikContext } from "formik";
import isEqual from "lodash/isEqual";
import type {
  CobranzasFormValues,
  DetallePagoRegistroRequestExt,
  DetallePagoResponse,
  AlumnoDataResponse,
} from "../../types/types";

/**
 * Transforma cada detalle obtenido del backend al formato usado por el formulario.
 */
const mapDetallePagos = (
  detallesPendientes: DetallePagoResponse[],
  currentDetalles: DetallePagoRegistroRequestExt[]
): DetallePagoRegistroRequestExt[] => {
  console.log(
    "[mapDetallePagos] Detalles pendientes recibidos:",
    detallesPendientes
  );
  return detallesPendientes.map((det) => {
    // Se utiliza 'mensualidadId' si existe; de lo contrario se usa 'id'
    const idKey = det.mensualidadId ?? det.id;
    // Buscar si ya existe un detalle en el formulario con ese idKey o pagoId
    const existing = currentDetalles.find(
      (d) => d.id === idKey || (d.pagoId != null && d.pagoId === det.pagoId)
    );
    // Si ya existe y no es autoGenerado, se conserva su valor; si no, se usa el importe pendiente
    const aCobrarValue =
      existing && !existing.autoGenerated
        ? existing.aCobrar
        : det.importePendiente ?? 0;

    const mappedDetail: DetallePagoRegistroRequestExt = {
      autoGenerated: existing ? existing.autoGenerated : true,
      id: det.id,
      version: det.version,
      descripcionConcepto: det.descripcionConcepto || "",
      conceptoId: det.conceptoId ?? null,
      subConceptoId: det.subConceptoId ?? null,
      cuotaOCantidad: det.cuotaOCantidad ?? "1",
      valorBase: det.valorBase ?? 0,
      bonificacionId: det.bonificacionId ?? null,
      recargoId: det.recargoId ?? null,
      importePendiente: det.importePendiente,
      aCobrar: aCobrarValue,
      cobrado: det.cobrado ?? false,
      mensualidadId: det.mensualidadId ?? null,
      matriculaId: det.matriculaId ?? null,
      importeInicial: det.importeInicial ?? null,
      stockId: det.stockId ?? null,
      pagoId: det.pagoId ?? null,
      tieneRecargo: det.tieneRecargo
    };

    console.log("[mapDetallePagos] Detalle mapeado:", mappedDetail);
    return mappedDetail;
  });
};

/**
 * Hook para sincronizar los detalles del formulario con los detalles provenientes del backend.
 * Se adapta para usar la propiedad correcta (detallePagos) o, si existiera, detallePagosPendientes.
 */
export const useSyncDetalles = (alumnoData: AlumnoDataResponse | undefined) => {
  const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();
  const initialSyncDone = useRef(false);

  useEffect(() => {
    console.log("[useSyncDetalles] Iniciando sincronización...");

    if (!alumnoData) {
      console.log(
        "[useSyncDetalles] No hay alumnoData. Abortando sincronización."
      );
      return;
    }

    if (initialSyncDone.current) {
      console.log("[useSyncDetalles] Sincronización ya realizada previamente.");
      return;
    }

    // Usamos la propiedad 'detallePagosPendientes' si existe; de lo contrario, 'detallePagos'
    const detallesPendientes =
      alumnoData.detallePagosPendientes || alumnoData.detallePagos || [];
    console.log(
      "[useSyncDetalles] Detalles pendientes a mapear:",
      detallesPendientes
    );

    // 1. Mapear los detalles pendientes del backend
    const autoDetails = mapDetallePagos(
      detallesPendientes,
      values.detallePagos
    );
    console.log(
      "[useSyncDetalles] Detalles automáticos mapeados:",
      autoDetails
    );

    // 2. Filtrar los detalles que hayan sido removidos manualmente y aquellos sin importe pendiente
    const removedIds = new Set(values.autoRemoved || []);
    const autoDetailsFiltered = autoDetails.filter((det) => {
      if (det.id != null) {
        const include =
          !removedIds.has(det.id) && Number(det.importePendiente) > 0;
        console.log(
          `[useSyncDetalles] Auto detalle id=${det.id} ${
            include ? "incluido" : "excluido"
          }`
        );
        return include;
      }
      const include = Number(det.importePendiente) > 0;
      console.log(
        `[useSyncDetalles] Auto detalle sin id ${
          include ? "incluido" : "excluido"
        }`
      );
      return include;
    });
    console.log(
      "[useSyncDetalles] Detalles automáticos filtrados:",
      autoDetailsFiltered
    );

    // 3. Extraer detalles manuales (no autogenerados) que ya están en el formulario
    const manualDetails = values.detallePagos.filter(
      (det) => !det.autoGenerated
    );
    console.log("[useSyncDetalles] Detalles manuales actuales:", manualDetails);

    // 4. Fusionar detalles manuales y automáticos, evitando duplicados
    let mergedDetails: DetallePagoRegistroRequestExt[];
    if (manualDetails.length > 0) {
      const manualIds = new Set(
        manualDetails.filter((det) => det.id != null).map((det) => det.id)
      );
      console.log(
        "[useSyncDetalles] IDs de detalles manuales:",
        Array.from(manualIds)
      );

      const finalAutoDetails = autoDetailsFiltered.filter((autoDet) => {
        if (autoDet.id != null) {
          const include = !manualIds.has(autoDet.id);
          if (!include) {
            console.log(
              `[useSyncDetalles] Auto detalle id=${autoDet.id} descartado por duplicado manual.`
            );
          }
          return include;
        }
        // Si no tiene id, se compara por campos clave
        const exists = manualDetails.some(
          (manDet) =>
            manDet.descripcionConcepto === autoDet.descripcionConcepto &&
            manDet.valorBase === autoDet.valorBase
        );
        if (exists) {
          console.log(
            "[useSyncDetalles] Auto detalle sin id descartado por coincidencia de campos:",
            autoDet
          );
        }
        return !exists;
      });

      mergedDetails = [...manualDetails, ...finalAutoDetails];
      console.log(
        "[useSyncDetalles] Fusión de manuales y automáticos:",
        mergedDetails
      );
    } else {
      mergedDetails = autoDetailsFiltered;
      console.log(
        "[useSyncDetalles] No hay detalles manuales; usando automáticos:",
        mergedDetails
      );
    }

    // 5. Actualizar el formulario solo si la lista final difiere de la actual
    if (!isEqual(mergedDetails, values.detallePagos)) {
      console.log(
        "[useSyncDetalles] Actualizando 'detallePagos' a:",
        mergedDetails
      );
      setFieldValue("detallePagos", mergedDetails);
    } else {
      console.log("[useSyncDetalles] No hay cambios en 'detallePagos'.");
    }

    initialSyncDone.current = true;
  }, [alumnoData, setFieldValue, values.autoRemoved, values.detallePagos]);
};
