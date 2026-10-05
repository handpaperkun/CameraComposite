# 素材与第三方组件说明

本仓库公开展示源代码，不代表仓库内所有素材获得统一的开源或商业使用授权。项目代码沿用 CameraComposite 仓库已有的 GNU GPL v3 许可证，全文见根目录 `LICENSE`。第三方素材不因此重新授权。

## 相机与扫描仪外观

`app/src/main/assets/cameras/catalog.json` 记录机型、原始来源网址、文件哈希和校准信息。产品图片来自 Pxlmag、相机厂商网站及产品手册；相关商标和图片权利归各权利人。公开下载不等于已获得再次分发或商业授权，本项目尚未核实全部素材的分发授权。

不得将本仓库的公开状态理解为对这些素材的许可。商业发布前需要逐项核实授权，或换成有权使用的自有素材。来源与覆盖情况另见 `docs/coverage.html` 和 `docs/restore-downloads.json`。

## 字体

站酷小薇、马善政和缝合像素字体的来源记录及对应 OFL 许可保存在 `app/src/main/assets/fonts/`。保留各字体自身的许可及署名，具体条款以目录内许可文本为准。

## 界面纹理

纸张和皮革纹理来源于 Subtle Patterns，使用 CC BY-SA 3.0；来源、作者和许可文本保存在 `app/src/main/assets/textures/`，适用范围是对应纹理素材。

## 依赖和研究数据

AndroidX ExifInterface 由构建流程从 Google Maven 获取，适用其 Apache 2.0 许可。Pillow 仅用于本地素材优化，遵循其自身许可，不作为 Android 运行时依赖。

`research/` 内的公开型号、EXIF 资料和研究快照保留原有来源；其中第三方项目的文件仍遵循各自许可，不因收录在本仓库而改变。
