const MONEY_INPUT = /^\d+(?:[.,]\d{1,2})?$/;

export const normalizeMoneyInput = (value: string): string | null => {
  const input = value.trim();
  if (!MONEY_INPUT.test(input) || (input.includes(",") && input.includes("."))) return null;
  const [integerPart, decimalPart = ""] = input.replace(",", ".").split(".");
  if (integerPart.length > 1 && integerPart.startsWith("0")) return null;
  return `${integerPart}.${decimalPart.padEnd(2, "0")}`;
};

export const isMoney = (value: string): boolean => normalizeMoneyInput(value) !== null;

export const isPositiveMoney = (value: string): boolean => {
  const normalized = normalizeMoneyInput(value);
  return normalized !== null && normalized !== "0.00";
};

export const compareMoney = (left: string, right: string): -1 | 0 | 1 => {
  const normalizedLeft = normalizeMoneyInput(left);
  const normalizedRight = normalizeMoneyInput(right);
  if (normalizedLeft === null || normalizedRight === null) throw new Error("Importe inválido");
  const [leftInteger, leftDecimal] = normalizedLeft.split(".");
  const [rightInteger, rightDecimal] = normalizedRight.split(".");
  if (leftInteger.length !== rightInteger.length) return leftInteger.length < rightInteger.length ? -1 : 1;
  const integerComparison = leftInteger.localeCompare(rightInteger);
  if (integerComparison !== 0) return integerComparison < 0 ? -1 : 1;
  const decimalComparison = leftDecimal.localeCompare(rightDecimal);
  return decimalComparison === 0 ? 0 : decimalComparison < 0 ? -1 : 1;
};

export const formatMoney = (value: string): string => {
  const normalized = normalizeMoneyInput(value);
  if (normalized === null) return value;
  const [integer, decimal] = normalized.split(".");
  return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ".")},${decimal}`;
};
