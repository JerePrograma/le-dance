/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: 0 },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: 0 },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
      spacing: {
        // Añadimos espaciado fluido al sistema de Tailwind
        "fluid-1": "var(--spacing-1)",
        "fluid-2": "var(--spacing-2)",
        "fluid-3": "var(--spacing-3)",
        "fluid-4": "var(--spacing-4)",
        "fluid-6": "var(--spacing-6)",
        "fluid-8": "var(--spacing-8)",
        "fluid-10": "var(--spacing-10)",
      },
      gap: {
        "fluid-xs": "var(--gap-xs)",
        "fluid-sm": "var(--gap-sm)",
        "fluid-md": "var(--gap-md)",
        "fluid-lg": "var(--gap-lg)",
      },
      fontSize: {
        "fluid-xs": "var(--text-xs)",
        "fluid-sm": "var(--text-sm)",
        "fluid-base": "var(--text-base)",
        "fluid-lg": "var(--text-lg)",
        "fluid-xl": "var(--text-xl)",
        "fluid-2xl": "var(--text-2xl)",
        "fluid-3xl": "var(--text-3xl)",
        "fluid-4xl": "var(--text-4xl)",
        "fluid-5xl": "var(--text-5xl)",
      },
    },
  },
  plugins: [
    require("tailwindcss-animate"),
    // Añadimos un plugin para las clases de truncado de líneas
    function ({ addUtilities }) {
      const newUtilities = {
        ".truncate-lines": {
          overflow: "hidden",
          "text-overflow": "ellipsis",
          display: "-webkit-box",
          "-webkit-box-orient": "vertical",
        },
        ".line-clamp-1": {
          "-webkit-line-clamp": "1",
        },
        ".line-clamp-2": {
          "-webkit-line-clamp": "2",
        },
        ".line-clamp-3": {
          "-webkit-line-clamp": "3",
        },
        ".line-clamp-4": {
          "-webkit-line-clamp": "4",
        },
      };
      addUtilities(newUtilities);
    },
  ],
};
