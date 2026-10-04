# Oistars Billings - Assets

This directory contains visual assets for the Oistars Billings app including screenshots and branding materials.

## Screenshot Assets (Google Play Store)

### Screen 1: Dashboard
- **File**: `dashboard-screen.svg`
- **Dimensions**: 1440x2560px
- **Format**: SVG (scalable vector)
- **Caption**: "Real-time financial metrics and quick actions"
- **Content**: Shows total revenue, outstanding balance, overdue invoices, and quick action buttons

### Screen 2: Invoices
- **File**: `invoices-screen.svg`
- **Dimensions**: 1440x2560px
- **Format**: SVG (scalable vector)
- **Caption**: "Filter, search, and manage invoices by status"
- **Content**: Invoice list with status filtering, search, PDF export capabilities

### Screen 3: Clients
- **File**: `clients-screen.svg`
- **Dimensions**: 1440x2560px
- **Format**: SVG (scalable vector)
- **Caption**: "Organize and track your complete client database"
- **Content**: Client directory with payment history and account status

### Screen 4: Create Invoice
- **File**: `create-invoice-screen.svg`
- **Dimensions**: 1440x2560px
- **Format**: SVG (scalable vector)
- **Caption**: "Intuitive invoice creation with automatic calculations"
- **Content**: Invoice builder with itemized line items and auto-calculated totals

### Screen 5: Settings
- **File**: `settings-screen.svg`
- **Dimensions**: 1440x2560px
- **Format**: SVG (scalable vector)
- **Caption**: "Customize theme and manage your account"
- **Content**: Theme toggle, user profile, privacy settings, account management

## Branding Assets

### Feature Graphic
- **File**: `feature-graphic.svg`
- **Dimensions**: 1024x500px
- **Format**: SVG (scalable vector)
- **Usage**: Google Play Store feature graphic (header image)
- **Content**: Oistars Billings branding with tagline

### App Icon
- **File**: `app-icon.svg`
- **Dimensions**: 512x512px
- **Format**: SVG (scalable vector)
- **Usage**: App launcher icon (multiple scales)
- **Content**: Oistars Billings logo with money emoji accent

## Color Palette

| Color | Hex Code | Usage |
|-------|----------|-------|
| Navy Dark | #0B192C | Primary background |
| Navy Medium | #1E3E62 | Secondary background |
| Navy Light | #134074 | Tertiary |
| Ocean Blue | #0066CC | Accent/CTA |
| Accent Gold | #E0A96D | Highlights |
| Pearl White | #F8F9FA | Text/Light mode |

## Converting SVG to PNG

### Using ImageMagick
```bash
convert -density 300 dashboard-screen.svg -background white dashboard-screen.png
```

### Using Inkscape
```bash
inkscape -w 1440 -h 2560 dashboard-screen.svg -o dashboard-screen.png
```

### Using Online Tools
- https://cloudconvert.com/svg-to-png
- https://convertio.co/svg-png/

## Asset Usage Guidelines

### For Google Play Store
1. Convert SVG assets to PNG format
2. Upload 5 screenshots (max 8)
3. Add captions to each screenshot
4. Use feature graphic as header
5. Ensure text is legible at mobile sizes

### For GitHub
1. Keep SVG format for scalability
2. Reference in README with relative paths
3. SVG renders natively in GitHub markdown
4. No additional tools needed for viewing

### For Print/Marketing
1. Export SVG to high-resolution PNG (300 DPI)
2. Use app icon in various sizes:
   - 192x192px (Play Store listing)
   - 512x512px (High-res)
   - 1024x1024px (Ultra HD)

## Customization

To modify these SVG assets:
1. Open in any SVG editor (Inkscape, Adobe XD, Figma)
2. Edit text, colors, or layout
3. Export as PNG for raster use
4. Keep SVG original for future edits

## File Organization

```
assets/
├── dashboard-screen.svg       # Dashboard screenshot
├── invoices-screen.svg        # Invoices screenshot
├── clients-screen.svg         # Clients screenshot
├── create-invoice-screen.svg  # Create invoice screenshot
├── settings-screen.svg        # Settings screenshot
├── feature-graphic.svg        # Play Store feature image
├── app-icon.svg              # App launcher icon
└── ASSETS.md                 # This file
```

---

**Note**: All assets are in SVG format for scalability and easy customization. Convert to PNG as needed for specific platforms.
