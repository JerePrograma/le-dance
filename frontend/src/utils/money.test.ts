import { describe, expect, it } from "vitest";
import { isPositiveMoney } from "./money";

describe("isPositiveMoney", () => {
  it("acepta moneda positiva sin convertirla a number", () => {
    expect(["0.01", "1", "1.20", "9007199254740993.99"].every(isPositiveMoney)).toBe(true);
    expect(["", "0", "0.00", "-1", "1.234", "01.00"].some(isPositiveMoney)).toBe(false);
  });
});
