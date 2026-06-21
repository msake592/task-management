import axiosInstance from './axiosInstance';

const PROJECTS_ENDPOINT = '/api/projects';

export const getProjects = async () => {
  const response = await axiosInstance.get(PROJECTS_ENDPOINT);
  return response.data;
};
