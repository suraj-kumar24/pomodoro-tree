import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/caprasimo/latin-400.css';
import '@fontsource/figtree/latin-400.css';
import '@fontsource/figtree/latin-600.css';
import '@fontsource/figtree/latin-700.css';
import './styles/organic.css';
import './styles/coil.css';
import { StoreProvider } from './store';
import { App } from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <StoreProvider>
      <App />
    </StoreProvider>
  </StrictMode>,
);
