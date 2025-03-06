import type React from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "../ui/button"
import { cn } from "../lib/utils"

interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (newPage: number) => void
  className?: string
}

const Pagination: React.FC<PaginationProps> = ({ currentPage, totalPages, onPageChange, className }) => {
  // Genera un array con los números de páginas
  const pages = Array.from({ length: totalPages }, (_, i) => i)

  // Determina qué páginas mostrar
  let pagesToShow = pages
  if (totalPages > 7) {
    if (currentPage < 3) {
      pagesToShow = [...pages.slice(0, 4), -1, ...pages.slice(totalPages - 1)]
    } else if (currentPage > totalPages - 4) {
      pagesToShow = [0, -1, ...pages.slice(totalPages - 4)]
    } else {
      pagesToShow = [0, -1, currentPage - 1, currentPage, currentPage + 1, -1, totalPages - 1]
    }
  }

  return (
    <nav
      role="navigation"
      aria-label="Navegación de páginas"
      className={cn("flex items-center justify-center gap-2", className)}
    >
      <Button
        variant="outline"
        size="icon"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        aria-label="Página anterior"
        className="h-8 w-8"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      <div className="flex items-center gap-1">
        {pagesToShow.map((page, index) => {
          if (page === -1) {
            return (
              <span key={`ellipsis-${index}`} className="px-2 text-muted-foreground">
                ...
              </span>
            )
          }

          return (
            <Button
              key={page}
              variant={page === currentPage ? "default" : "outline"}
              size="sm"
              onClick={() => onPageChange(page)}
              aria-label={`Ir a página ${page + 1}`}
              aria-current={page === currentPage ? "page" : undefined}
              className={cn("h-8 min-w-[2rem] px-2", page === currentPage && "pointer-events-none")}
            >
              {page + 1}
            </Button>
          )
        })}
      </div>

      <Button
        variant="outline"
        size="icon"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        aria-label="Página siguiente"
        className="h-8 w-8"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </nav>
  )
}

export default Pagination

