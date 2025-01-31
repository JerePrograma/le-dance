import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { alumnoEsquema } from "../../validaciones/alumnoEsquema";
import alumnosApi from "../../utilidades/alumnosApi";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import { toast } from "react-toastify";
import {
  AlumnoListadoResponse,
  AlumnoRequest,
  AlumnoResponse,
  InscripcionResponse,
} from "../../types/types";

const AlumnosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [alumnoId, setAlumnoId] = useState<number | null>(null);
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");

  // Búsqueda por nombre y sugerencias
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<
    AlumnoListadoResponse[]
  >([]);

  const initialValues: AlumnoRequest = {
    nombre: "",
    apellido: "",
    fechaNacimiento: "",
    fechaIncorporacion: "",
    celular1: "",
    celular2: "",
    email1: "",
    email2: "",
    documento: "",
    cuit: "",
    nombrePadres: "",
    autorizadoParaSalirSolo: false,
    activo: true,
    otrasNotas: "",
    cuotaTotal: 0,
  };

  const handleBuscar = useCallback(async (idStr: string, setValues: any) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        setMensaje("ID inválido");
        return;
      }
      const data: AlumnoResponse = await alumnosApi.obtenerAlumnoPorId(idNum);

      setValues({
        nombre: data.nombre,
        apellido: data.apellido,
        fechaNacimiento: data.fechaNacimiento || "",
        fechaIncorporacion: data.fechaIncorporacion || "",
        celular1: data.celular1 || "",
        celular2: data.celular2 || "",
        email1: data.email1 || "",
        email2: data.email2 || "",
        documento: data.documento || "",
        cuit: data.cuit || "",
        nombrePadres: data.nombrePadres || "",
        autorizadoParaSalirSolo: data.autorizadoParaSalirSolo || false,
        activo: data.activo ?? true,
        otrasNotas: data.otrasNotas || "",
        cuotaTotal: data.cuotaTotal || 0,
      });

      setAlumnoId(data.id);
      await cargarInscripcionesDelAlumno(data.id);
      setMensaje("Alumno cargado correctamente.");
    } catch (error) {
      console.error(error);
      setMensaje("No se encontró un alumno con ese ID.");
    }
  }, []);

  const cargarInscripcionesDelAlumno = async (id: number) => {
    try {
      const data = await inscripcionesApi.listarInscripcionesPorAlumno(id);
      setInscripciones(data);
    } catch (err) {
      console.error("Error al cargar inscripciones:", err);
      setInscripciones([]);
    }
  };

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) handleBuscar(idParam, () => {});
  }, [searchParams, handleBuscar]);

  const buscarAlumnosPorNombre = useCallback(async (nombre: string) => {
    if (nombre.length < 2) {
      setSugerenciasAlumnos([]);
      return;
    }
    try {
      const response = await alumnosApi.buscarPorNombre(nombre);
      setSugerenciasAlumnos(response);
    } catch (error) {
      console.error("Error al buscar alumnos:", error);
      setSugerenciasAlumnos([]);
    }
  }, []);

  const handleSeleccionarAlumno = async (
    id: number,
    nombreCompleto: string,
    setValues: any
  ) => {
    setNombreBusqueda(nombreCompleto);
    setIdBusqueda(id.toString());
    await handleBuscar(id.toString(), setValues);
    setSugerenciasAlumnos([]);
  };

  const handleGuardarAlumno = async (values: AlumnoRequest) => {
    try {
      if (alumnoId) {
        await alumnosApi.actualizarAlumno(alumnoId, values);
        toast.success("Alumno actualizado correctamente.");
      } else {
        const resp = await alumnosApi.registrarAlumno(values);
        setAlumnoId(resp.id);
        toast.success("Alumno creado correctamente.");
      }
    } catch (error) {
      toast.error("Error al guardar datos del alumno.");
    }
  };

  return (
    <div className="formulario">
      <h1 className="form-title">Ficha de Alumno</h1>

      <Formik
        initialValues={initialValues}
        validationSchema={alumnoEsquema}
        onSubmit={handleGuardarAlumno}
      >
        {({ setValues, isSubmitting }) => (
          <Form>
            <div className="form-busqueda">
              <label htmlFor="idBusqueda">Número de Alumno:</label>
              <input
                type="number"
                id="idBusqueda"
                value={idBusqueda}
                onChange={(e) => setIdBusqueda(e.target.value)}
                className="form-input"
              />
              <Boton onClick={() => handleBuscar(idBusqueda, setValues)}>
                Buscar
              </Boton>
            </div>

            {/* Buscador por nombre con sugerencias */}
            <div className="form-busqueda">
              <label htmlFor="nombreBusqueda">Buscar por Nombre:</label>
              <input
                type="text"
                id="nombreBusqueda"
                value={nombreBusqueda}
                onChange={(e) => {
                  setNombreBusqueda(e.target.value);
                  buscarAlumnosPorNombre(e.target.value);
                }}
                className="form-input"
              />
              {sugerenciasAlumnos.length > 0 && (
                <ul className="sugerencias-lista">
                  {sugerenciasAlumnos.map((alumno) => (
                    <li
                      key={alumno.id}
                      onClick={() =>
                        handleSeleccionarAlumno(
                          alumno.id,
                          `${alumno.nombre} ${alumno.apellido}`,
                          setValues
                        )
                      }
                      className="sugerencia-item"
                    >
                      <strong>{alumno.nombre}</strong> {alumno.apellido}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div>
              <label>Nombre:</label>
              <Field name="nombre" type="text" className="form-input" />
              <ErrorMessage name="nombre" component="div" className="error" />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="form-boton"
            >
              Guardar Alumno
            </button>
          </Form>
        )}
      </Formik>

      <h2>Inscripciones del Alumno</h2>
      {alumnoId ? (
        <Tabla
          encabezados={[
            "ID",
            "DisciplinaID",
            "BonificaciónID",
            "Notas",
            "Acciones",
          ]}
          datos={inscripciones}
          acciones={(fila) => (
            <button
              onClick={() =>
                navigate(`/inscripciones/formulario?id=${fila.id}`)
              }
              className="botones botones-primario"
            >
              Editar
            </button>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.disciplinaId,
            fila.bonificacionId ?? "N/A",
            fila.notas ?? "",
          ]}
        />
      ) : (
        <p>
          No se puede gestionar inscripciones hasta que guarde o cargue un
          Alumno.
        </p>
      )}

      {mensaje && <p className="form-mensaje">{mensaje}</p>}
    </div>
  );
};

export default AlumnosFormulario;
