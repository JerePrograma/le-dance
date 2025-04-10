import React, { useRef, useEffect } from "react";

interface NumberInputWithoutScrollProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  // Puedes extender las propiedades de un input normal si lo requieres
}

const NumberInputWithoutScroll: React.FC<NumberInputWithoutScrollProps> = (
  props
) => {
  const inputRef = useRef<HTMLInputElement>(null);

  // Handler para evitar que las flechas (ArrowUp/ArrowDown) modifiquen el valor
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "ArrowUp" || e.key === "ArrowDown") {
      e.preventDefault();
    }
  };

  useEffect(() => {
    const input = inputRef.current;
    if (input) {
      const handleWheel = (e: WheelEvent) => {
        e.preventDefault();
      };
      // Se agrega el listener con passive: false
      input.addEventListener("wheel", handleWheel, { passive: false });
      return () => {
        input.removeEventListener("wheel", handleWheel);
      };
    }
  }, []);

  return (
    <input ref={inputRef} type="number" onKeyDown={handleKeyDown} {...props} />
  );
};

export default NumberInputWithoutScroll;
