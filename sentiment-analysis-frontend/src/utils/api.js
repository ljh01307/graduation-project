import { getToken } from './auth.js';

const API_BASE = '/api';

async function request(url, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...options.headers
  };

  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers
  });

  const contentType = response.headers.get('content-type');
  let data;
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    const text = await response.text();
    if (!response.ok) {
      throw new Error(text || '请求失败');
    }
    return text;
  }

  if (!response.ok) {
    throw new Error(data.message || '请求失败');
  }

  if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
    if (data.code !== 200) {
      throw new Error(data.message || '请求失败');
    }
    return data.data;
  }

  return data;
}

export function getUserInfoFromStorage() {
  const userStr = localStorage.getItem('auth_user');
  return userStr ? JSON.parse(userStr) : null;
}

export async function fetchUsers() {
  return request('/users');
}

export async function fetchProducts(manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/product/list-with-stats${query ? '?' + query : ''}`);
}

export async function addProduct(category, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/product/add${query ? '?' + query : ''}`, {
    method: 'POST',
    body: JSON.stringify({ category })
  });
}

export async function updateProduct(id, category, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/product/update/${id}${query ? '?' + query : ''}`, {
    method: 'PUT',
    body: JSON.stringify({ category })
  });
}

export async function deleteProduct(id, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/product/delete/${id}${query ? '?' + query : ''}`, {
    method: 'DELETE'
  });
}

export async function fetchComments(productId, params = {}, manageUserId = null) {
  const queryParams = new URLSearchParams();
  if (manageUserId) queryParams.append('manageUserId', manageUserId);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      queryParams.append(key, value);
    }
  });
  const query = queryParams.toString();
  return request(`/review/list/${productId}${query ? '?' + query : ''}`);
}

export async function fetchAllComments(params = {}, manageUserId = null) {
  const queryParams = new URLSearchParams();
  if (manageUserId) queryParams.append('manageUserId', manageUserId);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      queryParams.append(key, value);
    }
  });
  const query = queryParams.toString();
  return request(`/review/list-all${query ? '?' + query : ''}`);
}

export async function uploadComments(productId, contents, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/review/upload${query ? '?' + query : ''}`, {
    method: 'POST',
    body: JSON.stringify({ productId, contents })
  });
}

export async function analyzeReviews(productId, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/review/analyze/${productId}${query ? '?' + query : ''}`, {
    method: 'POST'
  });
}

export async function fetchStats(productId, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/review/stats/${productId}${query ? '?' + query : ''}`);
}

export async function fetchWeeklyStats(productId, weeks = 4, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  params.append('weeks', weeks);
  const query = params.toString();
  return request(`/review/weekly/${productId}${query ? '?' + query : ''}`);
}

export async function fetchWordCloud(productId, topN = 50, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  params.append('topN', topN);
  const query = params.toString();
  return request(`/review/wordcloud/${productId}${query ? '?' + query : ''}`);
}

export async function fetchOverview(productId, startTime, endTime, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  if (startTime) params.append('startTime', startTime);
  if (endTime) params.append('endTime', endTime);
  const query = params.toString();
  return request(`/review/overview/${productId}${query ? '?' + query : ''}`);
}

export async function deleteReview(reviewId, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/review/delete/${reviewId}${query ? '?' + query : ''}`, {
    method: 'DELETE'
  });
}

export async function fetchUnanalyzedCount(productId, manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/review/unanalyzed-count/${productId}${query ? '?' + query : ''}`);
}

export async function fetchUnanalyzedCountAll(manageUserId = null) {
  const params = new URLSearchParams();
  if (manageUserId) params.append('manageUserId', manageUserId);
  const query = params.toString();
  return request(`/review/unanalyzed-count${query ? '?' + query : ''}`);
}

export async function updateUsername(newUsername) {
  return request('/auth/username', {
    method: 'PUT',
    body: JSON.stringify({ username: newUsername })
  });
}

export async function updatePassword(oldPassword, newPassword) {
  return request('/auth/password', {
    method: 'PUT',
    body: JSON.stringify({ oldPassword, newPassword })
  });
}

export async function addUser(username, password, role = 'USER') {
  return request('/users', {
    method: 'POST',
    body: JSON.stringify({ username, password, role })
  });
}

export async function updateUser(id, username) {
  return request(`/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ username })
  });
}

export async function deleteUser(id) {
  return request(`/users/${id}`, {
    method: 'DELETE'
  });
}