import { type FormEvent, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import bonificacionesApi from "../../api/bonificacionesApi";
import disciplinasApi from "../../api/disciplinasApi";
import inscripcionesApi from "../../api/inscripcionesApi";
import Boton from "../../componentes/comunes/Boton";
import type {
  BonificacionResponse,
  DisciplinaResponse,
  InscripcionRegistroRequest,
} from "../../types/types";

const emptyRequest: InscripcionRegistroRequest = {
  alumnoId: 0,
  disciplinaId: 0,
  bonificacionId: null,
  fechaInscripcion: "",
  costoParticular: "",
};

const InscripcionesFormulario = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const id = Number(searchParams.get("id")) || null;
  const presetAlumnoId = Number(searchParams.get("alumnoId")) || 0;
  const [values, setValues] = useState<InscripcionRegistroRequest>({
    ...emptyRequest,
    alumnoId: presetAlumnoId,
  });
  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([
      disciplinasApi.listarDisciplinas(),
      bonificacionesApi.listarBonificaciones(),
    ])
      .then(([disciplinasData, bonificacionesData]) => {
        setDisciplinas(disciplinasData);
        setBonificaciones(bonificacionesData.filter((item) => item.activo));
      })
      .catch(() => toast.error("No se pudieron cargar los datos del formulario."));
  }, []);

  useEffect(() => {
    if (!id) return;
    inscripcionesApi
      .obtenerPorId(id)
      .then((inscripcion) =>
        setValues({
          id: inscripcion.id,
          alumnoId: inscripcion.alumnoId,
          disciplinaId: inscripcion.disciplinaId,
          bonificacionId: inscripcion.bonificacionId ?? null,
          fechaInscripcion: inscripcion.fechaInscripcion,
          costoParticular: inscripcion.costoParticular ?? "",
        })
      )
      .catch(() => toast.error("No se pudo cargar la inscripción."));
  }, [id]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    try {
      if (id) await inscripcionesApi.actualizar(id, values);
      else await inscripcionesApi.crear(values);
      toast.success("Inscripción guardada correctamente.");
      navigate("/inscripciones");
    } catch {
      toast.error("No se pudo guardar la inscripción.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">{id ? "Editar inscripción" : "Nueva inscripción"}</h1>
      <form onSubmit={submit} className="formulario max-w-4xl mx-auto">
        <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label className="auth-label">Alumno ID<input className="form-input" type="number" min="1" required value={values.alumnoId || ""} onChange={(event) => setValues({ ...values, alumnoId: Number(event.target.value) })} /></label>
          <label className="auth-label">
            Disciplina
            <select className="form-input" required value={values.disciplinaId} onChange={(event) => setValues({ ...values, disciplinaId: Number(event.target.value) })}>
              <option value={0}>Seleccione una disciplina</option>
              {disciplinas.filter((item) => item.activo).map((disciplina) => <option key={disciplina.id} value={disciplina.id}>{disciplina.nombre}</option>)}
            </select>
          </label>
          <label className="auth-label">
            Bonificación opcional
            <select className="form-input" value={values.bonificacionId ?? ""} onChange={(event) => setValues({ ...values, bonificacionId: event.target.value ? Number(event.target.value) : null })}>
              <option value="">Sin bonificación</option>
              {bonificaciones.map((bonificacion) => <option key={bonificacion.id} value={bonificacion.id}>{bonificacion.descripcion}</option>)}
            </select>
          </label>
          <label className="auth-label">
            Fecha de inscripción
            <input className="form-input" type="date" required value={values.fechaInscripcion} onChange={(event) => setValues({ ...values, fechaInscripcion: event.target.value })} />
          </label>
          <label className="auth-label">
            Costo particular opcional
            <input className="form-input" type="text" inputMode="decimal" value={values.costoParticular ?? ""} onChange={(event) => setValues({ ...values, costoParticular: event.target.value })} />
          </label>
        </div>
        <p className="text-sm text-gray-600 mt-4">El backend calcula los cargos y saldos; este formulario sólo registra la inscripción y sus referencias explícitas.</p>
        <div className="form-acciones">
          <Boton type="submit" disabled={saving || values.alumnoId === 0 || values.disciplinaId === 0} className="page-button">Guardar</Boton>
          <Boton type="button" onClick={() => navigate("/inscripciones")} className="page-button-secondary">Cancelar</Boton>
        </div>
      </form>
    </div>
  );
};

export default InscripcionesFormulario;
