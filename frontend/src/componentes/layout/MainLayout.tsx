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
    <div className="min-h-screen bg-[hsl(var(--background))] pt-[var(--header-height)]">
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
        <Header />
        <div className="flex">
          <Sidebar />
          <main
            className={`flex-1 p-4 transition-all duration-300 ${isExpanded ? "md:ml-[var(--sidebar-width)]" : "md:ml-[4.5rem]"
              }`}
          >
            {children}
          </main>
        </div>
      </ThemeProvider>
    </div>
  )
}
