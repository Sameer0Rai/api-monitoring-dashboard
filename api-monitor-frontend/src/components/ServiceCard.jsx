import StatusBadge from "./StatusBadge";

export default function ServiceCard({ service, metrics }) {
  return (
    <div className="card">
      <h3>{service.name}</h3>
      <p>{service.url}</p>

      {metrics && (
        <>
          <p>Uptime: {metrics.uptimePercentage.toFixed(2)}%</p>
          <StatusBadge status={metrics.status} />
        </>
      )}
    </div>
  );
}
