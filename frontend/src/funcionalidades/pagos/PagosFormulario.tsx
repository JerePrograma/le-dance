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
import alumnosApi from "../../api/alumnosApi";
import inscripcionesApi from "../../api/inscripcionesApi";
import { useCobranzasData } from "../../hooks/useCobranzasData";
import { useAlumnoData } from "../../hooks/useAlumnoData";
import useDebounce from "../../hooks/useDebounce";
import type {
  CobranzasFormValues,
  DetallePagoRegistroRequest,
  DetallePagoRegistroRequestExt,
  ConceptoResponse,
  DisciplinaDetalleResponse,
  MetodoPagoResponse,
  StockResponse,
  AlumnoRegistroRequest,
  AlumnoResponse,
  PagoRegistroRequest,
  AlumnoDataResponse,
  InscripcionResponse,
  BonificacionResponse,
  RecargoResponse,
} from "../../types/types";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";
import { useSyncDetalles } from "../../hooks/context/useSyncDetalles";
import { normalizeInscripcion } from "./normalizeInscripcion";
import NumberInputWithoutScroll from "./NumberInputWithoutScroll";

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

const getCurrentDateGMT3 = (): string => {
  return new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  });
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

// ----- Valores iniciales -----
const defaultValues: CobranzasFormValues = {
  id: 0,
  reciboNro: 0,
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
  fecha: getCurrentDateGMT3(),
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
  aplicarRecargo: true,
  tieneRecargo: true,
  estadoPago: "",
  removido: false,
};

// ----- Normalización del alumno -----
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
    creditoAcumulado: alumno.creditoAcumulado,
    inscripciones: alumno.inscripciones
      ? alumno.inscripciones.map(normalizeInscripcion)
      : [],
  };
}

const splitConceptAndCuota = (
  text: string
): { concept: string; cuota: string } => {
  if (!text) return { concept: "", cuota: "" };
  if (!text.includes("-")) {
    return { concept: text.trim(), cuota: "" };
  }
  const parts = text.split("-");
  return {
    concept: parts[0].trim(),
    cuota: parts.slice(1).join(" - ").trim(),
  };
};

// ----- Componente: CobranzasFormHeader -----
interface CobranzasFormHeaderProps {
  handleAlumnoChange: (
    alumnoIdStr: string,
    currentValues: CobranzasFormValues,
    setFieldValue: (
      field: string,
      value: any
    ) => Promise<void | FormikErrors<CobranzasFormValues>>
  ) => Promise<void>;
}

const CobranzasFormHeader: React.FC<CobranzasFormHeaderProps> = ({
  handleAlumnoChange,
}) => {
  const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerencias, setSugerencias] = useState<AlumnoResponse[]>([]);
  const [activeSuggestionIndex, setActiveSuggestionIndex] =
    useState<number>(-1);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const debouncedNombre = useDebounce(nombreBusqueda, 300);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchSugerencias = async () => {
      const query = debouncedNombre.trim();
      if (query !== "") {
        try {
          const resultados = await alumnosApi.buscarPorNombre(query);
          setSugerencias(resultados);
          setActiveSuggestionIndex(-1);
        } catch (error) {
          // Manejo de error (opcional)
        }
      } else {
        setSugerencias([]);
      }
    };
    fetchSugerencias();
  }, [debouncedNombre]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        wrapperRef.current &&
        !wrapperRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelectAlumno = async (alumno: AlumnoResponse) => {
    setNombreBusqueda(`${alumno.nombre} ${alumno.apellido}`);
    setShowSuggestions(false);
    await handleAlumnoChange(alumno.id.toString(), values, setFieldValue);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (sugerencias.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev < sugerencias.length - 1 ? prev + 1 : 0
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev > 0 ? prev - 1 : sugerencias.length - 1
        );
      } else if (e.key === "Enter") {
        e.preventDefault();
        if (
          activeSuggestionIndex >= 0 &&
          activeSuggestionIndex < sugerencias.length
        ) {
          handleSelectAlumno(sugerencias[activeSuggestionIndex]);
        }
      }
    }
  };

  return (
    <div className="border p-4 mb-4">
      <h2 className="font-bold mb-2">Datos de Cobranzas</h2>
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
        <div>
          <label className="block font-medium">Recibo Nro:</label>
          <input
            type="text"
            readOnly
            className="border p-2 w-full"
            value={values.reciboNro}
          />
        </div>
        <div className="sm:col-span-2 relative" ref={wrapperRef}>
          <label className="block font-medium">Alumno:</label>
          <input
            type="text"
            className="border p-2 w-full"
            value={nombreBusqueda}
            onChange={(e) => {
              setNombreBusqueda(e.target.value);
              setShowSuggestions(true);
            }}
            onKeyDown={handleKeyDown}
            onFocus={() => {
              if (nombreBusqueda.trim() !== "") {
                setShowSuggestions(true);
              }
            }}
            placeholder="Buscar por nombre..."
          />
          {showSuggestions && sugerencias.length > 0 && (
            <ul className="absolute w-full bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 mt-1 z-10 rounded-md shadow-lg">
              {sugerencias.map((alumno, index) => (
                <li
                  key={alumno.id}
                  onClick={() => handleSelectAlumno(alumno)}
                  onMouseEnter={() => setActiveSuggestionIndex(index)}
                  className={`p-2 cursor-pointer ${
                    index === activeSuggestionIndex
                      ? "bg-slate-200 dark:bg-slate-600"
                      : "hover:bg-gray-200 dark:hover:bg-gray-700"
                  }`}
                >
                  {alumno.nombre} {alumno.apellido}
                </li>
              ))}
            </ul>
          )}
        </div>
        <div>
          <label className="block font-medium">Fecha:</label>
          <input
            type="date"
            className="border p-2 w-full"
            value={values.fecha}
            onChange={(e) => {
              const selected = e.target.value;
              const gmt3Date = new Date(selected + "T00:00:00-03:00")
                .toISOString()
                .split("T")[0];
              setFieldValue("fecha", gmt3Date);
            }}
          />
        </div>
      </div>
    </div>
  );
};

