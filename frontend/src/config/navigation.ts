import { Users, Music, GraduationCap, ClipboardCheck, CreditCard, BarChart3, UserCog, Shield } from "lucide-react"
import type { LucideIcon } from "lucide-react"

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
    icon: CreditCard,
    description: "Gestion de pagos, stocks y configuraciones generales",
    items: [
      {
        id: "rendicion-general",
        label: "Rendicion General",
        href: "/caja/planilla",
      },
      {
        id: "metodos-pago",
        label: "Metodos de Pago",
        href: "/metodos-pago",
      },
      {
        id: "conceptos",
        label: "Conceptos",
        href: "/conceptos",
      },
      {
        id: "subconceptos",
        label: "Subconceptos",
        href: "/subconceptos",
        description: "Gestion de subconceptos para conceptos",
      },
      {
        id: "stock",
        label: "Stock",
        href: "/stocks",
      },
      {
        id: "tipo-stocks",
        label: "Tipo Stocks",
        href: "/tipo-stocks",
      },
      {
        id: "generacion-cuotas",
        label: "Generacion de Cuotas",
        href: "/mensualidades",
      },
    ],
  },

  // CATEGORIA "Caja"
  {
    id: "caja",
    label: "Caja",
    icon: CreditCard,
    description: "Control de ingresos y movimientos diarios",
    items: [
      {
        id: "cobranza",
        label: "Cobranza",
        href: "/pagos/formulario",
      },
      {
        id: "liquidacion",
        label: "Liquidacion",
        href: "/liquidacion",
      },
      {
        id: "caja-del-dia",
        label: "Caja del Dia",
        href: "/caja/diaria",
      },
      {
        id: "rendicion-mensual",
        label: "Rendicion Mensual",
        href: "/caja/rendicion-mensual",
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
      },
      {
        id: "recargos",
        label: "Recargos",
        href: "/recargos",
      },
      {
        id: "salones",
        label: "Salones",
        href: "/salones",
      },
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
        href: "/alumnos/formulario",
      },
    ],
  },

  // CATEGORIA "Asistencias"
  {
    id: "asistencias",
    label: "Asistencias",
    icon: ClipboardCheck,
    description: "Control y seguimiento de asistencias",
    items: [
      {
        id: "asistencia-mensual",
        label: "Asistencia Mensual",
        href: "/asistencias-mensuales",
      },
      {
        id: "asistencias-alumnos",
        label: "Asist. Alumnos",
        href: "/asistencias/alumnos",
      },
    ],
  },

  // Items sueltos
  {
    id: "inscripciones",
    icon: ClipboardCheck,
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

