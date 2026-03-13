import axios from 'axios';

// Cria uma instância centralizada com a URL base do nosso motor Spring Boot
export const api = axios.create({
  baseURL: 'http://localhost:8081/api',
});