import React from 'react';
import './ConnectionStatus.css';

function ConnectionStatus({ connected, clientId }) {
  return (
    <div className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
      <span className="status-dot"></span>
      <span className="status-text">
        {connected ? `Connected` : 'Disconnected'}
      </span>
      {connected && clientId && (
        <span className="client-id">ID: {clientId.substring(0, 8)}</span>
      )}
    </div>
  );
}

export default ConnectionStatus;
