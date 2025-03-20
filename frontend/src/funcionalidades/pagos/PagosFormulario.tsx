// src/forms/CobranzasForm.tsx
import React, { useState, useCallback, useEffect } from "react";
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
import matriculasApi from "../../api/matriculasApi";
import { useCobranzasData } from "../../hooks/useCobranzasData";
import { useAlumnoData } from "../../hooks/useAlumnoData";
import { useInscripcionesActivas } from "../../hooks/useInscripcionesActivas";
import type {
  CobranzasFormValues,
  DetallePagoRegistroRequest,
  ConceptoResponse,
  DisciplinaDetalleResponse,
  InscripcionResponse,
  DeudasPendientesResponse,
  MetodoPagoResponse,
  MatriculaResponse,
  StockResponse,
} from "../../types/types";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";
import FormHeader from "../../componentes/FormHeader";
import { PaymentIdUpdater } from "../../componentes/PaymentUpdater";
import { useSyncDetalles } from "../../hooks/context/useSyncDetalles";
import { normalizeInscripcion } from "./NormalizeInscripcion";

// ----- Utilidades -----
const getMesVigente = () => {
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

// Valores iniciales para el formulario (sin dummy en inscripcion)
const defaultValues: CobranzasFormValues = {
  id: 0,
  reciboNro: "AUTO-001",
  alumno: "",
  alumnoId: "",
  inscripcion: null, // Se inicia en null
  inscripcionId: 0,
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
  mensualidadId: "",
  periodoMensual: getMesVigente(),
  autoRemoved: [],
  pagoParcial: 0,
};
// ----- Subcomponentes -----

/**
 * TotalsUpdater recalcula totales según detalles y método de pago
 */
const TotalsUpdater: React.FC<{ metodosPago: MetodoPagoResponse[] }> = ({
  metodosPago,
}) => {
  const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

  useEffect(() => {
    const baseTotal = values.detallePagos.reduce(
      (total, item) => total + Number(item.importePendiente || 0),
      0
    );
    let recargoValue = 0;
    if (values.metodoPagoId) {
      const selectedMetodo = metodosPago.find(
        (mp: MetodoPagoResponse) =>
          String(mp.id) === String(values.metodoPagoId)
      );
      if (
        selectedMetodo &&
        selectedMetodo.descripcion.toUpperCase() === "DEBITO"
      ) {
        recargoValue = Number(selectedMetodo.recargo) || 0;
      }
    }
    const computedTotalACobrar = baseTotal + recargoValue;
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
  }, [values.detallePagos, values.metodoPagoId, metodosPago, setFieldValue]);

  return null;
};

/**
 * ConceptosSection: Muestra selectores para disciplina, tarifa, concepto y stock.
 */
interface ConceptosSectionProps {
  inscripcionesData?: InscripcionResponse[];
  disciplinas: DisciplinaDetalleResponse[];
  stocks: StockResponse[];
  conceptos: ConceptoResponse[];
  values: CobranzasFormValues;
  setFieldValue: (field: string, value: any) => void;
  handleAgregarDetalle: (
    values: CobranzasFormValues,
    setFieldValue: (field: string, value: any) => void
  ) => void;
}
const ConceptosSection: React.FC<ConceptosSectionProps> = ({
  inscripcionesData,
  disciplinas,
  stocks,
  conceptos,
  values,
  setFieldValue,
  handleAgregarDetalle,
}) => {
  return (
    <div className="border p-4 mb-4">
      <h2 className="font-bold mb-2">Datos de Disciplina y Conceptos</h2>
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
        <div className="sm:col-span-2">
          <label className="block font-medium">Disciplina:</label>
          {inscripcionesData && inscripcionesData.length > 0 ? (
            <Field
              as="select"
              name="inscripcionId"
              className="border p-2 w-full"
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                const selectedId = Number(e.target.value);
                setFieldValue("inscripcionId", selectedId);
                const insc = inscripcionesData.find(
                  (ins) => ins.id === selectedId
                );
                if (insc) {
                  // Actualizamos la inscripción con datos reales
                  setFieldValue("inscripcion", normalizeInscripcion(insc));
                  setFieldValue("disciplina", insc.disciplina.nombre);
                } else {
                  setFieldValue("inscripcion", null);
                }
              }}
            >
              <option value="">Seleccione una disciplina</option>
              {inscripcionesData.map((insc) => (
                <option key={insc.id} value={insc.id}>
                  {insc.disciplina.nombre}
                </option>
              ))}
            </Field>
          ) : (
            <Field
              as="select"
              name="disciplina"
              className="border p-2 w-full"
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                setFieldValue("disciplina", e.target.value);
                setFieldValue("tarifa", "");
              }}
            >
              <option value="">Seleccione una disciplina</option>
              {disciplinas.map((disc) => (
                <option key={disc.id} value={disc.id.toString()}>
                  {disc.nombre}
                </option>
              ))}
            </Field>
          )}
        </div>
        <div>
          <label className="block font-medium">Tarifa:</label>
          <Field as="select" name="tarifa" className="border p-2 w-full">
            <option value="">Seleccione una tarifa</option>
            <option value="CUOTA">CUOTA</option>
            <option value="CLASE_SUELTA">CLASE SUELTA</option>
            <option value="CLASE_PRUEBA">CLASE DE PRUEBA</option>
          </Field>
        </div>
        {values.tarifa === "CUOTA" && (
          <div>
            <label className="block font-medium">Periodo Mensual:</label>
            <Field
              as="select"
              name="periodoMensual"
              className="border p-2 w-full"
            >
              <option value="">Seleccione el mes/periodo</option>
              {generatePeriodos(12).map((periodo, index) => (
                <option key={index} value={periodo}>
                  {periodo}
                </option>
              ))}
            </Field>
          </div>
        )}
        <div className="sm:col-span-2">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-medium">Concepto:</label>
              <Field
                as="select"
                name="conceptoSeleccionado"
                className="border p-2 w-full"
              >
                <option value="">Seleccione un concepto</option>
                {conceptos.map((conc) => (
                  <option key={conc.id} value={conc.id}>
                    {conc.descripcion}
                  </option>
                ))}
              </Field>
            </div>
            <div>
              <label className="block font-medium">Stock:</label>
              <Field
                as="select"
                name="stockSeleccionado"
                className="border p-2 w-full"
              >
                <option value="">Seleccione un stock</option>
                {stocks.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nombre}
                  </option>
                ))}
              </Field>
            </div>
          </div>
        </div>
        <div>
          <label className="block font-medium">Cantidad:</label>
          <Field
            name="cantidad"
            type="number"
            className="border p-2 w-full"
            min="1"
          />
        </div>
      </div>
      <div className="mb-4">
        <button
          type="button"
          className="bg-green-500 text-white p-2 rounded mt-4"
          onClick={() => handleAgregarDetalle(values, setFieldValue)}
        >
          Agregar Detalle
        </button>
      </div>
    </div>
  );
};

