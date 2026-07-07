import axiosInstance from './axiosInstance';

const getCommentsEndpoint = (taskId) => `/api/tasks/${taskId}/comments`;

export const getCommentsByTask = async (taskId) => {
  const response = await axiosInstance.get(getCommentsEndpoint(taskId));
  return response.data;
};

export const addComment = async (taskId, content) => {
  const response = await axiosInstance.post(getCommentsEndpoint(taskId), { content });
  return response.data;
};
