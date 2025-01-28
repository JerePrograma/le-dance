import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";

interface Bonificacion {
  id?: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

const BonificacionesFormulario: React.FC = () => {
  const [bonificacion, setBonificacion] = useState<Bonificacion>({
    descripcion: "",
    porcentajeDescuento: 0,
    activo: true,
    observaciones: "",
  });
  const [mensaje, setMensaje] = useState<string>("");
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id);
    }
  }, [searchParams]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    setBonificacion({
      ...bonificacion,
      [name]:
        type === "number" || name === "porcentajeDescuento"
          ? parseInt(value, 10)
          : value,
    });
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setBonificacion({ ...bonificacion, [name]: checked });
  };

  const handleBuscar = async (id: string) => {
    try {
      const response = await api.get<Bonificacion>(`/api/bonificaciones/${id}`);
      setBonificacion(response.data);
      setMensaje("Bonificación cargada correctamente.");
    } catch {
      setMensaje("Error al cargar la bonificación.");
    }
  };

  const handleGuardar = async () => {
    try {
      if (bonificacion.id) {
        await api.put(`/api/bonificaciones/${bonificacion.id}`, bonificacion);
        setMensaje("Bonificación actualizada correctamente.");
      } else {
        await api.post("/api/bonificaciones", bonificacion);
        setMensaje("Bonificación creada correctamente.");
      }
    } catch {
      setMensaje("Error al guardar la bonificación.");
    }
  };

  const handleLimpiar = () => {
    setBonificacion({
      descripcion: "",
      porcentajeDescuento: 0,
      activo: true,
      observaciones: "",
    });
    setMensaje("");
  };

  const handleVolverListado = () => {
    navigate("/bonificaciones");
  };

  return (
    <div className="formulario">
      <h1 className="form-titulo">Formulario de Bonificación</h1>
      <div className="form-grid">
        <input
          type="text"
          name="descripcion"
          value={bonificacion.descripcion}
          onChange={handleChange}
          placeholder="Descripción (Ej. 1/2 BECA)"
          className="form-input"
        />
        <input
          type="number"
          name="porcentajeDescuento"
          value={bonificacion.porcentajeDescuento || ""}
          onChange={handleChange}
          placeholder="Porcentaje de Descuento"
          className="form-input"
        />
        <textarea
          name="observaciones"
          value={bonificacion.observaciones || ""}
          onChange={handleChange}
          placeholder="Observaciones (Opcional)"
          className="form-input"
        />
        <label className="form-checkbox">
          <input
            type="checkbox"
            name="activo"
            checked={bonificacion.activo}
            onChange={handleCheckboxChange}
          />
          Activo
        </label>
      </div>
      <div className="form-acciones">
        <Boton onClick={handleGuardar}>Guardar</Boton>
        <Boton onClick={handleLimpiar}>Limpiar</Boton>
        <Boton onClick={handleVolverListado}>Volver al Listado</Boton>
      </div>
      {mensaje && <p className="form-mensaje">{mensaje}</p>}
    </div>
  );
};

export default BonificacionesFormulario;
