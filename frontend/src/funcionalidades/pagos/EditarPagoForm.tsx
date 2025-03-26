// src/forms/EditarPagoForm.tsx
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
import detallePagoApi from "../../api/detallesPagoApi"; // <-- Importamos el API de detallePago
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
} from "../../types/types";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";
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
  aplicarRecargo: true, // <-- Nuevo flag para controlar el recargo
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
    inscripciones: alumno.inscripciones
      ? alumno.inscripciones.map(normalizeInscripcion)
      : [],
    creditoAcumulado: alumno.creditoAcumulado,
  };
}

// ----- Nuevo Subcomponente: CobranzasFormHeader -----
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
          // Manejo de error si es necesario.
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
        {/* Recibo Nro */}
        <div>
          <label className="block font-medium">Recibo Nro:</label>
          <input
            type="text"
            readOnly
            className="border p-2 w-full"
            value={values.reciboNro}
          />
        </div>
        {/* Campo de búsqueda de Alumno */}
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
            <ul className="absolute w-full bg-gray-500 border border-gray-700 mt-1 z-10 rounded-md shadow-lg">
              {sugerencias.map((alumno, index) => (
                <li
                  key={alumno.id}
                  onClick={() => handleSelectAlumno(alumno)}
                  onMouseEnter={() => setActiveSuggestionIndex(index)}
                  className={`p-2 cursor-pointer ${
                    index === activeSuggestionIndex
                      ? "bg-slate-600"
                      : "hover:bg-gray-700"
                  }`}
                >
                  {alumno.nombre} {alumno.apellido}
                </li>
              ))}
            </ul>
          )}
        </div>
        {/* Fecha */}
        <div>
          <label className="block font-medium">Fecha:</label>
          <input
            type="date"
            className="border p-2 w-full"
            value={values.fecha}
            onChange={(e) => setFieldValue("fecha", e.target.value)}
          />
        </div>
      </div>
    </div>
  );
};

// ----- TotalsUpdater modificado -----
const TotalsUpdater: React.FC<{ metodosPago: MetodoPagoResponse[] }> = ({
  metodosPago,
}) => {
  const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

  useEffect(() => {
    // Sumar todos los "importePendiente" para Total Importe.
    const computedTotalImporte = values.detallePagos.reduce(
      (total, item) => total + Number(item.importePendiente || 0),
      0
    );

    // Sumar todos los "aCobrar" para Total a Cobrar.
    const computedTotalACobrar = values.detallePagos.reduce(
      (total, item) => total + Number(item.aCobrar || 0),
      0
    );

    // Incorporar el recargo solo si se aplica.
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

    // Actualizar los totales en el formulario.
    if (values.totalACobrar !== newTotalImporte) {
      setFieldValue("totalACobrar", newTotalImporte);
    }
    if (values.totalCobrado !== computedTotalACobrar) {
      setFieldValue("totalCobrado", computedTotalACobrar);
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

// ----- DetallesTable (Refactorizado para usar eliminarDetallePago con confirmación) -----
const DetallesTable: React.FC = () => (
  <FieldArray name="detallePagos">
    {({ remove, form }) => {
      // Función para manejar la eliminación con confirmación.
      const handleDeleteDetalle = async (index: number) => {
        const detalle = form.values.detallePagos[index];
        if (window.confirm("¿Está seguro de eliminar este detalle?")) {
          // Si el detalle ya fue persistido (tiene un id válido), se invoca el API.
          if (detalle.id && Number(detalle.id) !== 0) {
            try {
              await detallePagoApi.eliminarDetallePago(detalle.id);
              toast.success("Detalle eliminado correctamente");
            } catch (error) {
              toast.error("Error al eliminar el detalle");
              return;
            }
          }
          // Remover el detalle del arreglo del formulario.
          remove(index);
        }
      };

      return (
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
                      <Field name={`detallePagos.${index}.importePendiente`}>
                        {({ field, form }: any) => (
                          <input
                            type="number"
                            {...field}
                            onChange={(e) => {
                              const newValue = e.target.value;
                              // Actualiza importePendiente
                              form.setFieldValue(
                                `detallePagos.${index}.importePendiente`,
                                newValue
                              );
                              // Siempre sincroniza aCobrar con el nuevo valor
                              form.setFieldValue(
                                `detallePagos.${index}.aCobrar`,
                                newValue
                              );
                            }}
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
                        onClick={() => handleDeleteDetalle(index)}
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
      );
    }}
  </FieldArray>
);

// ----- Componente Principal -----
const EditarPagoForm: React.FC = () => {
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
      : [];

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
      ) => Promise<void | FormikErrors<CobranzasFormValues>>
    ) => {
      const id = Number(alumnoIdStr);
      await setFieldValue("alumnoId", id);
      setSelectedAlumnoId(id);
      await setFieldValue("matriculaRemoved", false);
      let alumnoInfo = alumnoData?.alumno;
      if (!alumnoInfo) {
        try {
          alumnoInfo = await alumnosApi.obtenerPorId(id);
        } catch (error) {}
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
    [alumnoData]
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
            reciboNro: pagoData.id.toString(),
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
              cuotaOCantidad: detalle.cuotaOCantidad || "1",
              valorBase: detalle.valorBase,
              importePendiente:
                detalle.importePendiente == null ||
                Number(detalle.importePendiente) === 0
                  ? detalle.valorBase
                  : detalle.importePendiente,
              bonificacionId: detalle.bonificacion
                ? detalle.bonificacion.id
                : null,
              recargoId: detalle.recargo ? detalle.recargo.id : null,
              aCobrar:
                detalle.importePendiente == null ||
                Number(detalle.importePendiente) === 0
                  ? detalle.valorBase
                  : detalle.importePendiente,
              cobrado: detalle.cobrado,
              mensualidadId: detalle.mensualidadId ?? null,
              matriculaId: detalle.matriculaId ?? null,
              importeInicial: detalle.importeInicial,
              stockId: detalle.stockId ?? null,
              version: detalle.version ?? null,
              pagoId: detalle.pagoId ?? null,
              autoGenerated: true,
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
              importePendiente: selectedConcept.precio,
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
              importePendiente: total,
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
              importePendiente: total,
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
            importeInicial: d.importeInicial,
            version: d.version ?? null,
            pagoId: d.pagoId ?? null,
            autoGenerated: false,
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
      <Formik
        key={location.key}
        initialValues={initialValues}
        onSubmit={onSubmit}
        enableReinitialize
      >
        {({ values, setFieldValue }) => {
          const selectedMetodoPago = metodosPago.find(
            (mp: MetodoPagoResponse) => mp.id === Number(values.metodoPagoId)
          );

          // Este useEffect se puede omitir si TotalsUpdater se encarga de actualizar totalCobrado.
          useEffect(() => {
            if (values.metodoPagoId) {
              setFieldValue("totalCobrado", values.totalACobrar);
            }
          }, [values.metodoPagoId, values.totalACobrar, setFieldValue]);

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
                  onClick={() => {
                    setFieldValue("aplicarRecargo", false);
                    toast.info("Recargo quitado");
                  }}
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

export default EditarPagoForm;
