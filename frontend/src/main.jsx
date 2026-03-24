import React from 'react';
import ReactDOM from 'react-dom/client';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';

// Alfa Design System — design tokens (click theme = Alfa Bank)
import '@alfalab/core-components/vars/bundle/click.css';

// Component CSS imports
import '@alfalab/core-components-button/esm/desktop/desktop.css';
import '@alfalab/core-components-button/esm/desktop/default.desktop.css';
import '@alfalab/core-components-input/esm/desktop/index.css';
import '@alfalab/core-components-input/esm/components/base-input/index.css';
import '@alfalab/core-components-input/esm/components/base-input/default.css';
import '@alfalab/core-components-table/esm/components/table/index.css';
import '@alfalab/core-components-table/esm/components/thead/index.css';
import '@alfalab/core-components-table/esm/components/thead-cell/index.css';
import '@alfalab/core-components-table/esm/components/tsortable-head-cell/index.css';
import '@alfalab/core-components-table/esm/components/tbody/index.css';
import '@alfalab/core-components-table/esm/components/trow/index.css';
import '@alfalab/core-components-table/esm/components/tcell/index.css';
import '@alfalab/core-components-table/esm/components/pagination/index.css';
import '@alfalab/core-components-modal/esm/desktop/desktop.css';
import '@alfalab/core-components-modal/esm/components/header/desktop.css';
import '@alfalab/core-components-modal/esm/components/content/desktop.css';
import '@alfalab/core-components-modal/esm/components/content/index.css';
import '@alfalab/core-components-modal/esm/components/footer/desktop.css';
import '@alfalab/core-components-modal/esm/components/footer/index.css';
import '@alfalab/core-components-modal/esm/components/footer/layout.css';
import '@alfalab/core-components-tag/esm/desktop/desktop.css';
import '@alfalab/core-components-tooltip/esm/desktop/desktop.css';
import '@alfalab/core-components-icon-button/esm/desktop/desktop.css';
import '@alfalab/core-components-badge/esm/index.css';
import '@alfalab/core-components-notification/esm/index.css';
import '@alfalab/core-components-dropzone/esm/index.css';
import '@alfalab/core-components-gap/esm/index.css';
import '@alfalab/core-components-divider/esm/index.css';
import '@alfalab/core-components-typography/esm/title/index.css';
import '@alfalab/core-components-typography/esm/text/index.css';
import '@alfalab/core-components-status/esm/index.css';

import App from './App';
import './styles/app.css';

dayjs.locale('ru');

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
