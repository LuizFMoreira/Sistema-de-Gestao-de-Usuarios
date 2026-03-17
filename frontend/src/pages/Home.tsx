import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';

interface Usuario {
  id: number;
  nome: string;
  email: string;
}

export function Home() {
  const navigate = useNavigate();
  const [userName, setUserName] = useState(localStorage.getItem('@App:user') || '');
  
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [erro, setErro] = useState('');
  const [mensagemSucesso, setMensagemSucesso] = useState('');

  // Estados: Editar Perfil
  const [editandoPerfil, setEditandoPerfil] = useState(false);
  const [editNome, setEditNome] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editSenha, setEditSenha] = useState('');
  const [mostrarEditSenha, setMostrarEditSenha] = useState(false);

  // Estados: Criação de Novo Utilizador
  const [criandoUsuario, setCriandoUsuario] = useState(false);
  const [novoNome, setNovoNome] = useState('');
  const [novoEmail, setNovoEmail] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [mostrarNovaSenha, setMostrarNovaSenha] = useState(false);

  // --- Matemática da Força da Senha ---
  const editTemTamanho = editSenha.length >= 8;
  const editTemMaiuscula = /[A-Z]/.test(editSenha);
  const editTemNumero = /[0-9]/.test(editSenha);
  const forcaEdit = [editTemTamanho, editTemMaiuscula, editTemNumero].filter(Boolean).length;

  const novoTemTamanho = novaSenha.length >= 8;
  const novoTemMaiuscula = /[A-Z]/.test(novaSenha);
  const novoTemNumero = /[0-9]/.test(novaSenha);
  const forcaNovo = [novoTemTamanho, novoTemMaiuscula, novoTemNumero].filter(Boolean).length;

  function getCorBarra(forca: number) {
    if (forca === 0) return 'bg-slate-200';
    if (forca === 1) return 'bg-red-500 w-1/3';
    if (forca === 2) return 'bg-yellow-500 w-2/3';
    return 'bg-green-500 w-full';
  }

  useEffect(() => {
    buscarUsuarios();
  }, []);

  async function buscarUsuarios() {
    const token = localStorage.getItem('@App:token');
    if (!token) {
      navigate('/login');
      return;
    }
    try {
      const response = await api.get('/usuarios', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setUsuarios(response.data);
    } catch (error: any) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.clear();
        navigate('/login');
      }
    }
  }

  // --- Funções de Edição ---
  function abrirPainelEdicao() {
    setEditNome(userName);
    setEditEmail(''); 
    setEditSenha('');
    setMostrarEditSenha(false);
    setMensagemSucesso(''); setErro(''); setCriandoUsuario(false);
    setEditandoPerfil(true);
  }

  async function handleAtualizarPerfil(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(''); setMensagemSucesso('');
    
    if (editSenha.length > 0 && forcaEdit < 3) {
      setErro('A nova senha não atende aos requisitos de segurança.');
      return;
    }

    const token = localStorage.getItem('@App:token');
    if (!token) {
      localStorage.clear();
      navigate('/login');
      return;
    }

    try {
      const response = await api.put('/usuarios/perfil', {
        nome: editNome, email: editEmail, senha: editSenha
      }, { headers: { Authorization: `Bearer ${token}` }});

      const nomeAtualizado = response.data.nome;
      localStorage.setItem('@App:user', nomeAtualizado);
      setUserName(nomeAtualizado);
      setEditandoPerfil(false);
      setMensagemSucesso('Perfil atualizado com sucesso!');
      buscarUsuarios(); 
    } catch (error: any) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.clear();
        navigate('/login');
        return;
      }
      setErro(error.response?.data?.message || error.response?.data?.erro || 'Erro ao atualizar o perfil.');
    }
  }

  // --- Funções de Criação de Novo Utilizador ---
  function abrirPainelCriacao() {
    setNovoNome(''); setNovoEmail(''); setNovaSenha('');
    setMostrarNovaSenha(false);
    setMensagemSucesso(''); setErro(''); setEditandoPerfil(false);
    setCriandoUsuario(true);
  }

  async function handleCriarUsuario(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(''); setMensagemSucesso('');
    
    if (forcaNovo < 3) {
      setErro('A senha do novo utilizador não atende aos requisitos.');
      return;
    }

    const token = localStorage.getItem('@App:token');
    if (!token) {
      localStorage.clear();
      navigate('/login');
      return;
    }

    try {
      await api.post('/usuarios', {
        nome: novoNome, email: novoEmail, senha: novaSenha
      }, { headers: { Authorization: `Bearer ${token}` }});

      setCriandoUsuario(false);
      setMensagemSucesso('Novo utilizador adicionado com sucesso!');
      buscarUsuarios();
    } catch (error: any) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.clear();
        navigate('/login');
        return;
      }
      setErro(error.response?.data?.erro || error.response?.data?.message || 'Erro ao criar utilizador.');
    }
  }

  // --- Função de Exclusão de Utilizador ---
  async function handleDeletarUsuario(id: number, nome: string) {
    // Barreira de segurança UX: Confirmação antes de apagar
    const confirmacao = window.confirm(`Tem a certeza absoluta que deseja excluir o utilizador "${nome}"? Esta ação é irreversível.`);
    if (!confirmacao) return;

    const token = localStorage.getItem('@App:token');
    if (!token) {
      localStorage.clear();
      navigate('/login');
      return;
    }

    try {
      await api.delete(`/usuarios/${id}`, { 
        headers: { Authorization: `Bearer ${token}` } 
      });
      setMensagemSucesso(`Utilizador ${nome} excluído com sucesso!`);
      buscarUsuarios(); // Atualiza a tabela imediatamente
    } catch (error: any) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.clear();
        navigate('/login');
        return;
      }
      setErro(error.response?.data?.erro || 'Erro ao excluir o utilizador.');
    }
  }

  function handleLogout() {
    localStorage.clear(); 
    navigate('/login');   
  }

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      {/* Cabeçalho */}
      <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Painel de Gestão</h1>
          <p className="text-slate-600">Bem-vindo de volta, <span className="font-semibold text-blue-600">{userName}</span>!</p>
        </div>
        <div className="flex gap-3">
          <button onClick={abrirPainelCriacao} className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors font-medium shadow-md">
            + Novo Utilizador
          </button>
          <button onClick={abrirPainelEdicao} className="px-4 py-2 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors font-medium">
            Editar Perfil
          </button>
          <button onClick={handleLogout} className="px-4 py-2 bg-red-100 text-red-600 rounded-lg hover:bg-red-200 transition-colors font-medium">
            Sair
          </button>
        </div>
      </div>

      {/* Alertas Globais */}
      {mensagemSucesso && <div className="max-w-4xl mx-auto mb-4 p-4 bg-green-100 text-green-700 rounded-lg font-medium">{mensagemSucesso}</div>}
      {erro && !editandoPerfil && !criandoUsuario && <div className="max-w-4xl mx-auto mb-4 p-4 bg-red-100 text-red-700 rounded-lg font-medium">{erro}</div>}

      {/* Formulário: Atualizar Perfil */}
      {editandoPerfil && (
        <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md mb-8 border-t-4 border-blue-500">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold text-slate-800">Atualizar Meus Dados</h2>
            <button onClick={() => setEditandoPerfil(false)} className="text-slate-400 hover:text-slate-600">Cancelar</button>
          </div>
          <form onSubmit={handleAtualizarPerfil} className="flex flex-col gap-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Novo Nome</label>
                <input type="text" required value={editNome} onChange={(e) => setEditNome(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" placeholder="Seu novo nome" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Novo E-mail</label>
                <input type="email" required value={editEmail} onChange={(e) => setEditEmail(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" placeholder="Seu novo e-mail" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Nova Senha (opcional)</label>
              <div className="relative">
                <input 
                  type={mostrarEditSenha ? "text" : "password"} 
                  value={editSenha} onChange={(e) => setEditSenha(e.target.value)} 
                  className="w-full p-3 pr-12 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" 
                  placeholder="Deixe em branco para manter a atual" 
                />
                <button 
                  type="button"
                  onClick={() => setMostrarEditSenha(!mostrarEditSenha)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-blue-600 transition-colors"
                >
                  {mostrarEditSenha ? (
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                  )}
                </button>
              </div>
              
              {/* Barra de Força - Edição */}
              {editSenha.length > 0 && (
                <div className="mt-3">
                  <div className="h-1.5 w-full bg-slate-200 rounded-full overflow-hidden">
                    <div className={`h-full transition-all duration-300 ${getCorBarra(forcaEdit)}`}></div>
                  </div>
                  <ul className="mt-2 text-xs text-slate-500 flex flex-col gap-1">
                    <li className={editTemTamanho ? "text-green-600 font-medium" : ""}>{editTemTamanho ? "✓" : "○"} Mínimo de 8 caracteres</li>
                    <li className={editTemMaiuscula ? "text-green-600 font-medium" : ""}>{editTemMaiuscula ? "✓" : "○"} Pelo menos uma letra maiúscula</li>
                    <li className={editTemNumero ? "text-green-600 font-medium" : ""}>{editTemNumero ? "✓" : "○"} Pelo menos um número</li>
                  </ul>
                </div>
              )}
            </div>
            {erro && editandoPerfil && <p className="text-red-500 text-sm font-medium bg-red-50 p-2 rounded">{erro}</p>}
            <button 
              type="submit" 
              disabled={editSenha.length > 0 && forcaEdit < 3}
              className={`w-full p-3 rounded-lg font-bold transition-colors mt-2 ${editSenha.length > 0 && forcaEdit < 3 ? 'bg-slate-300 text-slate-500 cursor-not-allowed' : 'bg-blue-600 text-white hover:bg-blue-700'}`}
            >
              Salvar Alterações
            </button>
          </form>
        </div>
      )}

      {/* Formulário: Adicionar Novo Utilizador */}
      {criandoUsuario && (
        <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md mb-8 border-t-4 border-green-500">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold text-slate-800">Cadastrar Novo Utilizador</h2>
            <button onClick={() => setCriandoUsuario(false)} className="text-slate-400 hover:text-slate-600">Cancelar</button>
          </div>
          <form onSubmit={handleCriarUsuario} className="flex flex-col gap-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nome do Utilizador</label>
                <input type="text" required value={novoNome} onChange={(e) => setNovoNome(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 outline-none" placeholder="Ex: Maria" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">E-mail</label>
                <input type="email" required value={novoEmail} onChange={(e) => setNovoEmail(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 outline-none" placeholder="maria@email.com" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Senha Provisória</label>
              <div className="relative">
                <input 
                  type={mostrarNovaSenha ? "text" : "password"} required 
                  value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} 
                  className="w-full p-3 pr-12 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 outline-none" 
                  placeholder="Crie uma senha para o utilizador" 
                />
                <button 
                  type="button"
                  onClick={() => setMostrarNovaSenha(!mostrarNovaSenha)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-green-600 transition-colors"
                >
                  {mostrarNovaSenha ? (
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                  )}
                </button>
              </div>
              
              {/* Barra de Força - Novo Utilizador */}
              {novaSenha.length > 0 && (
                <div className="mt-3">
                  <div className="h-1.5 w-full bg-slate-200 rounded-full overflow-hidden">
                    <div className={`h-full transition-all duration-300 ${getCorBarra(forcaNovo)}`}></div>
                  </div>
                  <ul className="mt-2 text-xs text-slate-500 flex flex-col gap-1">
                    <li className={novoTemTamanho ? "text-green-600 font-medium" : ""}>{novoTemTamanho ? "✓" : "○"} Mínimo de 8 caracteres</li>
                    <li className={novoTemMaiuscula ? "text-green-600 font-medium" : ""}>{novoTemMaiuscula ? "✓" : "○"} Pelo menos uma letra maiúscula</li>
                    <li className={novoTemNumero ? "text-green-600 font-medium" : ""}>{novoTemNumero ? "✓" : "○"} Pelo menos um número</li>
                  </ul>
                </div>
              )}
            </div>
            {erro && criandoUsuario && <p className="text-red-500 text-sm font-medium bg-red-50 p-2 rounded">{erro}</p>}
            <button 
              type="submit" 
              disabled={forcaNovo < 3}
              className={`w-full p-3 rounded-lg font-bold transition-colors mt-2 ${forcaNovo < 3 ? 'bg-slate-300 text-slate-500 cursor-not-allowed' : 'bg-green-600 text-white hover:bg-green-700'}`}
            >
              Adicionar ao Sistema
            </button>
          </form>
        </div>
      )}

      {/* Tabela de Utilizadores */}
      <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md">
        <h2 className="text-xl font-bold text-slate-800 mb-4">Utilizadores Registados no Sistema</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-100 border-b border-slate-200">
                <th className="p-3 font-semibold text-slate-700">ID</th>
                <th className="p-3 font-semibold text-slate-700">Nome</th>
                <th className="p-3 font-semibold text-slate-700">E-mail</th>
                <th className="p-3 font-semibold text-slate-700 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(user => (
                <tr key={user.id} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                  <td className="p-3 text-slate-600">#{user.id}</td>
                  <td className="p-3 text-slate-800 font-medium">{user.nome}</td>
                  <td className="p-3 text-slate-600">{user.email}</td>
                  <td className="p-3 text-right">
                    <button 
                      onClick={() => handleDeletarUsuario(user.id, user.nome)}
                      className="text-red-500 hover:text-red-700 font-medium transition-colors flex items-center justify-end w-full gap-1"
                      title="Excluir utilizador"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                      </svg>
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}