import type { ReactNode } from "react"
import Header from "../Header"
import Sidebar from "../Sidebar"
import { useSidebar } from "../../hooks/context/SideBarContext"
import { ThemeProvider } from "../theme-provider"

interface MainLayoutProps {
  children: ReactNode
}

export default function MainLayout({ children }: MainLayoutProps) {
  const { isExpanded } = useSidebar()

  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex">
          <Sidebar />
          <main
            className="flex-1 transition-all duration-300"
            style={{
              marginLeft: isExpanded ? "var(--sidebar-width)" : "4.5rem",
            }}
          >
            <div className="container py-6">{children}</div>
          </main>
        </div>
      </div>
    </ThemeProvider>
  )
}