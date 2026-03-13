import { useState } from 'react';
import type { FormEvent } from 'react'
import { api } from '../services/api';


export function Register() {
  // Matriz de estados para capturar as entradas do usuário
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mensagem, setMensagem] = useState({ texto: '', erro: false });

  // Função disparada ao submeter a equação (o formulário)
  async function handleRegister(e: FormEvent) {
    e.preventDefault(); // Impede o recarregamento padrão da página

    try {
      // Vetor de envio (DTO) exato que o Spring Boot espera
      await api.post('/usuarios', { nome, email, senha });
      
      setMensagem({ texto: 'Usuário cadastrado com sucesso! 🎉', erro: false });
      // Limpa os campos após o sucesso
      setNome(''); setEmail(''); setSenha('');
      
    } catch (error) {
      setMensagem({ texto: 'Erro ao cadastrar. Verifique os dados e tente novamente.', erro: true });
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-100">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-2xl shadow-xl">
        <h2 className="text-3xl font-bold text-center text-slate-800">Criar Conta</h2>
        
        {mensagem.texto && (
          <div className={`p-3 text-sm rounded-lg ${mensagem.erro ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
            {mensagem.texto}
          </div>
        )}

        <form onSubmit={handleRegister} className="space-y-4">
          <div>
            <label className="block mb-1 text-sm font-medium text-slate-700">Nome Completo</label>
            <input 
              type="text" required
              value={nome} onChange={(e) => setNome(e.target.value)}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="Digite seu nome"
            />
          </div>

          <div>
            <label className="block mb-1 text-sm font-medium text-slate-700">E-mail</label>
            <input 
              type="email" required
              value={email} onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="exemplo@email.com"
            />
          </div>

          <div>
            <label className="block mb-1 text-sm font-medium text-slate-700">Senha</label>
            <input 
              type="password" required
              value={senha} onChange={(e) => setSenha(e.target.value)}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="••••••••"
            />
          </div>

          <button 
            type="submit" 
            className="w-full py-3 text-white transition-colors bg-blue-600 rounded-lg hover:bg-blue-700 font-semibold"
          >
            Cadastrar
          </button>
        </form>
      </div>
    </div>
  );
}