# 静态资源目录

此目录存放应用的静态资源文件。

## 目录结构

```
static/
├── tabbar/           # TabBar 图标 (TODO: 添加图标后启用)
│   ├── home.png              # 首页图标
│   ├── home-active.png       # 首页图标（选中）
│   ├── station.png           # 充电站图标
│   ├── station-active.png    # 充电站图标（选中）
│   ├── order.png             # 订单图标
│   ├── order-active.png      # 订单图标（选中）
│   ├── user.png              # 我的图标
│   └── user-active.png       # 我的图标（选中）
└── images/           # 其他图片资源
    ├── avatar-default.png    # 默认头像
    ├── station-default.png   # 默认站点图
    ├── marker.png            # 地图标记
    └── marker-active.png     # 地图标记（选中）
```

## TabBar 图标

### 当前状态: ⚠️ 占位符

TabBar 暂时不显示图标（仅显示文字）。

添加图标后，修改 `pages.json` 和 `manifest.json` 中的 tabBar 配置:

```json
{
  "pagePath": "pages/home/index",
  "text": "首页",
  "iconPath": "static/tabbar/home.png",
  "selectedIconPath": "static/tabbar/home-active.png"
}
```

### 图标规格
- **尺寸**: 81×81 px (推荐) 或 40×40 pt @2x
- **格式**: PNG (支持透明背景)
- **命名**: `{name}.png` / `{name}-active.png`
- **颜色**: 
  - 默认: #999999
  - 选中: #00B42A (主题绿色)

### 推荐来源
- [iconfont.cn](https://www.iconfont.cn/) - 阿里图标库
- [flaticon.com](https://www.flaticon.com/)
- [icons8.com](https://icons8.com/)

## 地图标记
- 尺寸: 32×40 px
- 格式: PNG

## 注意事项

1. 图片资源需手动添加
2. 建议使用 SVG 转 PNG 或使用设计工具导出
3. 可使用在线图标库（如 iconfont）生成
