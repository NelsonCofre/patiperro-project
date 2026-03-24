export const API_BASE_URL = "http://localhost:8080";

export const API_ENDPOINTS = {
  auth: {
    tutores: {
      register: `${API_BASE_URL}/api/auth/tutores/register`,
      login: `${API_BASE_URL}/api/auth/tutores/login`
    }
  }
};
