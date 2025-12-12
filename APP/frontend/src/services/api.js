// src/services/api.js
// Backend base URL is configurable via Vite env (VITE_API_URL). Falls back to localhost for dev.
export const API_BASE = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");
// WebSocket base uses dedicated env (VITE_WS_URL) or derives from API_BASE.
export const WS_BASE = (import.meta.env.VITE_WS_URL || API_BASE.replace(/^http/, "ws")).replace(/\/$/, "");
const BASE = API_BASE;

// ==========================
// GET TOKEN
// ==========================
function getToken() {
  return localStorage.getItem("token");
}

// ==========================
// POST
// ==========================
export async function apiPost(url, body) {
  const token = getToken();

  const res = await fetch(BASE + url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {})
    },
    body: JSON.stringify(body)
  });

  const text = await res.text();
  let data = {};

  try {
    data = JSON.parse(text);
  } catch {
    // ignore
  }

  if (!res.ok) {
    throw new Error(data.error || text || "POST error");
  }

  return data;
}

// ==========================
// GET
// ==========================
export async function apiGet(url) {
  const token = getToken();

  const res = await fetch(BASE + url, {
    method: "GET",
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {})
    }
  });

  const text = await res.text();
  let data = {};

  try {
    data = JSON.parse(text);
  } catch {
    // ignore
  }

  if (!res.ok) {
    throw new Error(data.error || text || "GET error");
  }

  return data;
}

// ==========================
// DELETE
// ==========================
export async function apiDelete(url) {
  const token = getToken();

  const res = await fetch(BASE + url, {
    method: "DELETE",
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {})
    }
  });

  if (res.status === 204) {
    return {};
  }

  const text = await res.text();
  let data = {};

  try {
    data = JSON.parse(text);
  } catch {
    // ignore
  }

  if (!res.ok) {
    throw new Error(data.error || text || "DELETE error");
  }

  return data;
}

// ==========================
// UPLOAD (multipart/form-data)
// ==========================
export async function apiUploadForm(url, formData) {
  const token = getToken();

  const res = await fetch(BASE + url, {
    method: "POST",
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {})
    },
    body: formData
  });

  const text = await res.text();
  let data = {};

  try {
    data = JSON.parse(text);
  } catch {
    // ignore
  }

  if (!res.ok) {
    throw new Error(data.error || text || "UPLOAD error");
  }

  return data;
}

// ==========================
// PUT
// ==========================
export async function apiPut(url, body) {
  const token = getToken();

  const res = await fetch(BASE + url, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: JSON.stringify(body),
  });

  const text = await res.text();
  let data = {};
  try {
    data = JSON.parse(text);
  } catch {
    // ignore
  }
  if (!res.ok) {
    throw new Error(data.error || text || "PUT error");
  }
  return data;
}

// ==========================
// DOWNLOAD (blob with auth)
// ==========================
export async function apiDownload(url, fallbackName) {
  const token = getToken();

  const res = await fetch(BASE + url, {
    method: "GET",
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {})
    }
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "DOWNLOAD error");
  }

  const blob = await res.blob();

  // Try to extract filename from headers
  let fileName = fallbackName || "download";
  const disposition = res.headers.get("content-disposition");
  if (disposition) {
    const match = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition);
    if (match?.[1]) {
      fileName = match[1].replace(/['"]/g, "");
    }
  }

  const blobUrl = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = blobUrl;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(blobUrl);
}

// ==========================
// Profile
// ==========================
export async function apiChangePassword(currentPassword, newPassword) {
  return apiPost("/api/auth/change-password", { currentPassword, newPassword });
}

export async function apiChangeEmail(email) {
  return apiPost("/api/auth/change-email", { email });
}

// ==========================
// Memberships
// ==========================
export const apiMembers = {
  list: (groupId) => apiGet(`/api/groups/${groupId}/members`),
  add: (groupId, userOrEmail, role = "MEMBER") => {
    const body = { role };
    if (typeof userOrEmail === "string" && userOrEmail.includes("@")) {
      body.email = userOrEmail.trim();
    } else if (userOrEmail !== undefined && userOrEmail !== null && userOrEmail !== "") {
      body.userId = Number(userOrEmail);
    }
    return apiPost(`/api/groups/${groupId}/members`, body);
  },
  changeRole: (groupId, membershipId, role) =>
    apiPost(`/api/groups/${groupId}/members/${membershipId}/role`, { role }),
  remove: (groupId, membershipId) =>
    apiDelete(`/api/groups/${groupId}/members/${membershipId}`),
};

// ==========================
// Task attachments
// ==========================
export const apiTaskResources = {
  list: (taskId) => apiGet(`/api/tasks/${taskId}/resources`),
  attach: (taskId, resourceId) =>
    apiPost(`/api/tasks/${taskId}/resources`, { resourceId }),
  detach: (taskId, resourceId) =>
    apiDelete(`/api/tasks/${taskId}/resources/${resourceId}`),
};
