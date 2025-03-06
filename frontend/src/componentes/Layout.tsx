import type React from "react"
import { ThemeProvider } from "./theme-provider"
import Encabezado from "./Header"

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      <div className="min-h-screen bg-[hsl(var(--background))] text-[hsl(var(--foreground))]">
        <Encabezado />
        <main className="container mx-auto px-4 py-8 mt-16">{children}</main>
      </div>
    </ThemeProvider>
  )
}

export default Layout
