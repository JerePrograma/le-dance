import React from "react";
import { useNavigate } from "react-router-dom";

const Inicio: React.FC = () => {
  const navigate = useNavigate();

  const entidades = [
    { nombre: "Profesores", ruta: "/profesores" },
    { nombre: "Disciplinas", ruta: "/disciplinas" },
    { nombre: "Alumnos", ruta: "/alumnos" },
    { nombre: "Asistencias", ruta: "/asistencias" },
    { nombre: "Bonificaciones", ruta: "/bonificaciones" },
    { nombre: "Pagos", ruta: "/pagos" },
    { nombre: "Reportes", ruta: "/reportes" },
    { nombre: "Usuarios", ruta: "/usuarios" },
    { nombre: "Roles", ruta: "/roles" },
  ];

  return (
    <div className="bg-gray-100 dark:bg-gray-900 min-h-screen p-6">
      {/* Título Principal */}
      <h1 className="text-3xl font-bold text-center text-gray-800 dark:text-gray-200 mb-6">
        Panel de Gestión - LE DANCE
      </h1>

      {/* Tablero de Entidades */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {entidades.map((entidad) => (
          <button
            key={entidad.nombre}
            className="tarjeta group p-6 rounded-lg shadow-lg bg-white dark:bg-gray-800 transition-transform transform hover:scale-105"
            onClick={() => navigate(entidad.ruta)}
          >
            <h3 className="text-xl font-semibold text-gray-700 dark:text-gray-300 group-hover:text-primary-500">
              {entidad.nombre}
            </h3>
          </button>
        ))}
      </div>
    </div>
  );
};

export default Inicio;
