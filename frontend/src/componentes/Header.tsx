"use client"

import { useTheme } from "next-themes"
import { useSidebar } from "../hooks/context/SideBarContext"
import { Search, Bell, Menu, Moon, Sun } from "lucide-react"
import { cn } from "../componentes/lib/utils"

export default function Header() {
    const { isExpanded, setMobileSidebarOpen } = useSidebar()
    const { theme, setTheme } = useTheme()

    const toggleTheme = () => setTheme(theme === "light" ? "dark" : "light")

    return (
        <header
            className={cn(
                "fixed top-0 right-0 z-40 flex h-[var(--header-height)] items-center border-b border-[hsl(var(--border))] bg-[hsl(var(--background))]/80 backdrop-blur-md px-4 transition-all duration-300",
                "left-0", // por defecto en móvil, left = 0
                {
                    "md:left-[var(--sidebar-width)]": isExpanded,
                    "md:left-[4.5rem]": !isExpanded,
                }
            )}
        >
            {/* Botón para abrir el menú móvil */}
            <button
                onClick={() => setMobileSidebarOpen(true)}
                className="md:hidden p-2 rounded-md hover:bg-[hsl(var(--muted))] mr-2"
                aria-label="Abrir menú"
            >
                <Menu className="w-5 h-5" />
            </button>

            <div className="flex-1 flex items-center justify-between">
                <h1 className="text-xl font-bold text-[hsl(var(--foreground))]">
                    Panel de Gestión - LE DANCE
                </h1>

                <div className="flex items-center gap-4">
                    <div className="relative hidden md:block">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-[hsl(var(--muted-foreground))] w-4 h-4" />
                        <input
                            type="text"
                            placeholder="Buscar..."
                            className="pl-10 pr-4 py-2 rounded-full border border-[hsl(var(--border))] bg-[hsl(var(--muted))] focus:outline-none focus:border-[hsl(var(--primary))] focus:ring-2 focus:ring-[hsl(var(--primary))]/10 w-64"
                        />
                    </div>

                    <button
                        onClick={toggleTheme}
                        className="p-2 rounded-full hover:bg-[hsl(var(--muted))] relative flex items-center justify-center"
                        aria-label="Toggle theme"
                    >
                        {theme === "light" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
                        <span className="sr-only">Toggle theme</span>
                    </button>

                    <button
                        className="p-2 rounded-full hover:bg-[hsl(var(--muted))] relative"
                        aria-label="Notificaciones"
                    >
                        <Bell className="w-5 h-5" />
                        <span className="absolute top-1 right-1 w-2 h-2 bg-[hsl(var(--destructive))] rounded-full"></span>
                    </button>
                </div>
            </div>
        </header>
    )
}
