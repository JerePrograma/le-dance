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
} from "lucide-react";

export interface NavigationItem {
  id: string;
  icon?: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  label: string;
  href?: string;
  items?: NavigationItem[];
}

export const navigationItems: NavigationItem[] = [
  {
    id: "profesores",
    icon: Users,
    label: "Profesores",
    href: "/profesores",
  },
  {
    id: "disciplinas",
    icon: Music,
    label: "Disciplinas",
    href: "/disciplinas",
  },
  {
    id: "alumnos",
    icon: GraduationCap,
    label: "Alumnos",
    href: "/alumnos",
  },
  {
    id: "salones",
    icon: ClipboardCheck,
    label: "Salones",
    href: "/salones",
  },
  {
    id: "asistencias",
    label: "Asistencias",
    href: "/asistencias",
  },
  {
    id: "bonificaciones",
    icon: Gift,
    label: "Bonificaciones",
    href: "/bonificaciones",
  },
  {
    id: "inscripciones",
    icon: ClipboardCheck,
    label: "Inscripciones",
    href: "/inscripciones",
  },
  {
    id: "pagos",
    icon: CreditCard,
    label: "Pagos",
    href: "/pagos",
  },
  {
    id: "caja",
    icon: CreditCard, // Puedes cambiar el icono si lo deseas
    label: "Caja",
    href: "/caja",
  },
  {
    id: "reportes",
    icon: BarChart3,
    label: "Reportes",
    href: "/reportes",
  },
  {
    id: "usuarios",
    icon: UserCog,
    label: "Usuarios",
    href: "/usuarios",
  },
  {
    id: "roles",
    icon: Shield,
    label: "Roles",
    href: "/roles",
  },
];
