import {
  BarChart3,
  Building2,
  CalendarCheck,
  CreditCard,
  DollarSign,
  DoorOpen,
  Mic2,
  Package,
  Percent,
  PiggyBank,
  Receipt,
  Shield,
  Tags,
  TrendingUp,
  User,
  UserCheck,
  UserCog,
  Wallet,
  type LucideIcon,
} from "lucide-react";

export interface NavigationItem {
  id: string;
  icon?: LucideIcon;
  label: string;
  href?: string;
  description?: string;
  requiredRole?: string;
  items?: NavigationItem[];
}

export const navigationItems: NavigationItem[] = [
  { id: "alumnos", icon: User, label: "Alumnos", href: "/alumnos" },
  { id: "cobranza", icon: DollarSign, label: "Cobranza", href: "/pagos/formulario" },
  { id: "pagos", icon: Receipt, label: "Pagos", href: "/pagos" },
  { id: "caja", icon: PiggyBank, label: "Caja", href: "/caja" },
  {
    id: "administracion",
    label: "Administración",
    icon: Building2,
    items: [
      { id: "egresos", label: "Egresos", href: "/egresos", icon: Wallet },
      { id: "metodos-pago", label: "Métodos de pago", href: "/metodos-pago", icon: CreditCard },
      { id: "conceptos", label: "Conceptos", href: "/conceptos", icon: Tags },
      { id: "stocks", label: "Stock", href: "/stocks", icon: Package },
    ],
  },
  {
    id: "academico",
    label: "Gestión académica",
    icon: CalendarCheck,
    items: [
      { id: "inscripciones", label: "Inscripciones", href: "/inscripciones", icon: CalendarCheck },
      { id: "asistencias", label: "Asistencias", href: "/asistencias/alumnos", icon: CalendarCheck },
      { id: "profesores", label: "Profesores", href: "/profesores", icon: UserCheck },
      { id: "disciplinas", label: "Disciplinas", href: "/disciplinas", icon: Mic2 },
      { id: "salones", label: "Salones", href: "/salones", icon: DoorOpen },
      { id: "bonificaciones", label: "Bonificaciones", href: "/bonificaciones", icon: Percent },
      { id: "recargos", label: "Recargos", href: "/recargos", icon: TrendingUp },
    ],
  },
  { id: "reportes", label: "Alumnos por disciplina", href: "/alumnos-por-disciplina", icon: BarChart3 },
  {
    id: "seguridad",
    label: "Seguridad",
    icon: Shield,
    requiredRole: "ADMINISTRADOR",
    items: [
      { id: "usuarios", label: "Usuarios", href: "/usuarios", icon: UserCog, requiredRole: "ADMINISTRADOR" },
      { id: "roles", label: "Roles", href: "/roles", icon: Shield, requiredRole: "ADMINISTRADOR" },
    ],
  },
];
