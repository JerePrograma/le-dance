"use client"

import { Link } from "react-router-dom"
import { useSidebar } from "../hooks/context/SideBarContext"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { cn } from "../componentes/lib/utils"
import { useAuth } from "../hooks/context/authContext"
import { navigationItems } from "../config/navigation"
import NavGroup from "./NavGroup" // Se puede extraer la lógica de NavGroup

export default function Sidebar() {
    const { isExpanded, toggleSidebar, mobileSidebarOpen, setMobileSidebarOpen } = useSidebar()
    const { hasRole } = useAuth()

    // Filtrar navegación según rol
    const filteredNavigation = navigationItems.filter((item) => {
        if (item.requiredRole && !hasRole(item.requiredRole)) return false
        if (item.items) {
            item.items = item.items.filter((subItem) =>
                subItem.requiredRole ? hasRole(subItem.requiredRole) : true
            )
        }
        return true
    })

    return (
        <>
            {/* Sidebar de escritorio */}
            <aside
                className={cn(
                    "hidden md:flex fixed inset-y-0 left-0 z-40 flex-col bg-[hsl(var(--background))] border-r border-[hsl(var(--border))] transition-all duration-300",
                    isExpanded ? "w-[var(--sidebar-width)]" : "w-[4.5rem]"
                )}
            >
                <div className="flex items-center h-[var(--header-height)] px-4 border-b border-[hsl(var(--border))]">
                    {isExpanded ? (
                        <Link to="/" className="text-xl font-bold text-[hsl(var(--primary))]">
                            LE DANCE
                        </Link>
                    ) : (
                        <Link to="/" className="text-xl font-bold text-[hsl(var(--primary))]">
                            LD
                        </Link>
                    )}
                    <button
                        onClick={toggleSidebar}
                        className="p-1 rounded-md hover:bg-[hsl(var(--muted))] ml-auto"
                        aria-label={isExpanded ? "Colapsar menú" : "Expandir menú"}
                    >
                        {isExpanded ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                    </button>
                </div>
                <nav className="flex-1 overflow-y-auto py-4 px-3">
                    {filteredNavigation.map((item) => (
                        <NavGroup key={item.id} item={item} isExpanded={isExpanded} />
                    ))}
                </nav>
            </aside>

            {/* Sidebar móvil (overlay) */}
            {mobileSidebarOpen && (
                <div className="md:hidden fixed inset-0 z-40 flex">
                    <div
                        className="fixed inset-0 bg-black/50"
                        onClick={() => setMobileSidebarOpen(false)}
                    />
                    <aside className="relative w-3/4 max-w-sm bg-[hsl(var(--background))] border-r border-[hsl(var(--border))]">
                        <div className="flex items-center h-[var(--header-height)] px-4 border-b border-[hsl(var(--border))]">
                            <Link to="/" className="text-xl font-bold text-[hsl(var(--primary))]">
                                LE DANCE
                            </Link>
                            <button
                                onClick={() => setMobileSidebarOpen(false)}
                                className="p-1 rounded-md hover:bg-[hsl(var(--muted))] ml-auto"
                                aria-label="Cerrar menú"
                            >
                                <ChevronLeft className="w-5 h-5" />
                            </button>
                        </div>
                        <nav className="flex-1 overflow-y-auto py-4 px-3">
                            {filteredNavigation.map((item) => (
                                <NavGroup key={item.id} item={item} isExpanded={true} />
                            ))}
                        </nav>
                    </aside>
                </div>
            )}
        </>
    )
}
