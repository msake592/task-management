import axiosInstance from './axiosInstance';

const USERS_ENDPOINT = '/api/users';

export const getUsers = async () => {
  const response = await axiosInstance.get(`${USERS_ENDPOINT}/options`, {
    params: { size: 100 },
  });
  return response.data;
};

export const getAdminUsers = async () => {
  const response = await axiosInstance.get(USERS_ENDPOINT, {
    params: { size: 100, sort: 'id,asc' },
  });
  return response.data;
};

export const updateUser = async (id, data) => {
  const response = await axiosInstance.put(`${USERS_ENDPOINT}/${id}`, data);
  return response.data;
};
