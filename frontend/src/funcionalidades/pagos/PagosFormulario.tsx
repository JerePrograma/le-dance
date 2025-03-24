// src/forms/CobranzasForm.tsx
import React, { useState, useCallback, useEffect, useRef } from "react";
import { useSearchParams, useNavigate, useLocation } from "react-router-dom";
import {
  Formik,
  Form,
  Field,
  FieldArray,
  useFormikContext,
  FormikErrors,
} from "formik";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import { useCobranzasData } from "../../hooks/useCobranzasData";
import { useAlumnoData } from "../../hooks/useAlumnoData";
import type {
  CobranzasFormValues,
  DetallePagoRegistroRequest,
  DetallePagoRegistroRequestExt,
  ConceptoResponse,
  DisciplinaDetalleResponse,
  MetodoPagoResponse,
  MatriculaResponse,
  StockResponse,
  AlumnoRegistroRequest,
  AlumnoResponse,
  PagoRegistroRequest,
  AlumnoDataResponse,
} from "../../types/types";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";
import FormHeader from "../../componentes/FormHeader";
import { PaymentIdUpdater } from "../../componentes/PaymentUpdater";
import { useSyncDetalles } from "../../hooks/context/useSyncDetalles";
import { normalizeInscripcion } from "./normalizeInscripcion";

// ----- Utilidades -----
const getMesVigente = (): string => {
  return new Date()
    .toLocaleString("default", {
      month: "long",
      year: "numeric",
      timeZone: "America/Argentina/Buenos_Aires",
    })
    .toUpperCase();
};

const generatePeriodos = (numMeses = 12): string[] => {
  const periodos: string[] = [];
  const currentDate = new Date(
    new Date().toLocaleString("en-US", {
      timeZone: "America/Argentina/Buenos_Aires",
    })
  );
  for (let i = 0; i < numMeses; i++) {
    const date = new Date(
      currentDate.getFullYear(),
      currentDate.getMonth() - 2 + i,
      1
    );
    periodos.push(
      date
        .toLocaleString("default", { month: "long", year: "numeric" })
        .toUpperCase()
    );
  }
  return periodos;
};

// Valores iniciales para el formulario.
const defaultValues: CobranzasFormValues = {
  id: 0,
  reciboNro: "AUTO-001",
  alumno: {
    id: 0,
    nombre: "",
    apellido: "",
    fechaNacimiento: new Date().toISOString().split("T")[0],
    fechaIncorporacion: new Date().toISOString().split("T")[0],
    celular1: "",
    celular2: "",
    email1: "",
    email2: "",
    documento: "",
    cuit: "",
    nombrePadres: "",
    autorizadoParaSalirSolo: false,
    otrasNotas: "",
    cuotaTotal: 0,
    inscripciones: [],
  } as unknown as AlumnoRegistroRequest,
  alumnoId: 0,
  fecha: new Date().toISOString().split("T")[0],
  detallePagos: [],
  disciplina: "",
  tarifa: "",
  conceptoSeleccionado: "",
  stockSeleccionado: "",
  cantidad: 1,
  totalCobrado: 0,
  totalACobrar: 0,
  metodoPagoId: 0,
  observaciones: "",
  matriculaRemoved: false,
  mensualidadId: 0,
  periodoMensual: getMesVigente(),
  autoRemoved: [],
  pagoParcial: 0,
};

// ----- Hook TotalsUpdater -----
// Este hook recalcula el total a cobrar como la suma de los "importePendiente" de cada detalle.
const TotalsUpdater: React.FC = () => {
  const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

  useEffect(() => {
    const computedTotalACobrar = values.detallePagos.reduce(
      (total, item) => total + Number(item.importePendiente || 0),
      0
    );
    const computedTotalCobrado = values.detallePagos.reduce(
      (total, item) => total + Number(item.aCobrar || 0),
      0
    );
    if (values.totalACobrar !== computedTotalACobrar) {
      setFieldValue("totalACobrar", computedTotalACobrar);
    }
    if (values.totalCobrado !== computedTotalCobrado) {
      setFieldValue("totalCobrado", computedTotalCobrado);
    }
  }, [values.detallePagos, setFieldValue]);

  return null;
};

