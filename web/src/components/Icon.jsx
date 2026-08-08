import React from 'react';

/**
 * Renders a Material Symbols Outlined icon, matching the Stitch design
 * system exactly (stitch_kunjachaya_resident_club_app uses this icon set
 * throughout, not a third-party icon library).
 */
export default function Icon({ name, filled = false, className = '', style = {} }) {
  return (
    <span
      className={`material-symbols-outlined ${filled ? 'filled' : ''} ${className}`}
      style={style}
    >
      {name}
    </span>
  );
}
