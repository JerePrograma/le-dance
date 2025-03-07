import {
  Building2,
  Receipt,
  CreditCard,
  Wallet,
  Tags,
  Tag,
  Package,
  Calculator,
  DollarSign,
  PiggyBank,
  BadgeDollarSign,
  CalendarRange,
  FileSpreadsheet,
  Percent,
  TrendingUp,
  DoorOpen,
  UserCheck,
  Mic2,
  ClipboardCheck,
  CalendarCheck,
  ActivityIcon as AttendanceIcon,
  ClipboardList,
  BarChart3,
  UserCog,
  Shield,
  type LucideIcon,
  User,
} from "lucide-react";

export interface NavigationItem {
  id: string;
  icon?: LucideIcon;
  label: string;
  href?: string;
  description?: string;
  // Si se define, este item solo se muestra para usuarios con el rol indicado.
  requiredRole?: string;
  items?: NavigationItem[];
}

export const navigationItems: NavigationItem[] = [
  // CATEGORIA "Administracion"
  // Items sueltos
  {
    id: "alumnos",
    icon: User,
    label: "Alumnos",
    href: "/alumnos/formulario",
  },
  {
    id: "asistencias-alumnos",
    label: "Asist. Alumnos",
    href: "/asistencias/alumnos",
    icon: AttendanceIcon,
  },
  {
    id: "cobranza",
    label: "Cobranza",
    href: "/pagos/formulario",
    icon: DollarSign,
  },
  {
    id: "caja-del-dia",
    label: "Caja del Día",
    href: "/caja/diaria",
    icon: Wallet,
  },
  {
    id: "administracion",
    label: "Administracion",
    icon: Building2,
    description: "Gestión de pagos, stocks y configuraciones generales",
    items: [
      {
        id: "rendicion-general",
        label: "Rendición General",
        href: "/caja/planilla",
        icon: Receipt,
      },
      {
        id: "metodos-pago",
        label: "Métodos de Pago",
        href: "/metodos-pago",
        icon: CreditCard,
      },
      {
        id: "conceptos",
        label: "Conceptos",
        href: "/conceptos",
        icon: Tags,
      },
      {
        id: "subconceptos",
        label: "Subconceptos",
        href: "/subconceptos",
        description: "Gestión de subconceptos para conceptos",
        icon: Tag,
      },
      {
        id: "stock",
        label: "Stock",
        href: "/stocks",
        icon: Package,
      },
      {
        id: "generacion-cuotas",
        label: "Generación de Cuotas",
        href: "/mensualidades",
        icon: Calculator,
      },
    ],
  },

  // CATEGORIA "Caja"
  {
    id: "caja",
    label: "Caja",
    icon: PiggyBank,
    description: "Control de ingresos y movimientos diarios",
    items: [
      {
        id: "cobranza",
        label: "Cobranza",
        href: "/pagos/formulario",
        icon: DollarSign,
      },
      {
        id: "liquidacion",
        label: "Liquidación",
        href: "/liquidacion",
        icon: BadgeDollarSign,
      },
      {
        id: "caja-del-dia",
        label: "Caja del Día",
        href: "/caja/diaria",
        icon: Wallet,
      },
      {
        id: "rendicion-mensual",
        label: "Rendición Mensual",
        href: "/caja/rendicion-mensual",
        icon: FileSpreadsheet,
      },
    ],
  },

  // CATEGORIA "Manejo general"
  {
    id: "manejo-general",
    label: "Manejo General",
    icon: ClipboardCheck,
    description: "Gestión de alumnos, profesores y disciplinas",
    items: [
      {
        id: "bonificaciones",
        label: "Bonificaciones",
        href: "/bonificaciones",
        icon: Percent,
      },
      {
        id: "recargos",
        label: "Recargos",
        href: "/recargos",
        icon: TrendingUp,
      },
      {
        id: "salones",
        label: "Salones",
        href: "/salones",
        icon: DoorOpen,
      },
      {
        id: "profesores",
        icon: UserCheck,
        label: "Profesores",
        href: "/profesores",
      },
      {
        id: "disciplinas",
        icon: Mic2,
        label: "Disciplinas",
        href: "/disciplinas",
      },
      {
        id: "alumnos",
        icon: User,
        label: "Alumnos",
        href: "/alumnos/formulario",
      },
    ],
  },

  // CATEGORIA "Asistencias"
  {
    id: "asistencias",
    label: "Asistencias",
    icon: CalendarCheck,
    description: "Control y seguimiento de asistencias",
    items: [
      {
        id: "asistencia-mensual",
        label: "Asistencia Mensual",
        href: "/asistencias-mensuales",
        icon: CalendarRange,
      },
      {
        id: "asistencias-alumnos",
        label: "Asist. Alumnos",
        href: "/asistencias/alumnos",
        icon: AttendanceIcon,
      },
    ],
  },
  {
    id: "gestion",
    label: "Gestión",
    icon: CalendarCheck,
    description: "Gestión general",
    items: [
      {
        id: "reportes",
        icon: BarChart3,
        label: "Reportes",
        href: "/reportes",
        description: "Informes y estadísticas",
      },
      {
        id: "usuarios",
        icon: UserCog,
        label: "Usuarios",
        href: "/usuarios",
        description: "Administración de usuarios",
        requiredRole: "ADMINISTRADOR", // Solo se muestra a administradores
      },
      {
        id: "roles",
        icon: Shield,
        label: "Roles",
        href: "/roles",
        description: "Gestión de permisos y roles",
        requiredRole: "ADMINISTRADOR",
      },
      {
        id: "Alumnos activos",
        icon: ClipboardList,
        label: "Inscripciones",
        href: "/inscripciones",
        description: "Gestión de Inscripciones",
      },
    ],
  },
];
