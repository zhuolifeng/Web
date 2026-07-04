/**
 * Jest 全局 setup 文件。
 * 为 Ant Design 组件提供 window.matchMedia 的 mock 实现。
 */

window.matchMedia = window.matchMedia || function(query) {
  return {
    matches: false,
    media: query,
    onchange: null,
    addListener: function() {},
    removeListener: function() {},
    addEventListener: function() {},
    removeEventListener: function() {},
    dispatchEvent: function() { return true; },
  };
};

// Ant Design 的 getComputedStyle mock
const originalGetComputedStyle = window.getComputedStyle;
window.getComputedStyle = (elt, pseudoElt) => {
  try {
    return originalGetComputedStyle(elt, pseudoElt);
  } catch (e) {
    return { getPropertyValue: () => '' };
  }
};
