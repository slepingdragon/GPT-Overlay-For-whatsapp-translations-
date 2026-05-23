# Branding

Bold **X** in the center, with a **gradient spiral** orbiting outward — purple → magenta → pink, on pure black. The spiral suggests motion / orbit / international reach; the X is the universal "translate / convert / cross" mark. A second faint spiral underneath adds depth, and a soft purple→pink halo glow bleeds out from behind.

## Files

- [`app_icon.svg`](app_icon.svg) — full-color master, 1024×1024 viewBox, rounded square.
- [`app_icon_mono.svg`](app_icon_mono.svg) — white-on-black silhouette for notifications / monochrome contexts.

Open either in a browser to preview. Inkscape / Figma / Sketch also work.

## Exporting to PNG

- **Online**: [`cloudconvert.com/svg-to-png`](https://cloudconvert.com/svg-to-png) — drag the SVG, pick 1024×1024 (or 512×512 for Play Store listing).
- **Inkscape**: `inkscape app_icon.svg --export-type=png --export-width=1024 --export-filename=app_icon_1024.png`
- **ImageMagick**: `magick -density 600 -background none app_icon.svg -resize 1024x1024 app_icon_1024.png`

## Android — install as launcher icon

1. Android Studio → right-click `app/src/main/res` → **New → Image Asset**
2. **Icon Type**: Launcher Icons (Adaptive and Legacy)
3. **Foreground Layer → Source Asset**: pick your exported `app_icon_1024.png` (or point at the SVG directly)
4. **Background Layer → Color**: `#000000` (pure black matches the SVG bg). The adaptive-icon pipeline will use this for the launcher background.
5. **Next → Finish** — replaces `ic_launcher` across all densities.

## iOS — install as app icon

1. Open the Xcode project → `Assets.xcassets` → `AppIcon`
2. Drag the exported `app_icon_1024.png` into the **App Store 1024×1024** slot
3. Xcode 14+ auto-derives the smaller sizes from it
4. Build → new icon appears on the home screen and in TestFlight

## Tweaking

Edit `app_icon.svg` directly; the gradient stops are in the `<defs>` block:

```xml
<linearGradient id="grad" ...>
  <stop offset="0%"  stop-color="#7C4DFF"/>   <!-- start: purple -->
  <stop offset="45%" stop-color="#D946EF"/>   <!-- mid:  magenta -->
  <stop offset="100%" stop-color="#FF4081"/>  <!-- end:  pink   -->
</linearGradient>
```

Swap the hex codes to retint the whole logo. Don't need to touch any of the geometry.