// ----- Hook useSyncDetalles -----
// Refactorizado para sincronizar solo al inicio (o cuando cambie alumnoData.autoRemoved)
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

    // Sincronizar solo si no se ha hecho la sincronización inicial.
    if (initialSyncDone.current) {
      console.log(
        "[useSyncDetalles] Sincronización ya realizada. No se actualiza."
      );
      return;
    }

    console.log("[useSyncDetalles] AlumnoData recibido:", alumnoData);
    console.log(
      "[useSyncDetalles] Valores actuales del formulario (detallePagos):",
      values.detallePagos
    );
    console.log(
      "[useSyncDetalles] Valores actuales del formulario (autoRemoved):",
      values.autoRemoved
    );

    // 1. Mapear detalles pendientes al formato del formulario.
    const autoDetails = mapDetallePagos(
      alumnoData.detallePagosPendientes || [],
      values.detallePagos
    );
    console.log(
      "[useSyncDetalles] Detalles automáticos mapeados:",
      autoDetails
    );

    // 2. Filtrar detalles removidos manualmente.
    const removedIds = new Set(values.autoRemoved || []);
    const autoDetailsFiltered = autoDetails.filter((det) => {
      if (det.id != null) {
        const include =
          !removedIds.has(det.id) && Number(det.importePendiente) > 0;
        console.log(
          `[useSyncDetalles] Auto detalle id=${det.id} ${
            include ? "incluido" : "excluido"
          } por removedIds (${removedIds.has(det.id)}) y importePendiente (${
            det.importePendiente
          })`
        );
        return include;
      }
      const include = Number(det.importePendiente) > 0;
      console.log(
        `[useSyncDetalles] Auto detalle sin id ${
          include ? "incluido" : "excluido"
        } por importePendiente (${det.importePendiente})`
      );
      return include;
    });
    console.log(
      "[useSyncDetalles] Detalles automáticos filtrados:",
      autoDetailsFiltered
    );

    // 3. Extraer los detalles manuales (no autogenerados) ya existentes.
    const manualDetails = values.detallePagos.filter(
      (det) => !det.autoGenerated
    );
    console.log("[useSyncDetalles] Detalles manuales actuales:", manualDetails);

    // 4. Fusionar manual y automáticos evitando duplicados.
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
              `[useSyncDetalles] Auto detalle id=${autoDet.id} descartado porque ya existe manual.`
            );
          }
          return include;
        }
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
      console.log(
        "[useSyncDetalles] Detalles automáticos finales tras filtrar duplicados:",
        finalAutoDetails
      );
      mergedDetails = [...manualDetails, ...finalAutoDetails];
      console.log(
        "[useSyncDetalles] Fusionando manuales y automáticos:",
        mergedDetails
      );
    } else {
      mergedDetails = autoDetailsFiltered;
      console.log(
        "[useSyncDetalles] No hay detalles manuales; usando automáticos:",
        mergedDetails
      );
    }

    // 5. Actualizar el formulario si la lista final difiere.
    if (!isEqual(mergedDetails, values.detallePagos)) {
      console.log(
        "[useSyncDetalles] La lista final difiere. Actualizando 'detallePagos' a:",
        mergedDetails
      );
      setFieldValue("detallePagos", mergedDetails);
    } else {
      console.log(
        "[useSyncDetalles] La lista final es igual. No se actualiza."
      );
    }
    initialSyncDone.current = true;
  }, [alumnoData, setFieldValue, values.autoRemoved]);
};

/**
 * Componente principal para gestionar cobranzas.
 */
