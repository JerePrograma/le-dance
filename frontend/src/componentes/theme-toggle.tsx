"use client"

import { useEffect, useState } from "react"
import { Moon, Sun } from "lucide-react"

export function ThemeToggle() {
    const [theme, setTheme] = useState(
        typeof window !== "undefined" ? localStorage.getItem("theme") || "system" : "system",
    )

    useEffect(() => {
        localStorage.setItem("theme", theme)
        if (theme === "dark" || (theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches)) {
            document.documentElement.classList.add("dark")
        } else {
            document.documentElement.classList.remove("dark")
        }
    }, [theme])

    const toggleTheme = () => {
        setTheme(theme === "dark" ? "light" : "dark")
    }

    return (
        <button
            onClick={toggleTheme}
            aria-label="Toggle Dark Mode"
            className="group relative rounded-full bg-white/90 p-2 shadow-lg shadow-zinc-800/5 ring-1 ring-zinc-900/5 backdrop-blur transition dark:bg-zinc-800/90 dark:ring-white/10 dark:hover:ring-white/20"
        >
            <Sun className="h-5 w-5 transition-all dark:hidden" />
            <Moon className="hidden h-5 w-5 transition-all dark:block" />
            <span className="sr-only">Toggle theme</span>
        </button>
    )
}

