import { useState } from "react";

export default function ServiceForm({ onAdd }) {
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);

    onAdd(name, url)
      .then(() => {
        setName("");
        setUrl("");
      })
      .catch((err) => setFormError(err.message))
      .finally(() => setSubmitting(false));
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        placeholder="API Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
      />
      <input
        placeholder="API URL"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        required
      />
      <button disabled={submitting}>{submitting ? "Adding..." : "Add API"}</button>
      {formError && <p className="form-error">{formError}</p>}
    </form>
  );
}