// ----- TotalsUpdater -----
const TotalsUpdater: React.FC<{ metodosPago: MetodoPagoResponse[] }> = ({
  metodosPago,
}) => {
  const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

  useEffect(() => {
    const visibleDetalles = values.detallePagos.filter(
      (detalle: any) =>
        !detalle.autoGenerated ||
        (detalle.autoGenerated && !detalle.autoRemoved)
    );
    const computedTotalImporte = visibleDetalles.reduce(
      (total: number, item: any) => total + Number(item.importePendiente || 0),
      0
    );
    const computedTotalCobrado = visibleDetalles.reduce(
      (total: number, item: any) => total + Number(item.ACobrar || 0),
      0
    );

    let recargo = 0;
    if (values.metodoPagoId && values.aplicarRecargo) {
      const selectedMetodoPago = metodosPago.find(
        (mp: MetodoPagoResponse) => mp.id === Number(values.metodoPagoId)
      );
      if (selectedMetodoPago && selectedMetodoPago.recargo) {
        recargo = Number(selectedMetodoPago.recargo);
      }
    }

    const newTotalImporte = computedTotalImporte + recargo;
    const newTotalCobrado = computedTotalCobrado + recargo;

    if (values.totalACobrar !== newTotalImporte) {
      setFieldValue("totalACobrar", newTotalImporte);
    }
    if (values.totalCobrado !== newTotalCobrado) {
      setFieldValue("totalCobrado", newTotalCobrado);
    }
  }, [
    values.detallePagos,
    values.metodoPagoId,
    metodosPago,
    setFieldValue,
    values.aplicarRecargo,
  ]);

  return null;
};

// ----- ConceptosSection -----
// Nota: Se mantienen los controles para seleccionar disciplina, tarifa y (opcionalmente)
// un concepto o stock. Con ello se activa el flujo correspondiente en handleAgregarDetalle.
interface ConceptosSectionProps {
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
              <option key={disc.id} value={disc.nombre}>
                {disc.nombre}
              </option>
            ))}
          </Field>
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

