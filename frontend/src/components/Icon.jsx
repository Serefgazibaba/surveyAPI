const paths = {
  home: <><path d="m3 10 9-7 9 7v11h-6v-6H9v6H3z" /></>,
  list: <><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 9h6M9 13h6M9 17h4"/></>,
  plus: <><path d="M12 5v14M5 12h14"/></>,
  chart: <><path d="M3 3v18h18M7 15l4-4 3 3 5-6"/></>,
  users: <><circle cx="9" cy="8" r="4"/><path d="M2 21c0-4 3-6 7-6s7 2 7 6M16 4a4 4 0 0 1 0 8M18 16c2.5.6 4 2.6 4 5"/></>,
  template: <><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></>,
  search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
  calendar: <><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M8 2v4M16 2v4M3 10h18"/></>,
  send: <><path d="m22 2-7 20-4-9-9-4zM22 2 11 13"/></>,
  edit: <><path d="M12 20h9M16.5 3.5a2 2 0 0 1 3 3L7 19l-4 1 1-4z"/></>,
  eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></>,
  lock: <><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></>,
  check: <><path d="m20 6-11 11-5-5"/></>,
  left: <><path d="m15 18-6-6 6-6"/></>,
  right: <><path d="m9 18 6-6-6-6"/></>,
  up: <><path d="m18 15-6-6-6 6"/></>,
  down: <><path d="m6 9 6 6 6-6"/></>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
  trash: <><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/></>,
  copy: <><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1"/></>,
  x: <><path d="M18 6 6 18M6 6l12 12"/></>,
  alert: <><path d="M12 3 2 21h20zM12 10v5M12 18v.5"/></>,
  info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.5"/></>,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></>,
  bell: <><path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0"/></>,
  sparkle: <><path d="M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8"/></>,
  download: <><path d="M12 3v12M7 10l5 5 5-5M5 21h14"/></>,
  refresh: <><path d="M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5"/></>,
  logout: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/></>,
  filter: <><path d="M3 5h18l-7 8v6l-4 2v-8z"/></>,
};

/** SVG ikon. `name` paths tablosundaki anahtarlardan biri. */
export default function Icon({ name = "dot" }) {
  return <svg className="i" viewBox="0 0 24 24" aria-hidden="true">{paths[name] || <circle cx="12" cy="12" r="8"/>}</svg>;
}
