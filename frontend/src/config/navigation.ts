import {
  Building2,
  Receipt,
  CreditCard,
  Wallet,
  Tags,
  Tag,
  Package,
  Boxes,
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
  SchoolIcon as StudentIcon,
  ClipboardCheck,
  CalendarCheck,
  ActivityIcon as AttendanceIcon,
  ClipboardList,
  BarChart3,
  UserCog,
  Shield,
  type LucideIcon,
} from "lucide-react"

export interface NavigationItem {
  id: string
  icon?: LucideIcon
  label: string
  href?: string
  description?: string
  items?: NavigationItem[]
}

export const navigationItems: NavigationItem[] = [
  // CATEGORIA "Administracion"
  {
    id: "administracion",
    label: "Administracion",
    icon: Building2,
    description: "Gestion de pagos, stocks y configuraciones generales",
    items: [
      {
        id: "rendicion-general",
        label: "Rendicion General",
        href: "/caja/planilla",
        icon: Receipt,
      },
      {
        id: "metodos-pago",
        label: "Metodos de Pago",
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
        description: "Gestion de subconceptos para conceptos",
        icon: Tag,
      },
      {
        id: "stock",
        label: "Stock",
        href: "/stocks",
        icon: Package,
      },
      {
        id: "tipo-stocks",
        label: "Tipo Stocks",
        href: "/tipo-stocks",
        icon: Boxes,
      },
      {
        id: "generacion-cuotas",
        label: "Generacion de Cuotas",
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
        label: "Liquidacion",
        href: "/liquidacion",
        icon: BadgeDollarSign,
      },
      {
        id: "caja-del-dia",
        label: "Caja del Dia",
        href: "/caja/diaria",
        icon: Wallet,
      },
      {
        id: "rendicion-mensual",
        label: "Rendicion Mensual",
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
    description: "Gestion de alumnos, profesores y disciplinas",
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
        icon: StudentIcon,
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

  // Items sueltos
  {
    id: "inscripciones",
    icon: ClipboardList,
    label: "Inscripciones",
    href: "/inscripciones",
    description: "Gestion de nuevas inscripciones",
  },
  {
    id: "reportes",
    icon: BarChart3,
    label: "Reportes",
    href: "/reportes",
    description: "Informes y estadisticas",
  },
  {
    id: "usuarios",
    icon: UserCog,
    label: "Usuarios",
    href: "/usuarios",
    description: "Administracion de usuarios",
  },
  {
    id: "roles",
    icon: Shield,
    label: "Roles",
    href: "/roles",
    description: "Gestion de permisos y roles",
  },
]

