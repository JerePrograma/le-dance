import type React from "react";

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
    <div className="bg-card text-card-foreground p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow">
      <h3 className="text-xl font-bold mb-2">{titulo}</h3>
      <p className="text-2xl font-semibold mb-1">{valor}</p>
      <p className="text-sm text-muted-foreground mb-4">{descripcion}</p>
      {children && <div className="mt-4">{children}</div>}
    </div>
  );
};

export default Tarjeta;
