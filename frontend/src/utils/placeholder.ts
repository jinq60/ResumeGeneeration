// 模板缩略图加载失败时的兜底占位图（内联 SVG，无网络依赖）
export const TEMPLATE_PLACEHOLDER =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="400" viewBox="0 0 300 400">' +
      '<rect width="300" height="400" fill="#f2f4f7"/>' +
      '<rect x="60" y="60" width="180" height="24" rx="4" fill="#c9d2e3"/>' +
      '<rect x="60" y="100" width="120" height="12" rx="3" fill="#dfe5f0"/>' +
      '<rect x="60" y="140" width="180" height="8" rx="2" fill="#e8edf5"/>' +
      '<rect x="60" y="156" width="180" height="8" rx="2" fill="#e8edf5"/>' +
      '<rect x="60" y="172" width="140" height="8" rx="2" fill="#e8edf5"/>' +
      '<text x="150" y="250" text-anchor="middle" font-size="14" fill="#8a94a6">暂无缩略图</text>' +
      '</svg>'
  )
