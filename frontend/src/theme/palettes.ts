export interface ERPColorPalette {
  id: string
  name: string
  colors: readonly [string, string, string, string]
}

// Source: the supplied 4-column, 8-row palette image. The image is small, so
// these are close visual readings of the four swatches, kept editable here.
// Order is row-major, left to right, top to bottom.
export const erpPalettes: readonly ERPColorPalette[] = [
  { id: 'palette-01', name: 'Ocean Harbor', colors: ['#1D3559', '#005B98', '#59BCE1', '#9BDDEC'] },
  { id: 'palette-02', name: 'Summer Citrus', colors: ['#E90000', '#FFAA82', '#FFEFD5', '#9DE976'] },
  { id: 'palette-03', name: 'Berry Sunset', colors: ['#41104E', '#8B2C64', '#FF5A54', '#FFD331'] },
  { id: 'palette-04', name: 'Royal Orchid', colors: ['#2C003F', '#560075', '#A1248E', '#F1EDCA'] },
  { id: 'palette-05', name: 'Lilac Coral', colors: ['#A29AF8', '#CCE6E3', '#FFF8EF', '#FF6C60'] },
  { id: 'palette-06', name: 'Peach Grove', colors: ['#FFE5C9', '#B6DCD9', '#EA9D59', '#DC5D3C'] },
  { id: 'palette-07', name: 'Pink Violet', colors: ['#F85783', '#F9DCEB', '#A460F8', '#594BFA'] },
  { id: 'palette-08', name: 'Sky Festival', colors: ['#B8DCFA', '#FFF5C7', '#FCAC05', '#EB0017'] },
  { id: 'palette-09', name: 'Apricot Coast', colors: ['#FF913D', '#FFCA53', '#FFEBCF', '#6FA3C5'] },
  { id: 'palette-10', name: 'Moss Meadow', colors: ['#17370E', '#79966D', '#D7DCA3', '#F1EEC9'] },
  { id: 'palette-11', name: 'Rosewood', colors: ['#800D4C', '#B36F82', '#D99FA7', '#EDC4BA'] },
  { id: 'palette-12', name: 'Cedar Earth', colors: ['#082F26', '#99520B', '#C27935', '#F2D4BC'] },
  { id: 'palette-13', name: 'Slate Garden', colors: ['#1D2430', '#34415B', '#A5C99E', '#E3EEB8'] },
  { id: 'palette-14', name: 'Warm Linen', colors: ['#ECE3CD', '#DDD7C2', '#CDC8B5', '#B9B7A8'] },
  { id: 'palette-15', name: 'Neon Carnival', colors: ['#298AF8', '#FFEE31', '#FFC449', '#F51F65'] },
  { id: 'palette-16', name: 'Electric Iris', colors: ['#250AA8', '#1241C4', '#456CE0', '#C7EB72'] },
  { id: 'palette-17', name: 'Golden Grove', colors: ['#FFD13F', '#5D9656', '#49794D', '#EDC047'] },
  { id: 'palette-18', name: 'Olive Fields', colors: ['#D6D58C', '#B7C087', '#8E9A60', '#F0E8B9'] },
  { id: 'palette-19', name: 'Blush Linen', colors: ['#F8CCD8', '#FFE0E3', '#FFF0D9', '#FFF7EB'] },
  { id: 'palette-20', name: 'Mauve Haze', colors: ['#49245E', '#7E5B96', '#9F80B4', '#B99CCD'] },
  { id: 'palette-21', name: 'Plum Peach', colors: ['#4D2B5C', '#784164', '#E3998D', '#FFCE9F'] },
  { id: 'palette-22', name: 'Blue Lagoon', colors: ['#09668D', '#5091BA', '#A9D9F1', '#FBE9C9'] },
  { id: 'palette-23', name: 'Harvest Green', colors: ['#DC8427', '#F4DE9A', '#4B983D', '#226847'] },
  { id: 'palette-24', name: 'Autumn Market', colors: ['#4E7E3D', '#FFD248', '#F4B36C', '#F04A33'] },
  { id: 'palette-25', name: 'Soft Orchard', colors: ['#F3E5EB', '#FFFDF6', '#BDD4B9', '#6FA660'] },
  { id: 'palette-26', name: 'Coral Mint', colors: ['#D96C74', '#FFC98C', '#FFF0AA', '#AFE8CB'] },
  { id: 'palette-27', name: 'Spiced Amber', colors: ['#FFF4E7', '#FFB35E', '#DE7024', '#96491C'] },
  { id: 'palette-28', name: 'Sea Glass', colors: ['#287F7A', '#55ADA3', '#B6E7CE', '#E8F2C3'] },
  { id: 'palette-29', name: 'Mint Horizon', colors: ['#86D3CE', '#2EA3A5', '#C7E8E1', '#FF8D59'] },
  { id: 'palette-30', name: 'Sage Stone', colors: ['#5E745A', '#8FA98B', '#B8CEB5', '#DEE9D9'] },
  { id: 'palette-31', name: 'Honey Stone', colors: ['#F9EED8', '#B1B28C', '#EABF68', '#4C4946'] },
  { id: 'palette-32', name: 'Terracotta Glow', colors: ['#A34045', '#DF6A59', '#F39261', '#F6BF97'] },
]

export const defaultPalette: ERPColorPalette = {
  id: 'default',
  name: 'Original navy and blue',
  colors: ['#0B1730', '#315FF4', '#316BFF', '#E6EFFF'],
}

export function findPalette(id: string): ERPColorPalette | undefined {
  return id === defaultPalette.id ? defaultPalette : erpPalettes.find(palette => palette.id === id)
}
