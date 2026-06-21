import axiosInstance from './axiosInstance';

const USERS_ENDPOINT = '/api/users';

export const getUsers = async () => {
  const response = await axiosInstance.get(USERS_ENDPOINT);
  return response.data;
};
