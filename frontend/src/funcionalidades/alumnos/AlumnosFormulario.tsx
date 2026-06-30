import { type FormEvent, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import alumnosApi from "../../api/alumnosApi";
import Boton from "../../componentes/comunes/Boton";
import type { AlumnoRegistro } from "../../types/types";

const emptyAlumno: AlumnoRegistro = {
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  fechaIncorporacion: "",
  celular1: "",
  celular2: "",
  email: "",
  documento: "",
  fechaDeBaja: null,
  nombrePadres: "",
  autorizadoParaSalirSolo: false,
  activo: true,
  otrasNotas: "",
};

const AlumnosFormulario = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const alumnoId = Number(searchParams.get("id")) || null;
  const [values, setValues] = useState<AlumnoRegistro>(emptyAlumno);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!alumnoId) return;
    alumnosApi
      .obtenerPorId(alumnoId)
      .then((alumno) =>
        setValues({
          id: alumno.id,
          nombre: alumno.nombre,
          apellido: alumno.apellido,
          fechaNacimiento: alumno.fechaNacimiento ?? "",
          fechaIncorporacion: alumno.fechaIncorporacion ?? "",
          celular1: alumno.celular1 ?? "",
          celular2: alumno.celular2 ?? "",
          email: alumno.email ?? "",
          documento: alumno.documento ?? "",
          fechaDeBaja: alumno.fechaDeBaja,
          nombrePadres: alumno.nombrePadres ?? "",
          autorizadoParaSalirSolo: alumno.autorizadoParaSalirSolo,
          activo: alumno.activo,
          otrasNotas: alumno.otrasNotas ?? "",
        })
      )
      .catch(() => toast.error("No se pudo cargar el alumno."));
  }, [alumnoId]);

  const change = (field: keyof AlumnoRegistro, value: string | boolean) =>
    setValues((current) => ({ ...current, [field]: value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    try {
      if (alumnoId) await alumnosApi.actualizar(alumnoId, values);
      else await alumnosApi.registrar(values);
      toast.success("Alumno guardado correctamente.");
      navigate("/alumnos");
    } catch {
      toast.error("No se pudo guardar el alumno.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">{alumnoId ? "Editar alumno" : "Nuevo alumno"}</h1>
      <form onSubmit={submit} className="formulario max-w-4xl mx-auto">
        <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
          <TextField label="Nombre" value={values.nombre} onChange={(value) => change("nombre", value)} required />
          <TextField label="Apellido" value={values.apellido} onChange={(value) => change("apellido", value)} required />
          <TextField label="Fecha de nacimiento" type="date" value={values.fechaNacimiento} onChange={(value) => change("fechaNacimiento", value)} />
          <TextField label="Fecha de incorporación" type="date" value={values.fechaIncorporacion} onChange={(value) => change("fechaIncorporacion", value)} />
          <TextField label="Documento" value={values.documento ?? ""} onChange={(value) => change("documento", value)} />
          <TextField label="Email" type="email" value={values.email ?? ""} onChange={(value) => change("email", value)} />
          <TextField label="Celular principal" value={values.celular1 ?? ""} onChange={(value) => change("celular1", value)} />
          <TextField label="Celular alternativo" value={values.celular2 ?? ""} onChange={(value) => change("celular2", value)} />
          <TextField label="Padres o responsables" value={values.nombrePadres ?? ""} onChange={(value) => change("nombrePadres", value)} />
          <label className="auth-label flex items-center gap-2">
            <input type="checkbox" checked={values.autorizadoParaSalirSolo ?? false} onChange={(event) => change("autorizadoParaSalirSolo", event.target.checked)} />
            Autorizado para salir solo
          </label>
          {alumnoId && (
            <label className="auth-label flex items-center gap-2">
              <input type="checkbox" checked={values.activo} onChange={(event) => change("activo", event.target.checked)} />
              Activo
            </label>
          )}
          <label className="auth-label sm:col-span-2">
            Otras notas
            <textarea className="form-input" value={values.otrasNotas ?? ""} onChange={(event) => change("otrasNotas", event.target.value)} />
          </label>
        </div>
        <p className="text-sm text-gray-600 mt-4">
          Las inscripciones se gestionan en su pantalla específica para evitar duplicar el flujo y los cálculos financieros.
        </p>
        <div className="form-acciones">
          <Boton type="submit" disabled={saving} className="page-button">Guardar</Boton>
          <Boton type="button" onClick={() => navigate("/alumnos")} className="page-button-secondary">Cancelar</Boton>
        </div>
      </form>
    </div>
  );
};

const TextField = ({ label, value, onChange, type = "text", required = false }: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
}) => (
  <label className="auth-label">
    {label}
    <input className="form-input" type={type} value={value} required={required} onChange={(event) => onChange(event.target.value)} />
  </label>
);

export default AlumnosFormulario;
