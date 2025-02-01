// src/componentes/comunes/Tarjeta.tsx
import React from "react";

interface TarjetaProps {
  titulo: string;
  valor: string | number;
  descripcion: string;
  children?: React.ReactNode;
}

const Tarjeta: React.FC<TarjetaProps> = ({
  titulo,
  valor,
  descripcion,
  children,
}) => {
  return (
    <div className="tarjeta p-4 border rounded shadow hover:shadow-lg transition">
      <h3 className="text-xl font-bold">{titulo}</h3>
      <p className="text-2xl">{valor}</p>
      <p className="text-sm">{descripcion}</p>
      {children && <div className="mt-2">{children}</div>}
    </div>
  );
};

export default Tarjeta;
