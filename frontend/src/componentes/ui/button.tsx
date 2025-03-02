import React, { type ButtonHTMLAttributes } from "react"

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "default" | "outline"
  size?: "default" | "sm"
  className?: string
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = "", variant = "default", size = "default", children, ...props }, ref) => {
    const baseStyles =
      "inline-flex items-center justify-center rounded-md font-medium transition-colors focus:outline-none disabled:opacity-50"

    const variants = {
      default: "bg-gray-900 text-white hover:bg-gray-800 border border-transparent",
      outline: "border border-gray-200 bg-white hover:bg-gray-100 text-gray-900",
    }

    const sizes = {
      default: "h-10 px-4 py-2",
      sm: "h-8 px-3 text-sm",
    }

    return (
      <button ref={ref} className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className}`} {...props}>
        {children}
      </button>
    )
  },
)

Button.displayName = "Button"

