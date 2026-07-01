import { describe, expect, it } from "vitest";
import { compareMoney, formatMoney, isMoney, isPositiveMoney, normalizeMoneyInput } from "./money";

describe("money", () => {
  it("acepta moneda positiva sin convertirla a number", () => {
    expect(["0.01", "1", "1.20", "9007199254740993.99"].every(isPositiveMoney)).toBe(true);
    expect(["", "0", "0.00", "-1", "1.234", "01.00"].some(isPositiveMoney)).toBe(false);
  });

  it("normaliza cero, centavos, ceros finales, importes grandes y coma de entrada", () => {
    expect(normalizeMoneyInput("0")).toBe("0.00");
    expect(normalizeMoneyInput("0,01")).toBe("0.01");
    expect(normalizeMoneyInput("1.2")).toBe("1.20");
    expect(normalizeMoneyInput("9007199254740993.99")).toBe("9007199254740993.99");
    expect(isMoney("0.00")).toBe(true);
  });

  it("rechaza separadores inválidos, negativos, más de dos decimales y vacío", () => {
    expect(["", "-0.01", "1,2.3", "1.234", "1 000,00"].map(normalizeMoneyInput)).toEqual([
      null, null, null, null, null,
    ]);
  });

  it("compara y formatea sin convertir a number", () => {
    expect(compareMoney("9007199254740993.99", "9007199254740992.99")).toBe(1);
    expect(compareMoney("1,20", "1.2")).toBe(0);
    expect(formatMoney("1234567.8")).toBe("1.234.567,80");
  });
});
