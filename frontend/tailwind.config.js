// tailwind.config.js
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx,css}"],
  theme: {
    extend: {
      colors: {
        primary: "var(--color-primary)",
        secondary: "var(--color-secondary)",
        accent: "var(--color-accent)",
      },
      borderRadius: {
        lg: "var(--radius-lg)",
      },
      spacing: {
        4: "var(--spacing-4)",
        6: "var(--spacing-6)",
      },
    },
  },
  plugins: [],
};
