// src/utils/debounce.ts
export function debounce<T extends (...args: any[]) => void>(
  fn: T,
  delay: number
): T {
  let timeoutId: ReturnType<typeof setTimeout> | undefined;
  return function (...args: any[]) {
    if (timeoutId) clearTimeout(timeoutId);
    timeoutId = setTimeout(() => {
      fn(...args);
    }, delay);
  } as T;
}
