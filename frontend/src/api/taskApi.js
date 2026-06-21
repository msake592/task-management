import axiosInstance from './axiosInstance';

const TASKS_ENDPOINT = '/api/tasks';

export const getTasks = async () => {
  const response = await axiosInstance.get(TASKS_ENDPOINT);
  return response.data;
};

export const getTaskById = async (id) => {
  const response = await axiosInstance.get(`${TASKS_ENDPOINT}/${id}`);
  return response.data;
};

export const createTask = async (taskData) => {
  const response = await axiosInstance.post(TASKS_ENDPOINT, taskData);
  return response.data;
};

export const updateTask = async (id, taskData) => {
  const response = await axiosInstance.put(`${TASKS_ENDPOINT}/${id}`, taskData);
  return response.data;
};

export const deleteTask = async (id) => {
  await axiosInstance.delete(`${TASKS_ENDPOINT}/${id}`);
};
