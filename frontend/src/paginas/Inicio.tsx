import { Link } from "react-router-dom";
import {
  Users,
  Music,
  GraduationCap,
  ClipboardCheck,
  Gift,
  CreditCard,
  BarChart3,
  UserCog,
  Shield,
  LogOut,
} from "lucide-react";
import { useAuth } from "../hooks/context/authContext";

const navigationItems = [
  { icon: Users, label: "Profesores", href: "/profesores" },
  { icon: Music, label: "Disciplinas", href: "/disciplinas" },
  { icon: GraduationCap, label: "Alumnos", href: "/alumnos" },
  { icon: ClipboardCheck, label: "Asistencias", href: "/asistencias" },
  { icon: Gift, label: "Bonificaciones", href: "/bonificaciones" },
  { icon: CreditCard, label: "Pagos", href: "/pagos" },
  { icon: BarChart3, label: "Reportes", href: "/reportes" },
  { icon: UserCog, label: "Usuarios", href: "/usuarios" },
  { icon: Shield, label: "Roles", href: "/roles" },
];

export default function Dashboard() {
  const { logout } = useAuth();

  return (
    <div className="min-h-screen bg-[#1a1f2e] text-white">
      <header className="border-b border-white/10">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold text-[#a78bfa]">LE DANCE</h1>
          <button
            onClick={logout}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-[#a78bfa] hover:bg-[#9061f9] transition-colors"
          >
            <LogOut className="w-5 h-5" />
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        <h2 className="text-3xl font-bold text-center mb-12">
          Panel de Gestión - LE DANCE
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {navigationItems.map((item) => (
            <Link
              key={item.href}
              to={item.href}
              className="block p-6 rounded-lg bg-[#242b3d] hover:bg-[#2f3850] transition-colors group"
            >
              <div className="flex items-center gap-4">
                <item.icon className="w-8 h-8 text-[#a78bfa] group-hover:text-[#9061f9] transition-colors" />
                <span className="text-xl font-medium">{item.label}</span>
              </div>
            </Link>
          ))}
        </div>
      </main>
    </div>
  );
}
