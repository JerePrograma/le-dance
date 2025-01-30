/* InscripcionesFormulario.tsx */

import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import disciplinasApi from "../../utilidades/disciplinasApi";
import bonificacionesApi from "../../utilidades/bonificacionesApi";
import Boton from "../../componentes/comunes/Boton";

import {
  InscripcionRequest,
  InscripcionResponse,
  DisciplinaResponse,
  BonificacionResponse,
} from "../../types/types";

const InscripcionesFormulario: React.FC = () => {
  // Estado local del formulario
  const [inscripcionForm, setInscripcionForm] = useState<InscripcionRequest>({
    alumnoId: 0,
    disciplinaId: 0,
    bonificacionId: undefined,
    costoParticular: 0,
    notas: "",
  });

  // Listas para selects
  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>(
    []
  );

  // Manejo de si es "crear" o "editar"
  const [inscripcionId, setInscripcionId] = useState<number | null>(null);

  // Para mostrar mensajes al usuario
  const [mensaje, setMensaje] = useState("");

  // Hooks de routing
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // ======================================
  // Cargar Disciplinas / Bonificaciones
  // ======================================
  useEffect(() => {
    const fetchData = async () => {
      try {
        const [discData, bonData] = await Promise.all([
          disciplinasApi.listarDisciplinas(),
          bonificacionesApi.listarBonificaciones(),
        ]);
        setDisciplinas(discData);
        setBonificaciones(bonData);
      } catch (error) {
        console.error(error);
        setMensaje("Error al cargar disciplinas o bonificaciones.");
      }
    };
    fetchData();
  }, []);

  // ======================================
  // Determinar si estamos en modo edición o creación
  // ======================================
  const cargarInscripcion = useCallback(async (idStr: string) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        setMensaje("ID de inscripción inválido");
        return;
      }
      const data: InscripcionResponse =
        await inscripcionesApi.obtenerInscripcionPorId(idNum);
      setInscripcionForm({
        alumnoId: data.alumnoId,
        disciplinaId: data.disciplinaId,
        bonificacionId: data.bonificacionId,
        costoParticular: data.costoParticular ?? 0,
        notas: data.notas ?? "",
      });
      setInscripcionId(data.id);
      setMensaje("Inscripción cargada correctamente.");
    } catch (err) {
      console.error(err);
      setMensaje("No se encontró la inscripción con ese ID.");
    }
  }, []);

  useEffect(() => {
    const idParam = searchParams.get("id"); // ?id=XX => Editar
    const alumnoParam = searchParams.get("alumnoId"); // ?alumnoId=XX => Crear para un alumno
    if (idParam) {
      // Modo edición
      cargarInscripcion(idParam);
    } else if (alumnoParam) {
      // Modo creación, atado a un alumno
      const aId = Number(alumnoParam);
      if (!isNaN(aId)) {
        setInscripcionForm((prev) => ({ ...prev, alumnoId: aId }));
      }
    }
  }, [searchParams, cargarInscripcion]);

  // ======================================
  // Manejadores de inputs
  // ======================================
  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >
  ) => {
    const { name, value } = e.target;
    setInscripcionForm((prev) => ({
      ...prev,
      [name]: name === "costoParticular" ? Number(value) : value,
    }));
  };

  // Para selects de Disciplina y Bonificacion
  const handleSelectDisciplina = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = Number(e.target.value);
    setInscripcionForm((prev) => ({ ...prev, disciplinaId: val }));
  };

  const handleSelectBonificacion = (
    e: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const val = e.target.value === "" ? undefined : Number(e.target.value);

    setInscripcionForm((prev) => {
      let costoFinal = prev.costoParticular;

      if (val) {
        const bonificacionSeleccionada = bonificaciones.find(
          (b) => b.id === val
        );
        if (bonificacionSeleccionada) {
          costoFinal =
            prev.costoParticular -
            (prev.costoParticular *
              bonificacionSeleccionada.porcentajeDescuento) /
              100;
        }
      }

      return { ...prev, bonificacionId: val, costoParticular: costoFinal };
    });
  };

  // ======================================
  // Guardar (crear o actualizar)
  // ======================================
  const handleGuardar = async () => {
    // Validaciones mínimas
    if (!inscripcionForm.alumnoId || !inscripcionForm.disciplinaId) {
      setMensaje("Debes asignar un alumno y una disciplina.");
      return;
    }
    try {
      if (inscripcionId) {
        // Actualizar
        await inscripcionesApi.actualizarInscripcion(
          inscripcionId,
          inscripcionForm
        );
        setMensaje("Inscripción actualizada correctamente.");
      } else {
        // Crear
        const newIns = await inscripcionesApi.crearInscripcion(inscripcionForm);
        setInscripcionId(newIns.id);
        setMensaje("Inscripción creada correctamente.");
      }
    } catch (err) {
      console.error(err);
      setMensaje("Error al guardar la inscripción.");
    }
  };

  // ======================================
  // Limpiar
  // ======================================
  const handleLimpiar = () => {
    setInscripcionForm({
      alumnoId: 0,
      disciplinaId: 0,
      bonificacionId: undefined,
      costoParticular: 0,
      notas: "",
    });
    setInscripcionId(null);
    setMensaje("");
  };

  // ======================================
  // Volver
  // ======================================
  const handleVolver = () => {
    // Podrías volver a "/alumnos" o a la ficha del alumno
    if (inscripcionForm.alumnoId) {
      navigate(`/alumnos/formulario?id=${inscripcionForm.alumnoId}`);
    } else {
      navigate("/inscripciones");
    }
  };

  // ======================================
  // RENDER
  // ======================================
  return (
    <div className="formulario">
      <h1 className="form-title">
        {inscripcionId ? "Editar Inscripción" : "Nueva Inscripción"}
      </h1>

      {mensaje && <p className="form-mensaje">{mensaje}</p>}

      <div className="form-grid">
        <div>
          <label>Alumno ID:</label>
          <input
            name="alumnoId"
            type="number"
            value={inscripcionForm.alumnoId || ""}
            onChange={handleChange}
            disabled={!!inscripcionId}
            className="form-input"
          />
        </div>

        <div>
          <label>Disciplina:</label>
          <select
            value={inscripcionForm.disciplinaId}
            onChange={handleSelectDisciplina}
            className="form-input"
          >
            <option value={0}>-- Seleccionar --</option>
            {disciplinas.map((disc) => (
              <option key={disc.id} value={disc.id}>
                {disc.id} - {disc.nombre}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label>Bonificación:</label>
          <select
            value={inscripcionForm.bonificacionId || ""}
            onChange={handleSelectBonificacion}
            className="form-input"
          >
            <option value="">-- Ninguna --</option>
            {bonificaciones.map((bon) => (
              <option key={bon.id} value={bon.id}>
                {bon.descripcion}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label>Costo Particular:</label>
          <input
            name="costoParticular"
            type="number"
            value={inscripcionForm.costoParticular || 0}
            onChange={handleChange}
            className="form-input"
          />
        </div>

        <div>
          <label>Notas:</label>
          <textarea
            name="notas"
            value={inscripcionForm.notas || ""}
            onChange={handleChange}
            className="form-input"
          />
        </div>
      </div>

      <div className="form-acciones">
        <Boton onClick={handleGuardar}>Guardar</Boton>
        <Boton onClick={handleLimpiar}>Limpiar</Boton>
        <Boton onClick={handleVolver}>Volver</Boton>
      </div>
    </div>
  );
};

export default InscripcionesFormulario;
