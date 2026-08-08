import ErrorBanner from "../components/ErrorBanner";
import LatencyChart from "../components/LatencyChart";
import LoadingSpinner from "../components/LoadingSpinner";
import ServiceCard from "../components/ServiceCard";
import ServiceForm from "../components/ServiceForm";
import { useServices } from "../hooks/useServices";

export default function Dashboard() {
  const { services, logsByService, metricsByService, loading, error, addService } =
    useServices();

  return (
    <div className="container">
      <h1 style={{ fontSize: "36px" }}>API Monitoring Dashboard</h1>

      <ServiceForm onAdd={addService} />
      <ErrorBanner message={error} />

      {loading ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="cards">
            {services.map((service) => (
              <ServiceCard
                key={service.id}
                service={service}
                metrics={metricsByService[service.id]}
              />
            ))}
          </div>

          {services.map((service) => (
            <LatencyChart
              key={service.id}
              serviceName={service.name}
              data={logsByService[service.id]}
            />
          ))}
        </>
      )}
    </div>
  );
}
