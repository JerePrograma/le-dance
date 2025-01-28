interface TarjetaProps {
  titulo: string;
  valor: string | number;
  descripcion: string;
}

const Tarjeta = ({ titulo, valor, descripcion }: TarjetaProps) => {
  return (
    <div className="tarjeta">
      <h3>{titulo}</h3>
      <p className="valor">{valor}</p>
      <p className="descripcion">{descripcion}</p>
    </div>
  );
};

export default Tarjeta;
