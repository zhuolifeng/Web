import { parsePrice, formatYuan } from '../utils/price';

/**
 * price.js 工具函数单元测试。
 */
describe('parsePrice', () => {
  test('解析带人民币符号的价格', () => {
    expect(parsePrice('¥49.00')).toBe(49);
  });

  test('解析纯数字字符串', () => {
    expect(parsePrice('68.50')).toBe(68.5);
  });

  test('数字类型直接返回', () => {
    expect(parsePrice(100)).toBe(100);
  });

  test('null 返回 0', () => {
    expect(parsePrice(null)).toBe(0);
  });

  test('undefined 返回 0', () => {
    expect(parsePrice(undefined)).toBe(0);
  });

  test('空字符串返回 0', () => {
    expect(parsePrice('')).toBe(0);
  });

  test('无数字字符串返回 0', () => {
    expect(parsePrice('abc')).toBe(0);
  });
});

describe('formatYuan', () => {
  test('格式化数字为人民币', () => {
    expect(formatYuan(49)).toBe('¥49.00');
  });

  test('格式化字符串价格', () => {
    expect(formatYuan('¥68.50')).toBe('¥68.50');
  });

  test('格式化 0', () => {
    expect(formatYuan(0)).toBe('¥0.00');
  });

  test('格式化 null', () => {
    expect(formatYuan(null)).toBe('¥0.00');
  });
});