// ----- DetallesTable -----
const DetallesTable: React.FC = () => {
  const { bonificaciones, recargos } = useCobranzasData();

  return (
    <FieldArray name="detallePagos">
      {({ form, remove }) => (
        <div className="overflow-x-auto">
          <table className="border mb-4 w-auto table-layout-auto">
            <thead className="bg-gray-50">
              <tr>
                <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[120px]">
                  Concepto
                </th>
                <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">
                  Cuota/Cantidad
                </th>
                <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">
                  Valor Base
                </th>
                <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[120px]">
                  Bonificación
                </th>
                <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[120px]">
                  Recargo
                </th>
                <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">
                  Importe Pendiente
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
                form.values.detallePagos.map((detalle: any, index: number) => {
                  if (detalle.autoGenerated && detalle.autoRemoved) return null;
                  const bonificacionNombre =
                    bonificaciones.find(
                      (b: BonificacionResponse) =>
                        b.id ===
                        (detalle.bonificacionId ?? detalle.bonificacion?.id)
                    )?.descripcion || "";
                  const recargoNombre = detalle.tieneRecargo
                    ? recargos.find(
                        (r: RecargoResponse) =>
                          r.id === Number(detalle.recargoId)
                      )?.descripcion || ""
                    : "";
                  return (
                    <tr key={index} className="hover:bg-gray-50">
                      <td className="border p-2 text-center text-sm">
                        <input
                          type="text"
                          value={
                            detalle.descripcionConcepto &&
                            detalle.descripcionConcepto.includes("-")
                              ? splitConceptAndCuota(
                                  detalle.descripcionConcepto
                                ).concept
                              : detalle.descripcionConcepto || ""
                          }
                          readOnly
                          className="w-full px-2 py-1 border rounded text-center"
                        />
                      </td>
                      <td className="border p-2 text-center text-sm">
                        <Field name={`detallePagos.${index}.cuotaOCantidad`}>
                          {({ field, form }: any) => (
                            <input
                              type={
                                detalle.tipo === "MENSUALIDAD"
                                  ? "text"
                                  : "number"
                              }
                              value={field.value}
                              {...field}
                              {...(detalle.tipo !== "MENSUALIDAD" && {
                                min: "1",
                              })}
                              onChange={(e) => {
                                const newValue = e.target.value;
                                form.setFieldValue(
                                  `detallePagos.${index}.cuotaOCantidad`,
                                  newValue
                                );
                                if (detalle.tipo !== "MENSUALIDAD") {
                                  const base =
                                    Number(
                                      form.values.detallePagos[index].valorBase
                                    ) || 0;
                                  const updatedTotal = base * Number(newValue);
                                  form.setFieldValue(
                                    `detallePagos.${index}.importePendiente`,
                                    updatedTotal
                                  );
                                  form.setFieldValue(
                                    `detallePagos.${index}.ACobrar`,
                                    updatedTotal
                                  );
                                }
                              }}
                              className="w-full px-2 py-1 border rounded text-center"
                            />
                          )}
                        </Field>
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
                        <input
                          type="text"
                          readOnly
                          value={bonificacionNombre}
                          className="w-full px-2 py-1 border rounded text-center"
                        />
                      </td>
                      <td className="border p-2 text-center text-sm">
                        <input
                          type="text"
                          readOnly
                          value={recargoNombre}
                          className="w-full px-2 py-1 border rounded text-center"
                        />
                      </td>
                      <td className="border p-2 text-center text-sm">
                        <Field name={`detallePagos.${index}.importePendiente`}>
                          {({ field, form }: any) => (
                            <NumberInputWithoutScroll
                              {...field}
                              onChange={(e) => {
                                const newValue = e.target.value;
                                form.setFieldValue(
                                  `detallePagos.${index}.importePendiente`,
                                  newValue
                                );
                                form.setFieldValue(
                                  `detallePagos.${index}.ACobrar`,
                                  newValue
                                );
                              }}
                              className="w-full px-2 py-1 border rounded text-center no-spinner"
                            />
                          )}
                        </Field>
                      </td>
                      <td className="border p-2 text-center text-sm">
                        <Field name={`detallePagos.${index}.ACobrar`}>
                          {({ field, form }: any) => (
                            <div>
                              <NumberInputWithoutScroll
                                {...field}
                                className="w-full px-2 py-1 border rounded text-center no-spinner"
                              />
                              {detalle.descripcionConcepto &&
                                detalle.descripcionConcepto
                                  .toUpperCase()
                                  .includes("MATRICULA") &&
                                form.values.alumno &&
                                form.values.alumno.creditoAcumulado && (
                                  <small className="block text-xs text-gray-600">
                                    Saldo a favor:{" "}
                                    {Number(
                                      form.values.alumno.creditoAcumulado
                                    ).toLocaleString()}
                                  </small>
                                )}
                            </div>
                          )}
                        </Field>
                      </td>
                      <td className="border p-2 text-center text-sm">
                        <button
                          type="button"
                          className="bg-red-500 hover:bg-red-600 text-white p-1 rounded text-xs transition-colors mx-auto block"
                          onClick={() => {
                            if (!detalle.autoGenerated) {
                              remove(index);
                            } else {
                              form.setFieldValue(
                                `detallePagos.${index}.ACobrar`,
                                0
                              );
                              form.setFieldValue(
                                `detallePagos.${index}.removido`,
                                true
                              );
                              form.setFieldValue(
                                `detallePagos.${index}.autoRemoved`,
                                true
                              );
                            }
                          }}
                        >
                          Eliminar
                        </button>
                      </td>
                    </tr>
                  );
                })
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
};

// ----- Componente Principal: CobranzasForm -----
const CobranzasForm: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [initialValues, setInitialValues] =
    useState<CobranzasFormValues>(defaultValues);
  const [selectedAlumnoId, setSelectedAlumnoId] = useState<number>(0);
  const [activeInscripciones, setActiveInscripciones] = useState<
    InscripcionResponse[]
  >([]);

  const { disciplinas, stocks, metodosPago, conceptos } = useCobranzasData();
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

  const filteredDisciplinas =
    activeInscripciones.length > 0
      ? mappedDisciplinas.filter((disc) =>
          activeInscripciones.some((ins) => ins.disciplina.id === disc.id)
        )
      : mappedDisciplinas;

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
        value: any
      ) => Promise<void | FormikErrors<CobranzasFormValues>>
    ) => {
      const id = Number(alumnoIdStr);
      await setFieldValue("alumnoId", id);
      setSelectedAlumnoId(id);
      await setFieldValue("matriculaRemoved", false);
      let alumnoInfo;
      try {
        alumnoInfo = await alumnosApi.obtenerPorId(id);
      } catch (error) {
        alumnoInfo = null;
      }
      if (alumnoInfo) {
        await setFieldValue("alumno", normalizeAlumno(alumnoInfo));
      } else {
        await setFieldValue("alumno", {
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
        });
      }
      try {
        const inscripcionesActivas =
          await inscripcionesApi.obtenerInscripcionesActivas(id);
        setActiveInscripciones(inscripcionesActivas);
      } catch (error) {
        setActiveInscripciones([]);
      }
    },
    []
  );

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) {
      pagosApi
        .obtenerPagoPorId(Number(idParam))
        .then((pagoData) => {
          setInitialValues({
            ...defaultValues,
            id: pagoData.id,
            reciboNro: pagoData.id,
            alumno: pagoData.alumno
              ? normalizeAlumno(pagoData.alumno)
              : defaultValues.alumno,
            alumnoId: pagoData.alumno ? pagoData.alumno.id : 0,
            fecha: pagoData.fecha || new Date().toISOString().split("T")[0],
            detallePagos: pagoData.detallePagos.map((detalle: any) => ({
              id: detalle.id,
              descripcionConcepto: detalle.descripcionConcepto ?? "",
              conceptoId: detalle.conceptoId ?? null,
              subConceptoId: detalle.subConceptoId ?? null,
              cuotaOCantidad:
                detalle.cuotaOCantidad ??
                (splitConceptAndCuota(detalle.descripcionConcepto || "")
                  .cuota ||
                  "1"),
              valorBase: detalle.valorBase,
              importeInicial: detalle.importeInicial ?? detalle.valorBase,
              importePendiente:
                detalle.importePendiente == null ||
                Number(detalle.importePendiente) === 0
                  ? detalle.valorBase
                  : detalle.importePendiente,
              bonificacionId: detalle.bonificacion
                ? detalle.bonificacion.id
                : null,
              bonificacionNombre: detalle.bonificacion
                ? detalle.bonificacion.descripcion
                : "",
              recargoId: detalle.recargo ? detalle.recargo.id : null,
              recargoNombre: detalle.recargo ? detalle.recargo.descripcion : "",
              ACobrar:
                detalle.importePendiente == null ||
                Number(detalle.importePendiente) === 0
                  ? detalle.valorBase
                  : detalle.importePendiente,
              cobrado: detalle.cobrado,
              mensualidadId: detalle.mensualidadId ?? null,
              matriculaId: detalle.matriculaId ?? null,
              stockId: detalle.stockId ?? null,
              version: detalle.version ?? null,
              pagoId: detalle.pagoId ?? null,
              tieneRecargo: detalle.tieneRecargo,
              autoGenerated: true,
              estadoPago: detalle.estadoPago,
              removido: detalle.removido,
            })),
            totalACobrar: pagoData.detallePagos.reduce(
              (sum: number, detalle: any) =>
                sum +
                (Number(detalle.importePendiente) === 0 ||
                detalle.importePendiente == null
                  ? Number(detalle.valorBase)
                  : Number(detalle.importePendiente)),
              0
            ),
            totalCobrado: pagoData.detallePagos.reduce(
              (sum: number, detalle: any) =>
                sum +
                (Number(detalle.importePendiente) === 0 ||
                detalle.importePendiente == null
                  ? Number(detalle.valorBase)
                  : Number(detalle.importePendiente)),
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

  interface Recargo {
    id: string;
    diaDelMesAplicacion: number; // 15 para recargo día 15 o 1 para mes vencido
    porcentaje?: number;
    valorFijo?: number;
  }

  // Función para calcular el valor del recargo, igual que en el backend.
  const calcularRecargo = (recargo: Recargo, base: number): number => {
    if (!recargo) return 0;
    const recargoFijo = recargo.valorFijo ? recargo.valorFijo : 0;
    const recargoPorcentaje = recargo.porcentaje
      ? (recargo.porcentaje / 100) * base
      : 0;
    return recargoFijo + recargoPorcentaje;
  };

  // Función para obtener la fecha actual en GMT‑3 (se usa la zona "America/Argentina/Buenos_Aires").
  const obtenerFechaGMT3 = (): Date => {
    const dateString = new Date().toLocaleString("en-US", {
      timeZone: "America/Argentina/Buenos_Aires",
    });
    return new Date(dateString);
  };

  // ---- REFACTORED: calculo de recargo por fecha ----
  // Ahora primero aplica 20% si la cuota está vencida (pasó el mes completo),
  // y 10% sólo si estamos entre el día 15 y fin de mes.
  const calcularRecargoPorFecha = (
    fechaCuota: Date,
    recargoDia15: Recargo,
    recargoMesVencido: Recargo,
    baseNeta: number
  ): number => {
    const hoy = obtenerFechaGMT3();

    // Si ya pasó el primer día del mes siguiente, aplico 20%
    const primerDiaSig = new Date(fechaCuota);
    primerDiaSig.setMonth(primerDiaSig.getMonth() + 1, 1);
    if (hoy >= primerDiaSig) {
      return calcularRecargo(recargoMesVencido, baseNeta);
    }
    // Else if estamos en el mismo mes y ya pasó el día 15, aplico 10%
    const dia15 = new Date(fechaCuota);
    dia15.setDate(recargoDia15.diaDelMesAplicacion);
    if (hoy >= dia15) {
      return calcularRecargo(recargoDia15, baseNeta);
    }

    return 0;
  };

  // Función que calcula el importe final (base, descuento y recargo) para cuotas.
  // Se aplica solo si no es matrícula.
  const calcularImporteFinal = (
    valorBase: number,
    inscripcion: InscripcionResponse | undefined,
    esMatricula: boolean,
    aplicarRecargo: boolean,
    fechaCuota: Date
  ): number => {
    if (esMatricula) {
      // Para matrícula se retorna el valor base sin descuentos ni recargos.
      return valorBase;
    }

    // 1) Calcula el descuento (si existe una bonificación en la inscripción)
    const descuento =
      inscripcion && inscripcion.bonificacion
        ? valorBase * (inscripcion.bonificacion.porcentajeDescuento / 100)
        : 0;

    let recargoCalculado = 0;
    if (aplicarRecargo) {
      // 2) Definición de los recargos según la especificación:
      //    - día 15: 10%
      //    - mes vencido: 20%
      const recargoDia15: Recargo = {
        id: "dia15",
        diaDelMesAplicacion: 15,
        porcentaje: 10,
        valorFijo: 0,
      };
      const recargoMesVencido: Recargo = {
        id: "mesVencido",
        diaDelMesAplicacion: 1,
        porcentaje: 20,
        valorFijo: 0,
      };

      // 3) Aplica la lógica combinada: primero mes vencido, luego día 15
      recargoCalculado = calcularRecargoPorFecha(
        fechaCuota,
        recargoDia15,
        recargoMesVencido,
        valorBase - descuento // aplicamos sobre la base neta de descuento
      );
    }

    // 4) Importe final = base – descuento + recargo
    return valorBase - descuento + recargoCalculado;
  };

  // ----- handleAgregarDetalle refactorizado -----
  const handleAgregarDetalle = useCallback(
    async (
      values: CobranzasFormValues,
      setFieldValue: (field: string, value: any) => void
    ) => {
      const newDetails = [...values.detallePagos];
      let added = false;
      const isDuplicate = (conceptId: number) =>
        newDetails.some((detalle) => detalle.conceptoId === conceptId);
      const isDuplicateString = (desc: string) =>
        newDetails.some(
          (det) => det.descripcionConcepto.trim() === desc.trim()
        );

      // Si se seleccionó un concepto manualmente
      if (values.conceptoSeleccionado) {
        const selectedConcept = conceptos.find(
          (c: ConceptoResponse) =>
            c.id.toString() === values.conceptoSeleccionado
        );
        if (selectedConcept) {
          const descMayus = (selectedConcept.descripcion || "").toUpperCase();
          if (descMayus.includes("MATRICULA")) {
            // Caso Matrícula
            const credit = Number(values.alumno?.creditoAcumulado || 0);
            const originalPrice = selectedConcept.precio;
            const usado = Math.min(originalPrice, credit);
            const total = Math.max(0, originalPrice - usado);
            const conceptoDetalle = `MATRICULA ${new Date().getFullYear()}`;
            try {
              await pagosApi.verificarMensualidadOMatricula({
                id: 0,
                version: 0,
                alumno: values.alumno,
                descripcionConcepto: conceptoDetalle,
                ACobrar: total,
                cobrado: false,
                valorBase: total,
              } as unknown as DetallePagoRegistroRequest);
              setFieldValue("alumno.creditoAcumulado", credit - usado);
              newDetails.push({
                id: 0,
                descripcionConcepto: conceptoDetalle,
                conceptoId: selectedConcept.id,
                subConceptoId: selectedConcept.subConcepto
                  ? selectedConcept.subConcepto.id
                  : null,
                cuotaOCantidad: conceptoDetalle.split(" ")[1] || "1",
                valorBase: total,
                importeInicial: total,
                importePendiente: total,
                bonificacionId: null,
                recargoId: null,
                ACobrar: total,
                cobrado: false,
                mensualidadId: null,
                matriculaId: null,
                stockId: null,
                version: selectedConcept.version,
                autoGenerated: false,
                pagoId: null,
                tieneRecargo: values.aplicarRecargo,
                modifiedACobrar: false,
                removido: false,
                tipo: "MATRICULA",
              } as unknown as DetallePagoRegistroRequestExt);
              added = true;
            } catch (error) {
              toast.error("MATRÍCULA YA COBRADA");
              return;
            }
          } else {
            // Para otros conceptos seleccionados
            if (isDuplicate(selectedConcept.id)) {
              toast.error("Concepto ya se encuentra agregado");
              return;
            } else {
              const cantidad = Number(values.cantidad) || 1;
              // En este caso usamos la parte posterior al primer guion, solo para CUOTA
              const { cuota } = splitConceptAndCuota(
                selectedConcept.descripcion
              );
              newDetails.push({
                id: 0,
                descripcionConcepto: String(selectedConcept.descripcion),
                conceptoId: selectedConcept.id,
                subConceptoId: selectedConcept.subConcepto
                  ? selectedConcept.subConcepto.id
                  : null,
                cuotaOCantidad:
                  values.tarifa === "CUOTA"
                    ? cuota || cantidad.toString()
                    : cantidad.toString(),
                valorBase: selectedConcept.precio,
                importeInicial: selectedConcept.precio * cantidad,
                importePendiente: selectedConcept.precio * cantidad,
                bonificacionId: null,
                recargoId: null,
                ACobrar: selectedConcept.precio * cantidad,
                cobrado: false,
                mensualidadId: null,
                matriculaId: null,
                stockId: null,
                version: selectedConcept.version,
                autoGenerated: false,
                pagoId: null,
                tieneRecargo: values.aplicarRecargo,
                modifiedACobrar: false,
                removido: false,
                tipo: values.tarifa === "CUOTA" ? "MENSUALIDAD" : "normal",
              } as unknown as DetallePagoRegistroRequestExt);
              added = true;
            }
          }
        }
      }

      // --- Helper: Convierte un período en formato "MARZO DE 2025" en una fecha (día 1 del mes) ---
      const obtenerFechaDePeriodo = (periodo: string): Date => {
        // La expresión regular acepta letras para el mes, opcionalmente "DE" y luego 4 dígitos para el año.
        const regex = /^([A-ZÁÉÍÓÚÑ]+)(?:\s+DE)?\s+(\d{4})$/i;
        const match = periodo.match(regex);
        if (!match) {
          console.error(
            `Formato de periodo inválido: "${periodo}". Se esperaba "MES [DE] AÑO".`
          );
          return new Date();
        }
        const mesStr = match[1].toUpperCase();
        const anio = parseInt(match[2], 10);

        const meses: Record<string, number> = {
          ENERO: 0,
          FEBRERO: 1,
          MARZO: 2,
          ABRIL: 3,
          MAYO: 4,
          JUNIO: 5,
          JULIO: 6,
          AGOSTO: 7,
          SEPTIEMBRE: 8,
          OCTUBRE: 9,
          NOVIEMBRE: 10,
          DICIEMBRE: 11,
        };

        const mes = meses[mesStr];
        if (mes === undefined) {
          console.error(
            `Mes inválido en periodo: "${mesStr}" en el periodo "${periodo}".`
          );
          return new Date();
        }

        const fecha = new Date(anio, mes, 1);
        console.log(
          `Fecha obtenida para el período "${periodo}":`,
          fecha.toISOString()
        );
        return fecha;
      };

      // --- Refactor del bloque CUOTA ---
      const selectedDisciplina = mappedDisciplinas.find(
        (disc) => disc.nombre === values.disciplina
      );
      if (selectedDisciplina) {
        // Se obtiene la cantidad (valor mínimo 1)
        const cantidad = Number(values.cantidad) || 1;
        let precio = 0;
        let tarifaLabel = "";
        let conceptoDetalle = "";
        let totalOriginal = 0;
        let totalDescontado = 0;

        switch (values.tarifa) {
          case "CUOTA": {
            // Se obtiene la fecha de cuota a partir del período mensual
            const fechaCuota = obtenerFechaDePeriodo(values.periodoMensual);

            // Asigna el precio y arma la descripción completa
            precio = selectedDisciplina.valorCuota;
            tarifaLabel = "CUOTA";
            conceptoDetalle = `${selectedDisciplina.nombre} - ${tarifaLabel} - ${values.periodoMensual}`;
            totalOriginal = precio * cantidad;

            console.log("Datos iniciales de cuota:", {
              precio,
              cantidad,
              totalOriginal,
              conceptoDetalle,
            });

            // Busca la inscripción activa para la disciplina (para aplicar bonificaciones si corresponde)
            const inscripcion = activeInscripciones.find(
              (ins) => ins.disciplina.id === selectedDisciplina.id
            );
            if (inscripcion) {
              console.log("Inscripción encontrada:", inscripcion);
            } else {
              console.log(
                "No se encontró inscripción activa para la disciplina:",
                selectedDisciplina.id
              );
            }

            // Calcula el importe final incluyendo descuentos y recargos
            totalDescontado = calcularImporteFinal(
              totalOriginal,
              inscripcion,
              false, // No es matrícula
              values.aplicarRecargo, // Bandera para aplicar recargo
              fechaCuota
            );
            console.log(
              "Importe final (descontado + recargo) calculado:",
              totalDescontado
            );

            // Se realiza la verificación de la mensualidad vía API
            try {
              await pagosApi.verificarMensualidadOMatricula({
                id: 0,
                version: 0,
                alumno: values.alumno,
                descripcionConcepto: conceptoDetalle,
                cuotaOCantidad: `${tarifaLabel} - ${values.periodoMensual}`,
                valorBase: totalOriginal,
                bonificacionId: null,
                recargoId: null,
                ACobrar: totalDescontado,
                cobrado: false,
                conceptoId: selectedDisciplina.id,
                subConceptoId: null,
                mensualidadId: null,
                matriculaId: null,
                stockId: null,
                pagoId: null,
                tieneRecargo: values.aplicarRecargo,
              } as unknown as DetallePagoRegistroRequest);
              console.log(
                "Verificación de mensualidad exitosa para:",
                conceptoDetalle
              );
            } catch (error) {
              console.error("Error al verificar la mensualidad:", error);
              toast.error("MENSUALIDAD YA COBRADA");
              return;
            }
            break;
          }
          case "CLASE_SUELTA":
            precio = selectedDisciplina.claseSuelta || 0;
            tarifaLabel = "CLASE SUELTA";
            conceptoDetalle = `${selectedDisciplina.nombre} - ${tarifaLabel}`;
            totalOriginal = precio * cantidad;
            totalDescontado = totalOriginal;
            // No se realiza verificación para clases sueltas.
            break;
          case "CLASE_PRUEBA":
            precio = selectedDisciplina.clasePrueba || 0;
            tarifaLabel = "CLASE DE PRUEBA";
            conceptoDetalle = `${selectedDisciplina.nombre} - ${tarifaLabel}`;
            totalOriginal = precio * cantidad;
            totalDescontado = totalOriginal;
            // No se realiza verificación para clase de prueba.
            break;
          default:
            break;
        }

        if (isDuplicateString(conceptoDetalle)) {
          toast.error("Concepto ya se encuentra agregado");
        } else {
          newDetails.push({
            id: 0,
            descripcionConcepto: conceptoDetalle,
            conceptoId: null,
            subConceptoId: null,
            cuotaOCantidad:
              values.tarifa === "CUOTA"
                ? `${tarifaLabel} - ${values.periodoMensual}`
                : cantidad.toString(),
            valorBase: totalOriginal,
            importeInicial: totalDescontado,
            importePendiente: totalDescontado,
            bonificacionId: null,
            recargoId: null,
            ACobrar: totalDescontado,
            cobrado: false,
            mensualidadId: null,
            matriculaId: null,
            stockId: null,
            autoGenerated: false,
            pagoId: null,
            tieneRecargo: values.aplicarRecargo,
            removido: false,
            tipo: values.tarifa === "CUOTA" ? "MENSUALIDAD" : "normal",
          } as unknown as DetallePagoRegistroRequestExt);
          added = true;
        }
      }

      // Si se seleccionó stock, se procesa de forma independiente
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
              importeInicial: total,
              importePendiente: total,
              bonificacionId: null,
              recargoId: null,
              ACobrar: total,
              cobrado: false,
              mensualidadId: null,
              matriculaId: null,
              stockId: selectedStock.id,
              version: selectedStock.version,
              autoGenerated: false,
              pagoId: null,
              tieneRecargo: values.aplicarRecargo,
              removido: false,
            } as unknown as DetallePagoRegistroRequestExt);
            added = true;
          }
        }
      }

      if (added) {
        setFieldValue("detallePagos", newDetails);
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
    [conceptos, mappedDisciplinas, stocks, activeInscripciones]
  );

  const onSubmit = async (values: CobranzasFormValues, actions: any) => {
    if (!values.detallePagos || values.detallePagos.length === 0) {
      toast.error("No hay nada que cobrar");
      return;
    }
    try {
      const {
        detallePagos,
        alumno,
        fecha,
        totalACobrar,
        metodoPagoId,
        aplicarRecargo,
      } = values;
      const usuarioStorage = localStorage.getItem("usuario");
      if (!usuarioStorage) {
        throw new Error("Usuario no autenticado");
      }
      const usuario = JSON.parse(usuarioStorage);
      const usuarioId = Number(usuario.id);
      const pagoRegistroRequest: PagoRegistroRequest = {
        alumno,
        fecha,
        fechaVencimiento: fecha,
        monto: Number(totalACobrar),
        importeInicial: Number(totalACobrar),
        metodoPagoId: Number(metodoPagoId) || 0,
        activo: true,
        detallePagos: detallePagos.map((d) => ({
          id: d.id,
          descripcionConcepto: d.descripcionConcepto,
          conceptoId: d.conceptoId ?? null,
          subConceptoId: d.subConceptoId ?? null,
          importePendiente: d.importePendiente,
          cuotaOCantidad: d.cuotaOCantidad,
          valorBase: d.valorBase,
          bonificacionId: d.bonificacionId ? Number(d.bonificacionId) : null,
          recargoId: d.recargoId ? Number(d.recargoId) : null,
          ACobrar: d.ACobrar,
          cobrado: d.cobrado,
          importeInicial: d.importeInicial,
          mensualidadId: d.mensualidadId ?? null,
          matriculaId: d.matriculaId ?? null,
          stockId: d.stockId ?? null,
          version: d.version ?? 0,
          pagoId: d.pagoId ?? null,
          tieneRecargo: aplicarRecargo ? true : false,
          autoGenerated: false,
          estadoPago: d.estadoPago,
          removido: d.removido || false,
        })),
        pagoMedios: [],
        observaciones: [
          values.observaciones,
          values.detallePagos
            .filter((detalle) => !detalle.removido)
            .map((detalle) => {
              const ACobrar = Number(detalle.ACobrar || 0);
              const pendiente = Number(detalle.importePendiente || 0);
              const concepto = detalle.descripcionConcepto || "";
              if (ACobrar === 0) {
                return `DEUDA ${concepto}`;
              } else if (ACobrar === pendiente) {
                return `SALDA ${concepto}`;
              } else if (ACobrar < pendiente) {
                return `CTA ${concepto}`;
              } else {
                return null;
              }
            })
            .filter(Boolean)
            .join("\n"),
        ]
          .filter(Boolean)
          .join("\n"),
        usuarioId,
      };

      await pagosApi.registrarPago(pagoRegistroRequest);
      toast.success("Cobranza registrada correctamente");
      actions.resetForm();
      navigate(`/pagos/alumno/${values.alumno.id}`);
    } catch (error: any) {
      const errorMsg =
        error.response?.data?.detalle ||
        error.response?.data?.message ||
        error.message ||
        "Error al registrar la cobranza";
      toast.error(errorMsg);
    }
  };

  return (
    <ResponsiveContainer className="py-4">
      <h1 className="page-title text-2xl font-bold mb-4">
        Gestión de Cobranzas
      </h1>
      <Formik
        key={location.key}
        initialValues={initialValues}
        onSubmit={onSubmit}
        enableReinitialize
      >
        {({ values, setFieldValue }) => {
          useEffect(() => {
            if (!values.metodoPagoId && metodosPago.length > 0) {
              const efectivo = metodosPago.find(
                (mp: MetodoPagoResponse) =>
                  mp.descripcion.toUpperCase() === "EFECTIVO"
              );
              if (efectivo) {
                setFieldValue("metodoPagoId", efectivo.id);
              }
            }
          }, [values.metodoPagoId, metodosPago, setFieldValue]);

          useEffect(() => {
            const nuevoTotalCobrado = values.detallePagos.reduce(
              (sum: number, detalle: any) => sum + Number(detalle.ACobrar || 0),
              0
            );
            if (nuevoTotalCobrado !== values.totalCobrado) {
              setFieldValue("totalCobrado", nuevoTotalCobrado);
            }
          }, [values.detallePagos, setFieldValue, values.totalCobrado]);

          const selectedMetodoPago = metodosPago.find(
            (mp: MetodoPagoResponse) => mp.id === Number(values.metodoPagoId)
          );

          useEffect(() => {
            if (values.metodoPagoId) {
              setFieldValue("totalCobrado", values.totalACobrar);
            }
          }, [values.metodoPagoId, values.totalACobrar, setFieldValue]);

          const handleQuitarRecargo = useCallback(() => {
            setFieldValue("aplicarRecargo", false);
            const updatedDetalles = values.detallePagos.map((detalle: any) => {
              if (detalle.mensualidadId) {
                return {
                  ...detalle,
                  importePendiente: detalle.importeInicial,
                  ACobrar: detalle.importeInicial,
                  tieneRecargo: false,
                };
              }
              return detalle;
            });
            setFieldValue("detallePagos", updatedDetalles);
          }, [setFieldValue, values.detallePagos]);

          return (
            <Form className="w-full">
              <TotalsUpdater metodosPago={metodosPago} />
              {alumnoData && <SyncDetalles deudaData={alumnoData} />}
              <CobranzasFormHeader handleAlumnoChange={handleAlumnoChange} />
              <ConceptosSection
                disciplinas={filteredDisciplinas}
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
                    {selectedMetodoPago && selectedMetodoPago.recargo && (
                      <p className="text-sm text-gray-500">
                        Recargo: {selectedMetodoPago.recargo}
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="block font-medium">Total Cobrado:</label>
                    <input
                      type="number"
                      readOnly
                      className="border p-2 w-full"
                      value={values.totalCobrado}
                    />
                  </div>
                  <div>
                    <label className="block font-medium">Método de Pago:</label>
                    <Field
                      as="select"
                      name="metodoPagoId"
                      className="border p-2 w-full"
                      onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                        const newMetodoId = e.target.value;
                        setFieldValue("metodoPagoId", newMetodoId);
                        setFieldValue("aplicarRecargo", true);
                        const selectedMetodo = metodosPago.find(
                          (mp: MetodoPagoResponse) =>
                            mp.id === Number(newMetodoId)
                        );
                        const recargo =
                          selectedMetodo && selectedMetodo.recargo
                            ? Number(selectedMetodo.recargo)
                            : 0;
                        const updatedDetalles = values.detallePagos.map(
                          (detalle: any) => {
                            if (detalle.autoGenerated && detalle.autoRemoved) {
                              return { ...detalle, ACobrar: 0 };
                            }
                            const defaultValue =
                              detalle.importePendiente == null ||
                              Number(detalle.importePendiente) === 0
                                ? Number(detalle.valorBase)
                                : Number(detalle.importePendiente);
                            return {
                              ...detalle,
                              ACobrar: defaultValue,
                              modifiedACobrar: false,
                            };
                          }
                        );
                        setFieldValue("detallePagos", updatedDetalles);
                        const computedImporte = updatedDetalles.reduce(
                          (sum: number, detalle: any) =>
                            sum + Number(detalle.ACobrar || 0),
                          0
                        );
                        const totalConRecargo = computedImporte + recargo;
                        setFieldValue("totalACobrar", totalConRecargo);
                        setFieldValue("totalCobrado", totalConRecargo);
                      }}
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
                  type="button"
                  className="bg-red-500 p-2 rounded text-white"
                  onClick={handleQuitarRecargo}
                >
                  Quitar Recargo
                </button>
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
                    onClick={() =>
                      navigate(`/pagos/alumno/${selectedAlumnoId}`)
                    }
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
          );
        }}
      </Formik>
    </ResponsiveContainer>
  );
};

export default CobranzasForm;
