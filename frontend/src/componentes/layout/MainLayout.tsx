"use client"

import type { ReactNode } from "react"
import Header from "../Header"
import Sidebar from "../Sidebar"
import { useSidebar } from "../../hooks/context/SideBarContext"
import { ThemeProvider } from "next-themes"

interface MainLayoutProps {
  children: ReactNode;
}

export default function MainLayout({ children }: MainLayoutProps) {
  const { isExpanded } = useSidebar()

  return (
    <div className="min-h-[100dvh] bg-[hsl(var(--background))] pt-[var(--header-height)]">
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
        <Header />
        <div className="flex w-full">
          <Sidebar />
          <main
            className={`flex-1 w-full transition-all duration-300 px-[var(--container-padding)] py-[clamp(1rem,2vh,2rem)] ${isExpanded ? "md:ml-[var(--sidebar-width)]" : "md:ml-[var(--sidebar-width-collapsed)]"}`}
          >
            {/* Contenedor principal sin restricción de ancho máximo */}
            <div className="mx-auto w-full">
              {children}
            </div>
          </main>
        </div>
      </ThemeProvider>
    </div>
  )
}
