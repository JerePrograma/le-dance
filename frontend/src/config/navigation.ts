// src/config/navigation.ts
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
  ShoppingCart,
  Tag,
} from "lucide-react";
import type React from "react";

export interface NavigationItem {
  id: string;
  icon?: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  label: string;
  href?: string;
  /** Subitems */
  items?: NavigationItem[];
}

/**
 * Ejemplo: "Administración" y "Caja" son
 * categorías con subitems. Profesores, Disciplinas, etc.
 * se dejan sueltos. Ajusta a tus rutas reales.
 */
export const navigationItems: NavigationItem[] = [
  // CATEGORÍA "Administración"
  {
    id: "administracion",
    label: "Administración",
    icon: CreditCard,
    items: [
      {
        id: "rendicion-general",
        label: "Rendición General",
        href: "/caja/planilla",
      },
      {
        id: "metodos-pago",
        label: "Métodos de Pago",
        href: "/metodos-pago",
      },
      {
        id: "conceptos",
        label: "Conceptos",
        href: "/conceptos",
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
        label: "Generación de Cuotas",
        href: "/mensualidades", // ajusta si es tu ruta
      },
    ],
  },

  // CATEGORÍA "Caja"
  {
    id: "caja",
    label: "Caja",
    icon: CreditCard,
    items: [
      {
        id: "cobranza",
        label: "Cobranza",
        href: "/pagos/formulario", // tu ruta real
      },
      {
        id: "liquidacion",
        label: "Liquidacion",
        href: "/liquidacion", // tu ruta real
      },
      {
        id: "caja-del-dia",
        label: "Caja del Día",
        href: "/caja/diaria",
      },
      {
        id: "rendicion-mensual",
        label: "Rendición Mensual",
        href: "/caja/rendicion-mensual", // crea esa ruta si la tienes
      },
    ],
  },

  // CATEGORÍA "Manejo general"
  {
    id: "manejo-general",
    label: "Manejo General",
    icon: ClipboardCheck,
    items: [
      {
        id: "bonificaciones",
        label: "Bonificaciones",
        href: "/bonificaciones",
      },
      {
        id: "recargos",
        label: "Recargos",
        href: "/recargos", // crea la ruta si la tienes
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

  // CATEGORÍA "Asistencias"
  {
    id: "asistencias",
    label: "Asistencias",
    icon: ClipboardCheck,
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

  // ============================================
  // Items sueltos
  // ============================================
  {
    id: "inscripciones",
    icon: ClipboardCheck,
    label: "Inscripciones",
    href: "/inscripciones",
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
