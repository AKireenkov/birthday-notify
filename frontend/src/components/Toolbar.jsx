import React from 'react';
import { InputDesktop as Input } from '@alfalab/core-components-input/desktop';
import { TagDesktop as Tag } from '@alfalab/core-components-tag/desktop';

const SearchIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
    stroke="var(--color-light-text-tertiary)" strokeWidth="2"
    strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8" />
    <line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

export default function Toolbar({
  search,
  onSearchChange,
  showBirthdaysOnly,
  onToggleBirthdays,
}) {
  return (
    <div className="toolbar">
      <div className="toolbar-actions">
        <Tag
          size={40}
          view={showBirthdaysOnly ? 'filled' : 'outlined'}
          shape="rounded"
          checked={showBirthdaysOnly}
          onClick={onToggleBirthdays}
        >
          Именинники
        </Tag>
      </div>

      <div className="toolbar-search">
        <Input
          placeholder="Поиск по ФИО..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          size={40}
          style={{ width: 280 }}
          leftAddons={<SearchIcon />}
        />
      </div>
    </div>
  );
}
