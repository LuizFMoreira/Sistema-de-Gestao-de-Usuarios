import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../services/api';

export function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');

  async function handleLogin(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro('');

    try {
      const response = await api.post('/auth/login', { email, senha });
      
      localStorage.setItem('@App:token', response.data.token);
      localStorage.setItem('@App:user', response.data.nome);
      
      navigate('/home');
    } catch (error) {
      setErro('E-mail ou senha inválidos.');
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-50 p-4">
      <div className="w-full max-w-md bg-white p-8 rounded-2xl shadow-xl">
        <h2 className="text-3xl font-bold text-slate-800 text-center mb-2">Acesse sua conta</h2>
        <p className="text-slate-500 text-center mb-8">Digite suas credenciais para entrar no painel</p>

        <form onSubmit={handleLogin} className="flex flex-col gap-5">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">E-mail</label>
            <input 
              type="email" required
              value={email} onChange={(e) => setEmail(e.target.value)}
              className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              placeholder="exemplo@email.com"
            />
          </div>
          
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Senha</label>
            <input 
              type="password" required
              value={senha} onChange={(e) => setSenha(e.target.value)}
              className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              placeholder="Sua senha secreta"
            />
          </div>

          {erro && (
            <p className="text-red-500 text-sm font-medium bg-red-50 p-3 rounded-lg border border-red-100 text-center">
              {erro}
            </p>
          )}

          <button 
            type="submit" 
            className="w-full bg-blue-600 text-white p-3 rounded-lg font-bold hover:bg-blue-700 transition-colors mt-2 shadow-lg shadow-blue-600/30"
          >
            Entrar no Sistema
          </button>
        </form>

        <p className="mt-8 text-center text-sm text-slate-600">
          Ainda não tem uma conta?{' '}
          <Link to="/register" className="text-blue-600 font-bold hover:underline">
            Cadastre-se aqui
          </Link>
        </p>
      </div>
    </div>
  );
}