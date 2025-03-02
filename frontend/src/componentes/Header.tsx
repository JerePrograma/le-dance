"use client"

import { useTheme } from "next-themes"
import { useSidebar } from "../hooks/context/SideBarContext"
import { Search, Bell, User, Menu, Moon, Sun } from "lucide-react"

export default function Header() {
    const { toggleSidebar, isExpanded } = useSidebar()
    const { theme, setTheme } = useTheme()

    const toggleTheme = () => {
        setTheme(theme === "light" ? "dark" : "light")
    }

    return (
        <header
            className={`fixed top-0 right-0 z-50 flex h-[var(--header-height)] items-center border-b border-border bg-background/80 backdrop-blur-md px-4 transition-all duration-300 ${isExpanded ? "left-[var(--sidebar-width)]" : "left-[4.5rem]"
                } md:left-[var(--sidebar-width)]`}
        >
            <button onClick={toggleSidebar} className="md:hidden p-2 rounded-md hover:bg-muted mr-2">
                <Menu className="w-5 h-5" />
            </button>

            <div className="flex-1 flex items-center justify-between">
                <h1 className="text-xl font-bold">Panel de Gestión - LE DANCE</h1>

                <div className="flex items-center gap-4">
                    <div className="relative hidden md:block">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
                        <input
                            type="text"
                            placeholder="Buscar..."
                            className="pl-10 pr-4 py-2 rounded-full border border-border bg-muted focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 w-64"
                        />
                    </div>

                    <button onClick={toggleTheme} className="p-2 rounded-full hover:bg-muted relative">
                        <Sun className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
                        <Moon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
                        <span className="sr-only">Toggle theme</span>
                    </button>

                    <button className="p-2 rounded-full hover:bg-muted relative">
                        <Bell className="w-5 h-5" />
                        <span className="absolute top-1 right-1 w-2 h-2 bg-destructive rounded-full"></span>
                    </button>

                    <button className="flex items-center gap-2 p-2 rounded-md hover:bg-muted">
                        <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-primary-foreground">
                            <User className="w-5 h-5" />
                        </div>
                    </button>
                </div>
            </div>
        </header>
    )
}

