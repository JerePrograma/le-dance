"use client"

import type React from "react"

import { Link } from "react-router-dom"
import { useSidebar } from "../hooks/context/SideBarContext"
import {
    LayoutDashboard,
    Calendar,
    Users,
    GraduationCap,
    CreditCard,
    ClipboardList,
    BarChart2,
    Settings,
    ShoppingBag,
    BookOpen,
    ChevronRight,
} from "lucide-react"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "./ui/accordion"
import { Card, CardContent, CardHeader } from "./card"
import { cn } from "../lib/utils"

interface FeatureCardProps {
    name: string
    description: string
    icon: React.ElementType
    href: string
}

const FeatureCard = ({ name, description, icon: Icon, href }: FeatureCardProps) => {
    return (
        <Link to={href} className="block">
            <Card className="group h-full transition-all hover:shadow-lg hover:-translate-y-1 duration-300">
                <CardHeader className="space-y-0">
                    <div className="flex items-center space-x-4">
                        <div className="rounded-xl bg-primary/10 p-3 transition-colors group-hover:bg-primary/20">
                            <Icon className="h-6 w-6 text-primary transition-transform group-hover:scale-110" />
                        </div>
                        <h3 className="text-xl font-semibold tracking-tight">{name}</h3>
                    </div>
                </CardHeader>
                <CardContent>
                    <p className="text-sm text-muted-foreground mb-4">{description}</p>
                    <div className="flex items-center text-sm font-medium text-primary opacity-0 transition-opacity group-hover:opacity-100">
                        Acceder
                        <ChevronRight className="ml-1 h-4 w-4 transition-transform group-hover:translate-x-1" />
                    </div>
                </CardContent>
            </Card>
        </Link>
    )
}

interface SectionProps {
    title: string
    description: string
    items: {
        name: string
        description: string
        icon: React.ElementType
        href: string
    }[]
    defaultOpen?: boolean
}

const Section = ({ title, description, items, defaultOpen = false }: SectionProps) => {
    return (
        <AccordionItem value={title} className="border-none">
            <AccordionTrigger className="gap-4 py-6 transition-all hover:no-underline [&[data-state=open]>div]:text-primary">
                <div className="text-left">
                    <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
                    <p className="text-muted-foreground font-normal">{description}</p>
                </div>
            </AccordionTrigger>
            <AccordionContent className="pt-2 pb-6">
                <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {items.map((item) => (
                        <FeatureCard key={item.name} {...item} />
                    ))}
                </div>
            </AccordionContent>
        </AccordionItem>
    )
}

export default function MainContent() {
    const { isExpanded } = useSidebar()

    const features = [
        {
            title: "Principal",
            description: "Gestión diaria y operaciones principales",
            items: [
                {
                    name: "Dashboard",
                    description: "Vista general del sistema",
                    icon: LayoutDashboard,
                    href: "/",
                },
                {
                    name: "Asistencias",
                    description: "Control de asistencia a clases",
                    icon: Calendar,
                    href: "/asistencias",
                },
                {
                    name: "Alumnos",
                    description: "Gestión de estudiantes",
                    icon: Users,
                    href: "/alumnos",
                },
                {
                    name: "Clases",
                    description: "Administración de disciplinas",
                    icon: GraduationCap,
                    href: "/clases",
                },
            ],
        },
        {
            title: "Administración",
            description: "Gestión financiera y administrativa",
            items: [
                {
                    name: "Pagos",
                    description: "Gestión de cobros y pagos",
                    icon: CreditCard,
                    href: "/pagos",
                },
                {
                    name: "Inscripciones",
                    description: "Registro de nuevos alumnos",
                    icon: ClipboardList,
                    href: "/inscripciones",
                },
                {
                    name: "Reportes",
                    description: "Informes y estadísticas",
                    icon: BarChart2,
                    href: "/reportes",
                },
            ],
        },
        {
            title: "Sistema",
            description: "Configuración y administración del sistema",
            items: [
                {
                    name: "Configuración",
                    description: "Ajustes generales",
                    icon: Settings,
                    href: "/configuracion",
                },
                {
                    name: "Stock",
                    description: "Control de inventario",
                    icon: ShoppingBag,
                    href: "/stock",
                },
                {
                    name: "Conceptos",
                    description: "Gestión de conceptos y categorías",
                    icon: BookOpen,
                    href: "/conceptos",
                },
            ],
        },
    ]

    return (
        <main
            className={cn("flex-1 transition-all duration-300", isExpanded ? "ml-[var(--sidebar-width)]" : "ml-[4.5rem]")}
        >
            <div className="container mx-auto p-6 space-y-8">
                <div className="space-y-2 text-center">
                    <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">Panel de Gestión</h1>
                    <p className="text-lg text-muted-foreground">Sistema de administración LE DANCE</p>
                </div>

                <Accordion type="single" collapsible defaultValue="Principal" className="space-y-6">
                    {features.map((section) => (
                        <Section key={section.title} {...section} />
                    ))}
                </Accordion>
            </div>
        </main>
    )
}

