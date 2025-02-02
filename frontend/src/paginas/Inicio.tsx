import type React from "react";
import { useNavigate } from "react-router-dom";
import {
  Users,
  Music2,
  GraduationCap,
  ClipboardCheck,
  Gift,
  CreditCard,
  BarChart2,
  UserCog,
  Shield,
} from "lucide-react";

const entidades = [
  { nombre: "Profesores", ruta: "/profesores", icono: Users },
  { nombre: "Disciplinas", ruta: "/disciplinas", icono: Music2 },
  { nombre: "Alumnos", ruta: "/alumnos", icono: GraduationCap },
  { nombre: "Asistencias", ruta: "/asistencias", icono: ClipboardCheck },
  { nombre: "Bonificaciones", ruta: "/bonificaciones", icono: Gift },
  { nombre: "Pagos", ruta: "/pagos", icono: CreditCard },
  { nombre: "Reportes", ruta: "/reportes", icono: BarChart2 },
  { nombre: "Usuarios", ruta: "/usuarios", icono: UserCog },
  { nombre: "Roles", ruta: "/roles", icono: Shield },
];

const Inicio: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="bg-background dark:bg-background-dark min-h-screen p-6 @container">
      <h1 className="text-3xl font-bold text-center text-text dark:text-text-dark mb-6">
        Panel de Gestión - LE DANCE
      </h1>

      <div className="grid grid-cols-1 @sm:grid-cols-2 @lg:grid-cols-3 gap-6">
        {entidades.map(({ nombre, ruta, icono: Icon }) => (
          <button
            key={nombre}
            className="group p-6 rounded-[var(--radius-lg)] shadow-lg 
                       bg-white dark:bg-gray-800 
                       transition-all duration-300 ease-[var(--ease-default)]
                       hover:shadow-xl hover:scale-105 hover:bg-primary/5 dark:hover:bg-primary/10
                       focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 dark:focus:ring-offset-background-dark"
            onClick={() => navigate(ruta)}
          >
            <div className="flex items-center space-x-4">
              <Icon className="w-8 h-8 text-primary group-hover:text-primary/80 transition-colors" />
              <h3 className="text-xl font-semibold text-text dark:text-text-dark group-hover:text-primary">
                {nombre}
              </h3>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
};

export default Inicio;
