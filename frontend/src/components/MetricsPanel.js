import React, { useEffect, useState } from 'react';

function MetricsPanel() {
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    fetch('/api/metrics')
      .then(res => res.json())
      .then(setMetrics)
      .catch(() => setError('Failed to load metrics'));
  }, []);

  if (error) return <div className="metrics-panel error">{error}</div>;
  if (!metrics) return <div className="metrics-panel loading">Loading metrics...</div>;

  return (
    <div className="metrics-panel">
      <h2>Performance Metrics</h2>
      <ul>
        {Object.entries(metrics).map(([key, value]) => (
          <li key={key}><strong>{key}:</strong> {String(value)}</li>
        ))}
      </ul>
    </div>
  );
}

export default MetricsPanel;
