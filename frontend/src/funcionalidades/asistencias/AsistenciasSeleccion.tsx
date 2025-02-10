// src/funcionalidades/asistencias/AsistenciasSeleccion.tsx
import React from "react";
import { Link } from "react-router-dom";

const AsistenciasSeleccion: React.FC = () => {
    return (
        <div className="page-container">
            <h1 className="page-title text-center">Seleccione una opción de Asistencias</h1>
            <div className="flex flex-col md:flex-row justify-center gap-8 my-8">
                <Link
                    to="/asistencias-mensuales"
                    className="page-button hover:bg-gray-200 transition-colors"
                    aria-label="Ir al formulario de asistencia mensual"
                >
                    Formulario Asistencia Mensual
                </Link>
                <Link
                    to="/asistencias-diarias"
                    className="page-button-secondary hover:bg-gray-200 transition-colors"
                    aria-label="Ver el listado de asistencias diarias"
                >
                    Asistencias Diarias
                </Link>
            </div>
        </div>
    );
};

export default AsistenciasSeleccion;
