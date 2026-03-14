import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';

// Tipagem para os dados que virão do banco
interface Usuario {
  id: number;
  nome: string;
  email: string;
}

export function Home() {
  const navigate = useNavigate();
  const userName = localStorage.getItem('@App:user');
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [erro, setErro] = useState('');

  // Iniciamos o effect
  useEffect(() => {
    buscarUsuarios();
  }, []);

  async function buscarUsuarios() {
    const token = localStorage.getItem('@App:token');

    try {
      // Token está sendo mandando para o backend
      const response = await api.get('/usuarios', {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      setUsuarios(response.data);
    } catch (error) {
      console.error(error);
      setErro('Sua sessão expirou ou você não tem permissão. Faça login novamente.');
      // Se der erro de permissão, limpamos o cofre e mandamos pro login
      localStorage.clear();
      navigate('/login');
    }
  }

  function handleLogout() {
    localStorage.clear(); 
    navigate('/login');   
  }

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Painel de Gestão</h1>
          <p className="text-slate-600">Bem-vindo de volta, <span className="font-semibold">{userName}</span>!</p>
        </div>
        <button 
          onClick={handleLogout}
          className="px-4 py-2 bg-red-100 text-red-600 rounded-lg hover:bg-red-200 transition-colors font-medium"
        >
          Sair do Sistema
        </button>
      </div>

      <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md">
        <h2 className="text-xl font-bold text-slate-800 mb-4">Usuários Cadastrados</h2>
        
        {erro ? (
          <p className="text-red-500 bg-red-50 p-3 rounded-lg">{erro}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-100 border-b border-slate-200">
                  <th className="p-3 font-semibold text-slate-700">ID</th>
                  <th className="p-3 font-semibold text-slate-700">Nome</th>
                  <th className="p-3 font-semibold text-slate-700">E-mail</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map(user => (
                  <tr key={user.id} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                    <td className="p-3 text-slate-600">#{user.id}</td>
                    <td className="p-3 text-slate-800 font-medium">{user.nome}</td>
                    <td className="p-3 text-slate-600">{user.email}</td>
                  </tr>
                ))}
                {usuarios.length === 0 && (
                  <tr>
                    <td colSpan={3} className="p-4 text-center text-slate-500">
                      Buscando dados seguros...
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}