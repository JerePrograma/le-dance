// src/componentes/comunes/Boton.tsx
import React from "react";
import clsx from "clsx";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  secondary?: boolean;
  children: React.ReactNode;
}

const Boton: React.FC<ButtonProps> = React.memo(
  ({ secondary, children, className, ...props }) => {
    const baseClass = secondary ? "form-botonSecundario" : "form-boton";
    return (
      <button className={clsx(baseClass, className)} {...props}>
        {children}
      </button>
    );
  }
);

export default Boton;