/**
 * DetallesTable: Renderiza la tabla de detalles utilizando FieldArray.
 */
const DetallesTable: React.FC = () => (
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
              <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">
                Bonificación
              </th>
              <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
                Recargo
              </th>
              <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
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
            {form.values.detallePagos && form.values.detallePagos.length > 0 ? (
              form.values.detallePagos.map((_: any, index: number) => (
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
                      name={`detallePagos.${index}.cuota`}
                      type="text"
                      className="w-full px-2 py-1 border rounded text-center"
                      readOnly
                    />
                  </td>
                  <td className="border p-2 text-center text-sm">
                    <Field
                      name={`detallePagos.${index}.montoOriginal`}
                      type="number"
                      className="w-full px-2 py-1 border rounded text-center"
                      readOnly
                    />
                  </td>
                  <td className="border p-2 text-center text-sm">
                    <Field
                      name={`detallePagos.${index}.montoBonificacion`}
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
                    <Field name={`detallePagos.${index}.importePendiente`}>
                      {({ field }: any) => (
                        <input
                          type="number"
                          {...field}
                          className="w-full px-2 py-1 border rounded text-center"
                          readOnly
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
                      onClick={() => {
                        remove(index);
                      }}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))
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
);

// ----- Componente Principal -----
const CobranzasForm: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [initialValues, setInitialValues] =
    useState<CobranzasFormValues>(defaultValues);
  const [selectedAlumnoId, setSelectedAlumnoId] = useState<number>(0);
  const [, setMatricula] = useState<MatriculaResponse | null>(null);

  // Obtener datos unificados de cobranzas y alumno
  const { alumnos, disciplinas, stocks, metodosPago, conceptos } =
    useCobranzasData();
  const inscripcionesQuery = useInscripcionesActivas(selectedAlumnoId);
  const { data: alumnoData } = useAlumnoData(selectedAlumnoId);

  // Actualiza las disciplinas al formato esperado
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

  // Wrapper para sincronizar detalles (sin cambios significativos aquí)
  const SyncDetalles: React.FC<{
    deudaData: DeudasPendientesResponse;
    ultimoPago: any;
  }> = ({ deudaData, ultimoPago }) => {
    useSyncDetalles(deudaData, ultimoPago);
    return null;
  };

  // Manejo del cambio de alumno
  const handleAlumnoChange = useCallback(
    async (
      alumnoIdStr: string,
      _currentValues: CobranzasFormValues,
      setFieldValue: (
        field: string,
        value: any
      ) => Promise<void | FormikErrors<CobranzasFormValues>>
    ) => {
      await setFieldValue("alumno", alumnoIdStr);
      await setFieldValue("alumnoId", alumnoIdStr);
      const id = Number(alumnoIdStr);
      setSelectedAlumnoId(id);
      await setFieldValue("matriculaRemoved", false);
      if (id > 0) {
        try {
          const response = await matriculasApi.obtenerMatricula(id);
          setMatricula(response);
        } catch (error) {
          toast.error("Error al cargar matrícula");
        }
      }
    },
    []
  );

  // Cargar datos de pago si se pasa "id" en los query params
  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) {
      pagosApi
        .obtenerPagoPorId(Number(idParam))
        .then((pagoData) => {
          setInitialValues({
            ...defaultValues,
            id: 0,
            reciboNro: pagoData.id.toString(),
            alumno: pagoData.alumnoId ? pagoData.alumnoId.toString() : "",
            // Si la inscripción es válida, normalizarla; de lo contrario, dejar en null
            inscripcion:
              pagoData.inscripcion && pagoData.inscripcion.id > 0
                ? normalizeInscripcion(pagoData.inscripcion)
                : null,
            inscripcionId: pagoData.inscripcion ? pagoData.inscripcion.id : -1,
            alumnoId: pagoData.alumnoId ? pagoData.alumnoId.toString() : "",
            fecha: pagoData.fecha || new Date().toISOString().split("T")[0],
            detallePagos: pagoData.detallePagos.map((detalle: any) => ({
              id: detalle.id,
              descripcionConcepto: detalle.descripcionConcepto ?? "",
              conceptoId: detalle.conceptoId ?? null,
              subConceptoId: detalle.subConceptoId ?? null,
              cuota: detalle.cuota,
              montoOriginal: detalle.montoOriginal,
              stockId: detalle.stockId,
              bonificacionId: detalle.bonificacion
                ? detalle.bonificacion.id
                : null,
              recargoId: detalle.recargo ? detalle.recargo.id : null,
              importePendiente: detalle.importePendiente,
              aCobrar: detalle.importePendiente,
              aFavor: detalle.aFavor != null ? detalle.aFavor : 0,
              cobrado: detalle.cobrado,
              mensualidadId: detalle.mensualidadId ?? null,
              matriculaId: detalle.matriculaId ?? null,
              autoGenerated: true,
            })),
            totalCobrado: pagoData.detallePagos.reduce(
              (sum: number, detalle: any) =>
                sum +
                (Number(detalle.aCobrar) ||
                  Number(detalle.importePendiente) ||
                  0),
              0
            ),
            totalACobrar: pagoData.detallePagos.reduce(
              (sum: number, detalle: any) =>
                sum + (Number(detalle.importePendiente) || 0),
              0
            ),
            metodoPagoId: pagoData.metodoPago.id ? pagoData.metodoPago.id : 0,
            observaciones: pagoData.observaciones || "",
          });
        })
        .catch(() => {
          toast.error("Error al cargar los datos del pago.");
        });
    }
  }, [searchParams]);

  // Función para agregar detalle
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
        newDetails.some((det) => det.descripcionConcepto === desc);

      // Agregar detalle basado en concepto seleccionado
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
              subConceptoId: null,
              cuota: "1",
              montoOriginal: selectedConcept.precio,
              bonificacionId: null,
              recargoId: null,
              importePendiente: selectedConcept.precio,
              aCobrar: selectedConcept.precio,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              conceptoId: null,
              autoGenerated: false,
              aFavor: 0,
              stockId: null,
            });
            added = true;
          }
        }
      }
      // Agregar detalle basado en disciplina/tarifa
      if ((values.inscripcion || values.disciplina) && values.tarifa) {
        const inscSeleccionada = inscripcionesQuery.data?.find(
          (insc) => insc.id === values.inscripcion?.id
        );
        if (inscSeleccionada) {
          let precio = 0;
          let tarifaLabel = "";
          if (values.tarifa === "CUOTA") {
            precio = inscSeleccionada.disciplina.valorCuota;
            tarifaLabel = "CUOTA";
          } else if (values.tarifa === "CLASE_SUELTA") {
            precio = inscSeleccionada.disciplina.claseSuelta || 0;
            tarifaLabel = "CLASE SUELTA";
          } else if (values.tarifa === "CLASE_PRUEBA") {
            precio = inscSeleccionada.disciplina.clasePrueba || 0;
            tarifaLabel = "CLASE DE PRUEBA";
          }
          const cantidad = Number(values.cantidad) || 1;
          const total = precio * cantidad;
          const conceptoDetalle = `${inscSeleccionada.disciplina.nombre} - ${tarifaLabel} - ${values.periodoMensual}`;
          if (isDuplicateString(conceptoDetalle)) {
            toast.error("Concepto ya se encuentra agregado");
          } else {
            newDetails.push({
              id: 0,
              descripcionConcepto: conceptoDetalle,
              conceptoId: inscSeleccionada.disciplina.id,
              subConceptoId: null,
              cuota: cantidad.toString(),
              montoOriginal: total,
              bonificacionId: null,
              recargoId: null,
              importePendiente: total,
              aCobrar: total,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              stockId: null,
              autoGenerated: false,
              aFavor: 0,
            });
            added = true;
          }
        }
      }
      // Agregar detalle basado en stock seleccionado
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
              cuota: cantidad.toString(),
              montoOriginal: total,
              bonificacionId: null,
              recargoId: null,
              importePendiente: total,
              aCobrar: total,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              stockId: selectedStock.id,
              autoGenerated: false,
              aFavor: 0,
            });
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
        !(values.inscripcion || values.disciplina) &&
        !values.stockSeleccionado
      ) {
        toast.error("Seleccione al menos un conjunto de campos para agregar");
      }
    },
    [conceptos, inscripcionesQuery.data, stocks]
  );

  // onSubmit: Mapea los valores y envía el registro al backend
  const onSubmit = async (values: CobranzasFormValues, actions: any) => {
    // Validar que la inscripción sea válida
    try {
      const detallesFiltrados = values.detallePagos.filter(
        (detalle) => Number(detalle.importePendiente) !== 0
      );
      const pagoRegistroRequest = {
        alumno: {
          id: Number(values.alumnoId),
          nombre: values.alumno,
        },
        fecha: values.fecha,
        fechaVencimiento: values.fecha,
        monto: Number(values.totalACobrar),
        inscripcion: values.inscripcion, // Se envía la inscripción actualizada
        metodoPagoId: values.metodoPagoId ? Number(values.metodoPagoId) : 0,
        recargoAplicado: false,
        bonificacionAplicada: false,
        pagoMatricula: false,
        activo: true,
        detallePagos: detallesFiltrados.map<DetallePagoRegistroRequest>(
          (d) => ({
            id: d.id,
            descripcionConcepto: d.descripcionConcepto,
            conceptoId: d.conceptoId ?? null,
            subConceptoId: d.subConceptoId ?? null,
            cuota: d.cuota,
            montoOriginal: d.montoOriginal,
            bonificacionId: d.bonificacionId ? Number(d.bonificacionId) : null,
            recargoId: d.recargoId ? Number(d.recargoId) : null,
            aCobrar: d.aCobrar,
            importePendiente: d.importePendiente,
            cobrado: d.cobrado,
            mensualidadId: d.mensualidadId ?? null,
            matriculaId: d.matriculaId ?? null,
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
            <TotalsUpdater metodosPago={metodosPago} />
            <PaymentIdUpdater
              ultimoPago={alumnoData?.ultimoPago ?? undefined}
            />
            {alumnoData?.deudas && (
              <SyncDetalles
                deudaData={alumnoData.deudas}
                ultimoPago={alumnoData.ultimoPago}
              />
            )}
            <FormHeader
              alumnos={alumnos}
              handleAlumnoChange={handleAlumnoChange}
            />
            <ConceptosSection
              inscripcionesData={inscripcionesQuery.data}
              disciplinas={mappedDisciplinas}
              stocks={stocks}
              conceptos={conceptos}
              values={values}
              setFieldValue={setFieldValue}
              handleAgregarDetalle={handleAgregarDetalle}
            />
            <DetallesTable />
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
                  {values.metodoPagoId &&
                    (() => {
                      const selectedMetodo = metodosPago.find(
                        (mp: MetodoPagoResponse) =>
                          String(mp.id) === String(values.metodoPagoId)
                      );
                      const isDebit =
                        selectedMetodo &&
                        selectedMetodo.descripcion.toUpperCase() === "DEBITO";
                      return isDebit ? (
                        <p className="text-sm text-info">
                          Se ha agregado un recargo de ${selectedMetodo.recargo}{" "}
                          por DEBITO.
                        </p>
                      ) : null;
                    })()}
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
