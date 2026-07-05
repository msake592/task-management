import axiosInstance from './axiosInstance';

const USERS_ENDPOINT = '/api/users';

export const getUsers = async () => {
  const response = await axiosInstance.get(`${USERS_ENDPOINT}/options`, {
    params: { size: 100 },
  });
  return response.data;
};
