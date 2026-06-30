const POSITIVE_MONEY = /^(?:0\.(?:0[1-9]|[1-9]\d?)|[1-9]\d*(?:\.\d{1,2})?)$/;

export const isPositiveMoney = (value: string): boolean => POSITIVE_MONEY.test(value);
