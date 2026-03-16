import axios from 'axios';

// base do spring
export const api = axios.create({
  baseURL: 'http://localhost:8081/api',
});