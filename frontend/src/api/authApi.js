import axiosInstance from './axiosInstance';

const AUTH_ENDPOINT = '/api/auth';

export const login = async (credentials) => {
  const response = await axiosInstance.post(`${AUTH_ENDPOINT}/login`, credentials);
  return response.data;
};

export const register = async (userData) => {
  const response = await axiosInstance.post(`${AUTH_ENDPOINT}/register`, userData);
  return response.data;
};
