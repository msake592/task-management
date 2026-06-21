import axiosInstance from './axiosInstance';

const PROJECTS_ENDPOINT = '/api/projects';

export const getProjects = async () => {
  const response = await axiosInstance.get(PROJECTS_ENDPOINT);
  return response.data;
};

export const createProject = async (projectData) => {
  const response = await axiosInstance.post(PROJECTS_ENDPOINT, projectData);
  return response.data;
};
