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

export const getProjectMembers = async (projectId) => {
  const response = await axiosInstance.get(`${PROJECTS_ENDPOINT}/${projectId}/members`);
  return response.data;
};

export const addProjectMember = async (projectId, memberData) => {
  const response = await axiosInstance.post(`${PROJECTS_ENDPOINT}/${projectId}/members`, memberData);
  return response.data;
};
