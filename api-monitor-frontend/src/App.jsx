import { BrowserRouter, Route, Routes } from "react-router-dom";
import Dashboard from "./pages/Dashboard";

// A single route today, but wiring up the router now means adding a future page
// (e.g. a per-service detail view) is a new <Route>, not a restructuring of App.jsx.
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
      </Routes>
    </BrowserRouter>
  );
}
