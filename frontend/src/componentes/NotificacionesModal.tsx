"use client";
import { useEffect, useState } from "react";
import api from "../api/axiosConfig";

interface NotificacionesModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function NotificacionesModal({
  isOpen,
  onClose,
}: NotificacionesModalProps) {
  const [notificaciones, setNotificaciones] = useState<string[]>([]);

  useEffect(() => {
    if (isOpen) {
      api
        .get("/notificaciones/cumpleaneros")
        .then((res) => setNotificaciones(res.data))
        .catch((err) => console.error("Error al cargar notificaciones", err));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    // Contenedor del modal (overlay)
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      {/* Cuerpo del modal */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6 w-96">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-800 dark:text-gray-200">
            Cumpleañeros de hoy
          </h2>
          <button
            onClick={onClose}
            aria-label="Cerrar modal"
            className="text-gray-600 dark:text-gray-300"
          >
            &times;
          </button>
        </div>
        {notificaciones.length > 0 ? (
          <ul className="space-y-2 max-h-60 overflow-y-auto">
            {notificaciones.map((notificacion, index) => (
              <li
                key={index}
                className="border-b last:border-none py-1 text-gray-700 dark:text-gray-300"
              >
                {notificacion}
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-gray-600 dark:text-gray-400">
            No hay notificaciones para hoy.
          </p>
        )}
      </div>
    </div>
  );
}
