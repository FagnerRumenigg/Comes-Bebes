/**
 * Traços SVG extraídos de docs/telas/*.html (viewBox 0 0 24 24, outline,
 * fill="none", stroke="currentColor"). Manter o desenho idêntico ao
 * mockup de origem — não redesenhar aqui.
 */
export const ICON_PATHS = {
  home: '<path d="m3 10 9-7 9 7v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 22v-8h6v8"/>',
  bookmark: '<path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.5L5 21V4a1 1 0 0 1 1-1z"/>',
  'bookmark-remove':
    '<path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.5L5 21V4a1 1 0 0 1 1-1z"/><path d="m8 8 8 8M16 8l-8 8"/>',
  bell: '<path d="M18 9a6 6 0 0 0-12 0c0 6-3 7-3 7h18s-3-1-3-7"/><path d="M13.7 20a2 2 0 0 1-3.4 0"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  filter: '<path d="M4 6h16M7 12h10M10 18h4"/>',
  check: '<path d="m4.5 12.5 5 5 10-11"/>',
  close: '<path d="M6 6l12 12M18 6L6 18"/>',
  back: '<path d="M19 12H5m0 0 6-6m-6 6 6 6"/>',
  'chevron-right': '<path d="m9 6 6 6-6 6"/>',
  'chevron-down': '<path d="m6 9 6 6 6-6"/>',
  more: '<circle cx="12" cy="5" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="1.4" fill="currentColor" stroke="none"/>',
  person: '<circle cx="12" cy="8" r="3.6"/><path d="M4.5 20a7.5 7.5 0 0 1 15 0"/>',
  settings:
    '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-2.9 1.2v.2a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-3-1.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0-1.2-2.9H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.3-3l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 2.9-1.2V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 3 1.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0 1.2 2.9h.2a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.6 1z"/>',
  help: '<circle cx="12" cy="12" r="9"/><path d="M9.2 9a3 3 0 0 1 5.6 1c0 2-3 2.5-3 4"/><path d="M12 17.5v.01"/>',
  logout: '<path d="M15 17l5-5-5-5M20 12H9M12 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h6"/>',
  sun: '<circle cx="12" cy="12" r="4.2"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4"/>',
  moon: '<path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5z"/>',
  alert: '<circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16.5v.01"/>',
  envelope: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>',
  lock: '<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  eye: '<path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z"/><circle cx="12" cy="12" r="2.8"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3.5 9h17M3.5 15h17M12 3a15 15 0 0 1 0 18 15 15 0 0 1 0-18"/>',
  people:
    '<circle cx="9" cy="8" r="3"/><path d="M3 20a6 6 0 0 1 12 0"/><path d="M16 5.5a3 3 0 0 1 0 5"/><path d="M18 20a6 6 0 0 0-2-4.5"/>',
  share:
    '<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4"/>',
  react: '<circle cx="12" cy="12" r="9"/><path d="M8.5 14.5a4.5 4.5 0 0 0 7 0"/><path d="M9 9.5v.01M15 9.5v.01"/>',
  'my-version':
    '<path d="M4 12a8 8 0 0 1 13.7-5.7L20 8"/><path d="M20 4v4h-4"/><path d="M20 12a8 8 0 0 1-13.7 5.7L4 16"/><path d="M4 20v-4h4"/>',
  camera:
    '<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7.5v5.5l3 2"/>',
  collection: '<path d="M4 7h16v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z"/><path d="M7 4h10v3H7z"/>',
  invite: '<circle cx="9" cy="8" r="3"/><path d="M3 20a6 6 0 0 1 12 0"/><path d="M17 8h5M19.5 5.5v5"/>',
  mute: '<path d="M11 5 6 9H3v6h3l5 4z"/><path d="m17 9 4 6M21 9l-4 6"/>',
  trash: '<path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13"/>',
} as const

export type IconName = keyof typeof ICON_PATHS
