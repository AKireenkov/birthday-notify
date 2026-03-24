/**
 * API layer for Alfa Birthday.
 * Connected to real Spring Boot backend via Vite proxy (/api → localhost:8080).
 */

const BASE = '/api';

async function handleResponse(res) {
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || data.message || `HTTP ${res.status}`);
  }
  return data;
}

/** GET /api/employees */
export async function getEmployees() {
  const res = await fetch(`${BASE}/employees`);
  return handleResponse(res);
}

/** POST /api/employees */
export async function createEmployee(data) {
  const res = await fetch(`${BASE}/employees`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

/** PUT /api/employees/:id */
export async function updateEmployee(id, data) {
  const res = await fetch(`${BASE}/employees/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

/** DELETE /api/employees/:id */
export async function deleteEmployee(id) {
  const res = await fetch(`${BASE}/employees/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const data = await res.json();
    throw new Error(data.error || `HTTP ${res.status}`);
  }
  return { success: true };
}

/** POST /api/employees/upload (CSV file) */
export async function uploadCsv(file) {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${BASE}/employees/upload`, { method: 'POST', body: form });
  return handleResponse(res);
}

/** POST /api/notify/send-test */
export async function sendTestEmail() {
  const res = await fetch(`${BASE}/notify/send-test`, { method: 'POST' });
  return handleResponse(res);
}

/** POST /api/sync/run */
export async function syncFromFolder() {
  const res = await fetch(`${BASE}/sync/run`, { method: 'POST' });
  return handleResponse(res);
}