const CobranzasForm: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [initialValues, setInitialValues] =
    useState<CobranzasFormValues>(defaultValues);
  const [selectedAlumnoId, setSelectedAlumnoId] = useState<number>(0);

  // Datos de la app
  const { alumnos, disciplinas, stocks, metodosPago, conceptos } =
    useCobranzasData();
  const { data: alumnoData } = useAlumnoData(selectedAlumnoId);

  const mappedDisciplinas: DisciplinaDetalleResponse[] = disciplinas.map(
    (disc) => ({
      ...disc,
      salon: (disc as any).salon ?? "",
      salonId: (disc as any).salonId ?? 0,
      matricula: (disc as any).matricula ?? 0,
      profesorApellido: (disc as any).profesorApellido ?? "",
      profesorId: (disc as any).profesorId ?? 0,
      inscritos: (disc as any).inscritos ?? 0,
      horarios: (disc as any).horarios ?? [],
    })
  );

  // Hook para sincronizar detalles pendientes con el formulario.
  const SyncDetalles: React.FC<{ deudaData: AlumnoDataResponse }> = ({
    deudaData,
  }) => {
    useSyncDetalles(deudaData);
    return null;
  };

  const handleAlumnoChange = useCallback(
    async (
      alumnoIdStr: string,
      _currentValues: CobranzasFormValues,
      setFieldValue: (
        field: string,
        value: any,
        shouldValidate?: boolean
      ) => Promise<void>
    ) => {
      const id = Number(alumnoIdStr);
      await setFieldValue("alumnoId", id, true);
      setSelectedAlumnoId(id);
      await setFieldValue("matriculaRemoved", false, true);

      if (alumnoData) {
        console.log(
          "[handleAlumnoChange] Asignando data completa del alumno:",
          alumnoData
        );
        await setFieldValue("alumno", alumnoData, true);
      } else {
        await setFieldValue(
          "alumno",
          {
            id,
            nombre: "",
            apellido: "",
            fechaNacimiento: new Date().toISOString().split("T")[0],
            fechaIncorporacion: new Date().toISOString().split("T")[0],
            celular1: "",
            celular2: "",
            email1: "",
            email2: "",
            documento: "",
            cuit: "",
            nombrePadres: "",
            autorizadoParaSalirSolo: false,
            otrasNotas: "",
            cuotaTotal: 0,
            inscripciones: [],
          },
          true
        );
      }
    },
    [alumnoData]
  );

  function normalizeAlumno(alumno: AlumnoResponse): AlumnoRegistroRequest {
    return {
      id: alumno.id,
      nombre: alumno.nombre,
      apellido: alumno.apellido,
      fechaNacimiento: alumno.fechaNacimiento,
      fechaIncorporacion: alumno.fechaIncorporacion,
      edad: alumno.edad,
      celular1: alumno.celular1,
      celular2: alumno.celular2,
      email1: alumno.email1,
      email2: alumno.email2,
      documento: alumno.documento,
      fechaDeBaja: alumno.fechaDeBaja,
      deudaPendiente: alumno.deudaPendiente,
      nombrePadres: alumno.nombrePadres,
      autorizadoParaSalirSolo: alumno.autorizadoParaSalirSolo,
      activo: alumno.activo,
      otrasNotas: alumno.otrasNotas,
      cuotaTotal: alumno.cuotaTotal,
      inscripciones: alumno.inscripciones.map(normalizeInscripcion),
    };
  }

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) {
      pagosApi
        .obtenerPagoPorId(Number(idParam))
        .then((pagoData) => {
          setInitialValues({
            ...defaultValues,
            id: pagoData.id,
            reciboNro: pagoData.id.toString(),
            alumno: pagoData.alumno
              ? normalizeAlumno(pagoData.alumno)
              : defaultValues.alumno,
            alumnoId: pagoData.alumno.id,
            fecha: pagoData.fecha || new Date().toISOString().split("T")[0],
            detallePagos: pagoData.detallePagos.map((detalle: any) => ({
              id: detalle.id,
              descripcionConcepto: detalle.descripcionConcepto ?? "",
              conceptoId: detalle.conceptoId ?? null,
              subConceptoId: detalle.subConceptoId ?? null,
              cuotaOCantidad: detalle.cuotaOCantidad || "1",
              valorBase: detalle.valorBase ?? 0,
              bonificacionId: detalle.bonificacion
                ? detalle.bonificacion.id
                : null,
              recargoId: detalle.recargo ? detalle.recargo.id : null,
              aCobrar: detalle.importePendiente, // Inicialmente, aCobrar = importePendiente
              importePendiente: detalle.importePendiente,
              cobrado: detalle.cobrado,
              mensualidadId: detalle.mensualidadId ?? null,
              matriculaId: detalle.matriculaId ?? null,
              stockId: detalle.stockId ?? null,
              version: detalle.version ?? null,
              pagoId: detalle.pagoId ?? null,
              autoGenerated: true,
            })),
            totalCobrado: pagoData.detallePagos.reduce(
              (sum: number, detalle: any) =>
                sum + (Number(detalle.importePendiente) || 0),
              0
            ),
            totalACobrar: pagoData.detallePagos.reduce(
              (sum: number, detalle: any) =>
                sum + (Number(detalle.importePendiente) || 0),
              0
            ),
            metodoPagoId: pagoData.metodoPago?.id || 0,
            observaciones: pagoData.observaciones || "",
          });
        })
        .catch(() => {
          toast.error("Error al cargar los datos del pago.");
        });
    }
  }, [searchParams]);

  const handleAgregarDetalle = useCallback(
    (
      values: CobranzasFormValues,
      setFieldValue: (field: string, value: any) => void
    ) => {
      const newDetails = [...values.detallePagos];
      let added = false;

      const isDuplicate = (concept: number) =>
        newDetails.some((detalle) => detalle.conceptoId === concept);
      const isDuplicateString = (desc: string) =>
        newDetails.some(
          (det) => det.descripcionConcepto.trim() === desc.trim()
        );

      if (values.conceptoSeleccionado) {
        const selectedConcept = conceptos.find(
          (c: ConceptoResponse) =>
            c.id.toString() === values.conceptoSeleccionado
        );
        if (selectedConcept) {
          if (isDuplicate(selectedConcept.id)) {
            toast.error("Concepto ya se encuentra agregado");
          } else {
            newDetails.push({
              id: 0,
              descripcionConcepto: String(selectedConcept.descripcion),
              conceptoId: selectedConcept.id,
              subConceptoId: null,
              cuotaOCantidad: "1",
              valorBase: selectedConcept.precio,
              bonificacionId: null,
              recargoId: null,
              aCobrar: selectedConcept.precio,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              stockId: null,
              version: selectedConcept.version,
              autoGenerated: false,
              pagoId: null,
            } as DetallePagoRegistroRequestExt);
            added = true;
          }
        }
      }
      if (values.disciplina && values.tarifa) {
        const selectedDisciplina = mappedDisciplinas.find(
          (disc) => disc.nombre === values.disciplina
        );
        if (selectedDisciplina) {
          let precio = 0;
          let tarifaLabel = "";
          if (values.tarifa === "CUOTA") {
            precio = selectedDisciplina.valorCuota;
            tarifaLabel = "CUOTA";
          } else if (values.tarifa === "CLASE_SUELTA") {
            precio = selectedDisciplina.claseSuelta || 0;
            tarifaLabel = "CLASE SUELTA";
          } else if (values.tarifa === "CLASE_PRUEBA") {
            precio = selectedDisciplina.clasePrueba || 0;
            tarifaLabel = "CLASE DE PRUEBA";
          }
          const cantidad = Number(values.cantidad) || 1;
          const total = precio * cantidad;
          const conceptoDetalle = `${selectedDisciplina.nombre} - ${tarifaLabel} - ${values.periodoMensual}`;
          if (isDuplicateString(conceptoDetalle)) {
            toast.error("Concepto ya se encuentra agregado");
          } else {
            newDetails.push({
              id: 0,
              descripcionConcepto: conceptoDetalle,
              conceptoId: selectedDisciplina.id,
              subConceptoId: null,
              cuotaOCantidad: cantidad.toString(),
              valorBase: total,
              bonificacionId: null,
              recargoId: null,
              aCobrar: total,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              stockId: null,
              autoGenerated: false,
              pagoId: null,
            } as DetallePagoRegistroRequestExt);
            added = true;
          }
        }
      }
      if (values.stockSeleccionado) {
        const selectedStock = stocks.find(
          (s: StockResponse) => s.id.toString() === values.stockSeleccionado
        );
        if (selectedStock) {
          const cantidad = Number(values.cantidad) || 1;
          const total = selectedStock.precio * cantidad;
          const stockDesc = selectedStock.nombre;
          if (isDuplicateString(stockDesc)) {
            toast.error("Concepto ya se encuentra agregado");
          } else {
            newDetails.push({
              id: 0,
              descripcionConcepto: stockDesc,
              conceptoId: null,
              subConceptoId: null,
              cuotaOCantidad: cantidad.toString(),
              valorBase: total,
              bonificacionId: null,
              recargoId: null,
              aCobrar: total,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              stockId: selectedStock.id,
              version: selectedStock.version,
              autoGenerated: false,
              pagoId: null,
            } as DetallePagoRegistroRequestExt);
            added = true;
          }
        }
      }
      if (added) {
        setFieldValue("detallePagos", newDetails);
        const totalCobrado = newDetails.reduce(
          (acc, det) => acc + (Number(det.aCobrar) || 0),
          0
        );
        setFieldValue("totalCobrado", totalCobrado);
        setFieldValue("conceptoSeleccionado", "");
        setFieldValue("stockSeleccionado", "");
        setFieldValue("cantidad", 1);
      } else if (
        !values.conceptoSeleccionado &&
        !values.disciplina &&
        !values.stockSeleccionado
      ) {
        toast.error("Seleccione al menos un conjunto de campos para agregar");
      }
    },
    [conceptos, mappedDisciplinas, stocks]
  );

  const onSubmit = async (values: CobranzasFormValues, actions: any) => {
    try {
      const detallesFiltrados = values.detallePagos.filter(
        (detalle) => Number(detalle.aCobrar) !== 0
      );
      const pagoRegistroRequest: PagoRegistroRequest = {
        alumno: values.alumno,
        fecha: values.fecha,
        fechaVencimiento: values.fecha,
        monto: Number(values.totalACobrar),
        importeInicial: Number(values.totalACobrar),
        metodoPagoId: Number(values.metodoPagoId) || 0,
        activo: true,
        detallePagos: detallesFiltrados.map<DetallePagoRegistroRequest>(
          (d) => ({
            id: d.id,
            descripcionConcepto: d.descripcionConcepto,
            conceptoId: d.conceptoId ?? null,
            subConceptoId: d.subConceptoId ?? null,
            importePendiente: d.importePendiente,
            cuotaOCantidad: d.cuotaOCantidad,
            valorBase: d.valorBase,
            bonificacionId: d.bonificacionId ? Number(d.bonificacionId) : null,
            recargoId: d.recargoId ? Number(d.recargoId) : null,
            aCobrar: d.aCobrar,
            cobrado: d.cobrado,
            mensualidadId: d.mensualidadId ?? null,
            matriculaId: d.matriculaId ?? null,
            stockId: d.stockId ?? null,
            version: d.version ?? null,
            pagoId: d.pagoId ?? null,
          })
        ),
        pagoMedios: [],
      };

      await pagosApi.registrarPago(pagoRegistroRequest);
      toast.success("Cobranza registrada correctamente");
      actions.resetForm();
      navigate("/pagos");
    } catch (error: any) {
      const errorMsg =
        error.response?.data?.detalle || "Error al registrar la cobranza";
      toast.error(errorMsg);
    }
  };

  return (
    <ResponsiveContainer className="py-4">
      <h1 className="page-title text-2xl font-bold mb-4">
        Gestión de Cobranzas
      </h1>
      {alumnoData?.ultimoPago && (
        <div className="mb-4 p-2 border">
          <p>
            Último pago registrado: <strong>{alumnoData.ultimoPago.id}</strong>
          </p>
          <p>Monto: {alumnoData.ultimoPago.monto}</p>
        </div>
      )}
      <Formik
        key={location.key}
        initialValues={initialValues}
        onSubmit={onSubmit}
        enableReinitialize
      >
        {({ values, setFieldValue }) => (
          <Form className="w-full">
            <TotalsUpdater />
            <PaymentIdUpdater
              ultimoPago={alumnoData?.ultimoPago ?? undefined}
            />
            {alumnoData && alumnoData.detallePagosPendientes && (
              <SyncDetalles deudaData={alumnoData} />
            )}
            <FormHeader
              alumnos={alumnos}
              handleAlumnoChange={handleAlumnoChange}
            />
            {/* Se asume que ConceptosSection y DetallesTable se importan y utilizan sin cambios */}
            {/* Ejemplo: */}
            {/* <ConceptosSection
              disciplinas={mappedDisciplinas}
              stocks={stocks}
              conceptos={conceptos}
              values={values}
              setFieldValue={setFieldValue}
              handleAgregarDetalle={handleAgregarDetalle}
            /> */}
            {/* Renderizado de la tabla de detalles */}
            <FieldArray name="detallePagos">
              {({ remove, form }) => (
                <div className="overflow-x-auto">
                  <table className="border mb-4 w-auto table-layout-auto">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[120px]">
                          Concepto
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
                          Cantidad
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">
                          Valor Base
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
                          Bonificación
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
                          Recargo
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">
                          Importe
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
                          A Cobrar
                        </th>
                        <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">
                          Acciones
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {form.values.detallePagos &&
                      form.values.detallePagos.length > 0 ? (
                        form.values.detallePagos.map(
                          (_: any, index: number) => (
                            <tr key={index} className="hover:bg-gray-50">
                              <td className="border p-2 text-center text-sm">
                                <Field
                                  name={`detallePagos.${index}.descripcionConcepto`}
                                  type="text"
                                  className="w-full px-2 py-1 border rounded text-center"
                                  readOnly
                                />
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <Field
                                  name={`detallePagos.${index}.cuotaOCantidad`}
                                  type="text"
                                  className="w-full px-2 py-1 border rounded text-center"
                                  readOnly
                                />
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <Field
                                  name={`detallePagos.${index}.valorBase`}
                                  type="number"
                                  className="w-full px-2 py-1 border rounded text-center"
                                  readOnly
                                />
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <Field
                                  name={`detallePagos.${index}.bonificacionId`}
                                  type="number"
                                  className="w-full px-2 py-1 border rounded text-center"
                                  readOnly
                                />
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <Field
                                  name={`detallePagos.${index}.recargoId`}
                                  type="number"
                                  className="w-full px-2 py-1 border rounded text-center"
                                  readOnly
                                />
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <Field
                                  name={`detallePagos.${index}.importePendiente`}
                                >
                                  {({ field }: any) => (
                                    <input
                                      type="number"
                                      {...field}
                                      readOnly
                                      className="w-full px-2 py-1 border rounded text-center"
                                    />
                                  )}
                                </Field>
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <Field name={`detallePagos.${index}.aCobrar`}>
                                  {({ field }: any) => (
                                    <input
                                      type="number"
                                      {...field}
                                      className="w-full px-2 py-1 border rounded text-center"
                                    />
                                  )}
                                </Field>
                              </td>
                              <td className="border p-2 text-center text-sm">
                                <button
                                  type="button"
                                  className="bg-red-500 hover:bg-red-600 text-white p-1 rounded text-xs transition-colors mx-auto block"
                                  onClick={() => remove(index)}
                                >
                                  Eliminar
                                </button>
                              </td>
                            </tr>
                          )
                        )
                      ) : (
                        <tr>
                          <td colSpan={8} className="text-center text-sm p-2">
                            No hay datos
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </FieldArray>
            <div className="border p-4 mb-4">
              <h2 className="font-bold mb-2">Totales y Pago</h2>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block font-medium">Total Importe:</label>
                  <input
                    type="number"
                    readOnly
                    className="border p-2 w-full"
                    value={values.totalACobrar}
                  />
                </div>
                <div>
                  <label className="block font-medium">Total Cobrado:</label>
                  <Field
                    name="totalCobrado"
                    type="number"
                    className="border p-2 w-full"
                  />
                  <small className="text-gray-500">
                    Se autocompleta sumando el valor de A Cobrar de cada
                    detalle, pero puedes modificarlo.
                  </small>
                </div>
                <div>
                  <label className="block font-medium">Método de Pago:</label>
                  <Field
                    as="select"
                    name="metodoPagoId"
                    className="border p-2 w-full"
                  >
                    <option value="">Seleccione un método de pago</option>
                    {metodosPago.map((mp: MetodoPagoResponse) => (
                      <option key={mp.id} value={mp.id}>
                        {mp.descripcion}
                      </option>
                    ))}
                  </Field>
                </div>
              </div>
            </div>
            <div className="border p-4 mb-4">
              <label className="block font-medium">Observaciones:</label>
              <Field
                as="textarea"
                name="observaciones"
                className="border p-2 w-full"
                rows={3}
              />
            </div>
            <div className="flex justify-end gap-4">
              <button
                type="submit"
                className="bg-green-500 p-2 rounded text-white"
              >
                {searchParams.get("id")
                  ? "Actualizar Cobranza"
                  : "Registrar Cobranza"}
              </button>
              {selectedAlumnoId > 0 && (
                <button
                  type="button"
                  onClick={() => navigate(`/pagos/alumno/${selectedAlumnoId}`)}
                  className="bg-blue-500 text-white p-2 rounded"
                  aria-label="Ver historial de pagos del alumno"
                >
                  Ver Historial
                </button>
              )}
              <button
                type="reset"
                className="bg-gray-500 p-2 rounded text-white"
              >
                Cancelar
              </button>
            </div>
          </Form>
        )}
      </Formik>
    </ResponsiveContainer>
  );
};

export default CobranzasForm;
