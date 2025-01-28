// Button.tsx
import React from "react";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  secondary?: boolean;
  children: React.ReactNode;
}

const Boton: React.FC<ButtonProps> = ({ secondary, children, ...props }) => {
  // Escogemos la clase base y la secundaria
  const baseClass = secondary ? "form-botonSecundario" : "form-boton";

  return (
    <button className={baseClass} {...props}>
      {children}
    </button>
  );
};

export default Boton;
