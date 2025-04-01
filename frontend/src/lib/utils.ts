/**
 * Utilidad para combinar clases de Tailwind CSS de manera eficiente
 *
 * Esta función combina clsx (para unir clases condicionalmente) con
 * tailwind-merge (para resolver conflictos entre clases de Tailwind)
 *
 * @example
 * // Uso básico
 * cn("text-red-500", "bg-blue-500")
 *
 * // Con condiciones
 * cn("text-lg", isActive && "font-bold", { "opacity-50": isDisabled })
 *
 * // Resolviendo conflictos (la última clase gana)
 * cn("text-red-500", "text-blue-500") // => "text-blue-500"
 */

import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Combina múltiples clases de Tailwind CSS resolviendo conflictos
 *
 * @param inputs - Lista de clases, objetos condicionales o arrays
 * @returns String con las clases combinadas y conflictos resueltos
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

/**
 * Formatea un valor monetario como string con formato de moneda
 *
 * @param amount - Cantidad a formatear
 * @param currency - Código de moneda (por defecto 'USD')
 * @param locale - Configuración regional (por defecto 'es-ES')
 * @returns String formateado (ej: "$1,234.56")
 */
export function formatCurrency(
  amount: number,
  currency: string = "USD",
  locale: string = "es-ES"
): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

/**
 * Formatea una fecha como string según la configuración regional
 *
 * @param date - Fecha a formatear
 * @param locale - Configuración regional (por defecto 'es-ES')
 * @param options - Opciones de formato
 * @returns String de fecha formateada
 */
export function formatDate(
  date: Date | string | number,
  locale: string = "es-ES",
  options: Intl.DateTimeFormatOptions = {
    day: "numeric",
    month: "long",
    year: "numeric",
  }
): string {
  const dateObj = date instanceof Date ? date : new Date(date);
  return new Intl.DateTimeFormat(locale, options).format(dateObj);
}

/**
 * Trunca un texto a una longitud máxima y añade puntos suspensivos
 *
 * @param text - Texto a truncar
 * @param maxLength - Longitud máxima (por defecto 100)
 * @returns Texto truncado con puntos suspensivos si es necesario
 */
export function truncateText(text: string, maxLength: number = 100): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + "...";
}
