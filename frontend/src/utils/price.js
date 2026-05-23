/**
 * Parse a money string like "¥49.00" into a number. Returns 0 for unparseable input.
 */
export function parsePrice(text) {
  if (text == null) return 0;
  if (typeof text === 'number') return text;
  const cleaned = String(text).replace(/[^0-9.]/g, '');
  if (!cleaned) return 0;
  const n = parseFloat(cleaned);
  return Number.isFinite(n) ? n : 0;
}

/**
 * Format a number as a "¥xx.xx" Chinese-yuan label.
 */
export function formatYuan(value) {
  const n = typeof value === 'number' ? value : parsePrice(value);
  return `¥${n.toFixed(2)}`;
}
