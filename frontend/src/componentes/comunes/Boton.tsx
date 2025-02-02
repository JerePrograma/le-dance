import React from "react";
import clsx from "clsx";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  secondary?: boolean;
  children: React.ReactNode;
}

const Boton: React.FC<ButtonProps> = React.memo(
  ({ secondary, children, className, ...props }) => {
    const baseClass = secondary
      ? "bg-secondary text-secondary-foreground hover:bg-secondary/80"
      : "bg-primary text-primary-foreground hover:bg-primary/90";

    return (
      <button
        className={clsx(
          "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none ring-offset-background",
          "h-10 py-2 px-4",
          baseClass,
          className
        )}
        {...props}
      >
        {children}
      </button>
    );
  }
);

export default Boton;
