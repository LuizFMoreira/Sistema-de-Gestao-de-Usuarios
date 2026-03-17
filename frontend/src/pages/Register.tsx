import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { isAxiosError } from 'axios';

export function Register() {
  const navigate = useNavigate();
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mensagem, setMensagem] = useState({ texto: '', erro: false });

  // Estado para controlar o olhinho da senha
  const [mostrarSenha, setMostrarSenha] = useState(false);

  const temTamanho = senha.length >= 8;
  const temMaiuscula = /[A-Z]/.test(senha);
  const temNumero = /[0-9]/.test(senha);
  const forcaSenha = [temTamanho, temMaiuscula, temNumero].filter(Boolean).length;

  async function handleRegister(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); 
    setMensagem({ texto: '', erro: false });

    if (forcaSenha < 3) {
      setMensagem({ texto: 'A senha não atende a todos os requisitos.', erro: true });
      return;
    }

    try {
      await api.post('/usuarios', { nome, email, senha });
      setMensagem({ texto: 'Usuário cadastrado com sucesso! 🎉 Redirecionando...', erro: false });
      
      setTimeout(() => navigate('/login'), 2000);
    } catch (error) {
      if (isAxiosError(error) && error.response?.data?.erro) {
        setMensagem({ texto: error.response.data.erro, erro: true });
      } else {
        setMensagem({ texto: 'Erro de conexão com o servidor.', erro: true });
      }
    }
  }

  function getCorBarra() {
    if (forcaSenha === 0) return 'bg-slate-200';
    if (forcaSenha === 1) return 'bg-red-500 w-1/3';
    if (forcaSenha === 2) return 'bg-yellow-500 w-2/3';
    return 'bg-green-500 w-full';
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-50 p-4">
      <div className="w-full max-w-md bg-white p-8 rounded-2xl shadow-xl">
        
        <h2 className="text-3xl font-bold text-center text-slate-800 mb-2">Criar Conta</h2>
        <p className="text-slate-500 text-center mb-6">Preencha os dados para se registrar</p>
        
        {mensagem.texto && (
          <div className={`p-3 mb-4 text-sm font-medium rounded-lg text-center ${mensagem.erro ? 'bg-red-50 text-red-700 border border-red-100' : 'bg-green-50 text-green-700 border border-green-100'}`}>
            {mensagem.texto}
          </div>
        )}

        <form onSubmit={handleRegister} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Nome Completo</label>
            <input 
              type="text" required value={nome} onChange={(e) => setNome(e.target.value)}
              className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              placeholder="Ex: Luiz Fernando"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">E-mail</label>
            <input 
              type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
              className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              placeholder="exemplo@email.com"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Senha Segura</label>
            <div className="relative">
              <input 
                type={mostrarSenha ? "text" : "password"} required value={senha} onChange={(e) => setSenha(e.target.value)}
                className="w-full p-3 pr-12 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                placeholder="Crie uma senha forte"
              />
              <button 
                type="button"
                onClick={() => setMostrarSenha(!mostrarSenha)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-blue-600 transition-colors"
                title={mostrarSenha ? "Ocultar senha" : "Mostrar senha"}
              >
                {mostrarSenha ? (
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                )}
              </button>
            </div>
            
            {senha.length > 0 && (
              <div className="mt-3">
                <div className="h-1.5 w-full bg-slate-200 rounded-full overflow-hidden">
                  <div className={`h-full transition-all duration-300 ${getCorBarra()}`}></div>
                </div>
                <ul className="mt-2 text-xs text-slate-500 flex flex-col gap-1">
                  <li className={temTamanho ? "text-green-600 font-medium" : ""}>
                    {temTamanho ? "✓" : "○"} Mínimo de 8 caracteres
                  </li>
                  <li className={temMaiuscula ? "text-green-600 font-medium" : ""}>
                    {temMaiuscula ? "✓" : "○"} Pelo menos uma letra maiúscula
                  </li>
                  <li className={temNumero ? "text-green-600 font-medium" : ""}>
                    {temNumero ? "✓" : "○"} Pelo menos um número
                  </li>
                </ul>
              </div>
            )}
          </div>

          <button 
            type="submit" disabled={forcaSenha < 3}
            className={`w-full p-3 rounded-lg font-bold transition-all shadow-lg mt-2
              ${forcaSenha < 3 
                ? 'bg-slate-300 text-slate-500 cursor-not-allowed' 
                : 'bg-blue-600 text-white hover:bg-blue-700 shadow-blue-600/30'}`}
          >
            Cadastrar
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Já possui uma conta?{' '}
          <Link to="/login" className="text-blue-600 font-bold hover:underline">
            Faça login aqui
          </Link>
        </p>

      </div>
    </div>
  );
}