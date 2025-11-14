import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  // StrictMode disabled in development to prevent duplicate WebSocket connections
  // Re-enable for production builds
  // <React.StrictMode>
    <App />
  // </React.StrictMode>
);
