"use client"

import React from "react"

import { Link } from "react-router-dom"
import { ChevronRight } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "../componentes/ui/card"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../componentes/ui/accordion"
import { navigationItems } from "../config/navigation"

interface FeatureCardProps {
    name: string
    description?: string
    icon?: React.ElementType
    href: string
}

const FeatureCard = ({ name, description, icon: Icon, href }: FeatureCardProps) => {
    return (
        <Link to={href}>
            <Card className="group h-full transition-all hover:shadow-lg hover:-translate-y-1 duration-300">
                <CardHeader>
                    <div className="flex items-center space-x-4">
                        {Icon && (
                            <div className="rounded-xl bg-primary/10 p-3 transition-colors group-hover:bg-primary/20">
                                <Icon className="h-6 w-6 text-primary transition-transform group-hover:scale-110" />
                            </div>
                        )}
                        <CardTitle className="text-xl">{name}</CardTitle>
                    </div>
                </CardHeader>
                {description && (
                    <CardContent>
                        <p className="text-sm text-muted-foreground mb-4">{description}</p>
                        <div className="flex items-center text-sm font-medium text-primary opacity-0 transition-opacity group-hover:opacity-100">
                            Acceder
                            <ChevronRight className="ml-1 h-4 w-4 transition-transform group-hover:translate-x-1" />
                        </div>
                    </CardContent>
                )}
            </Card>
        </Link>
    )
}

interface SectionProps {
    title: string
    description?: string
    items: {
        id: string
        label: string
        icon?: React.ElementType
        href?: string
        items?: any[]
    }[]
}

const Section = ({ title, description, items }: SectionProps) => {
    return (
        <AccordionItem value={title} className="border-none">
            <AccordionTrigger className="gap-4 py-6 transition-all hover:no-underline [&[data-state=open]>div]:text-primary">
                <div className="text-left">
                    <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
                    {description && <p className="text-muted-foreground font-normal">{description}</p>}
                </div>
            </AccordionTrigger>
            <AccordionContent className="pt-2 pb-6">
                <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {items.map((item) => (
                        <React.Fragment key={item.id}>
                            {/* Si el item tiene subitems, mostrarlos */}
                            {item.items ? (
                                item.items.map((subItem) => (
                                    <FeatureCard key={subItem.id} name={subItem.label} icon={subItem.icon} href={subItem.href || "#"} />
                                ))
                            ) : (
                                /* Si no tiene subitems, mostrar el item principal */
                                <FeatureCard name={item.label} icon={item.icon} href={item.href || "#"} />
                            )}
                        </React.Fragment>
                    ))}
                </div>
            </AccordionContent>
        </AccordionItem>
    )
}

export default function Home() {
    const categories = navigationItems.filter((item) => item.items)
    const singleItems = navigationItems.filter((item) => !item.items)

    return (
        <div className="container mx-auto p-6 space-y-8">
            <div className="space-y-2 text-center">
                <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">Panel de Gestión</h1>
                <p className="text-lg text-muted-foreground">Sistema de administración LE DANCE</p>
            </div>

            {/* Importante: value debe ser controlado o tener defaultValue */}
            <Accordion
                type="multiple" // Cambiado a multiple para permitir múltiples secciones abiertas
                defaultValue={["Administración"]} // Array para type="multiple"
                className="space-y-6"
            >
                {categories.map((category) => (
                    <Section
                        key={category.id}
                        title={category.label}
                        items={category.items ? [category, ...category.items] : [category]}
                    />
                ))}

                {singleItems.length > 0 && (
                    <Section title="Accesos Directos" description="Accesos rápidos a funciones principales" items={singleItems} />
                )}
            </Accordion>
        </div>
    )
}

