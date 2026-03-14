import { useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../services/api';
import { isAxiosError } from 'axios';
import { useNavigate } from 'react-router-dom';

export function Login() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mensagem, setMensagem] = useState({ texto: '', erro: false });
  const navigate = useNavigate();

  async function handleLogin(e: FormEvent) {
    e.preventDefault();

    try {
      // bate no endpoint de auth que a gente liberou no SecurityConfig
      const response = await api.post('/auth/login', { email, senha });
      
      const { token, nome } = response.data;

      // guarda o token e o nome pra usar no resto do app (e o F5 não deslogar)
      localStorage.setItem('@App:token', token);
      localStorage.setItem('@App:user', nome);

      setMensagem({ texto: `Boa! Bem-vindo, ${nome}. Entrando...`, erro: false });

      // um delayzinho só pro usuário ler a mensagem de sucesso
      setTimeout(() => {
        navigate('/home');
      }, 1200);

    } catch (error) {
      // se der 401 ou 403, a senha ou e-mail estão zoados
      if (isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) {
        setMensagem({ texto: 'E-mail ou senha inválidos.', erro: true });
      } else {
        setMensagem({ texto: 'Ih, deu erro na conexão com o Java.', erro: true });
      }
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-100 p-4">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-2xl shadow-xl">
        <div className="text-center">
          <h2 className="text-3xl font-bold text-slate-800 italic">Login</h2>
          <p className="text-sm text-slate-500 mt-2">Acesse sua conta para gerenciar</p>
        </div>
        
        {mensagem.texto && (
          <div className={`p-3 text-sm rounded-lg text-center font-medium ${
            mensagem.erro ? 'bg-red-50 text-red-600 border border-red-200' : 'bg-green-50 text-green-600 border border-green-200'
          }`}>
            {mensagem.texto}
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase ml-1 mb-1">E-mail</label>
            <input 
              type="email" placeholder="exemplo@gmail.com" required
              className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              value={email} onChange={e => setEmail(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase ml-1 mb-1">Senha</label>
            <input 
              type="password" placeholder="••••••••" required
              className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              value={senha} onChange={e => setSenha(e.target.value)}
            />
          </div>

          <button 
            type="submit" 
            className="w-full py-3 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all active:scale-95"
          >
            Acessar Painel
          </button>
        </form>

        <div className="pt-4 border-t border-slate-100 text-center">
          <p className="text-sm text-slate-600">
            Ainda não tem acesso? 
            <button 
              onClick={() => navigate('/cadastro')} 
              className="ml-1 text-blue-600 font-semibold hover:underline"
            >
              Criar conta agora
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}