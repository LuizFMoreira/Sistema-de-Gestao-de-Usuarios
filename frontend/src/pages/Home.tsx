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

  // editar o perfil
  const [editandoPerfil, setEditandoPerfil] = useState(false);
  const [editNome, setEditNome] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editSenha, setEditSenha] = useState('');

  // criancao de usuarios
  const [criandoUsuario, setCriandoUsuario] = useState(false);
  const [novoNome, setNovoNome] = useState('');
  const [novoEmail, setNovoEmail] = useState('');
  const [novaSenha, setNovaSenha] = useState('');

  // força da senha ---
  const editTemTamanho = editSenha.length >= 8;
  const editTemMaiuscula = /[A-Z]/.test(editSenha);
  const editTemNumero = /[0-9]/.test(editSenha);
  const forcaEdit = [editTemTamanho, editTemMaiuscula, editTemNumero].filter(Boolean).length;

  // força da senha ---
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
    try {
      const response = await api.get('/usuarios', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setUsuarios(response.data);
    } catch (error) {
      localStorage.clear();
      navigate('/login');
    }
  }

  //função de edit
  function abrirPainelEdicao() {
    setEditNome(userName);
    setEditEmail(''); 
    setEditSenha('');
    setMensagemSucesso(''); setErro(''); setCriandoUsuario(false);
    setEditandoPerfil(true);
  }

  async function handleAtualizarPerfil(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(''); setMensagemSucesso('');
    
    // Trava: se ele digitou uma senha e ela for fraca, bloqueia.
    if (editSenha.length > 0 && forcaEdit < 3) {
      setErro('A nova senha não atende aos requisitos de segurança.');
      return;
    }

    const token = localStorage.getItem('@App:token');
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
      setErro(error.response?.data?.message || 'Erro ao atualizar o perfil.');
    }
  }

  // --- função para criar novo usuario ---
  function abrirPainelCriacao() {
    setNovoNome(''); setNovoEmail(''); setNovaSenha('');
    setMensagemSucesso(''); setErro(''); setEditandoPerfil(false);
    setCriandoUsuario(true);
  }

  async function handleCriarUsuario(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(''); setMensagemSucesso('');
    
    // Trava de segurança da senha
    if (forcaNovo < 3) {
      setErro('A senha do novo usuário não atende aos requisitos.');
      return;
    }

    try {
      await api.post('/usuarios', {
        nome: novoNome, email: novoEmail, senha: novaSenha
      });

      setCriandoUsuario(false);
      setMensagemSucesso('Novo usuário adicionado com sucesso!');
      buscarUsuarios();
    } catch (error: any) {
      setErro(error.response?.data?.erro || error.response?.data?.message || 'Erro ao criar usuário.');
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
            + Novo Usuário
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
              <input type="password" value={editSenha} onChange={(e) => setEditSenha(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" placeholder="Deixe em branco para manter a atual" />
              
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

      {/* Formulário: Adicionar Novo Usuário */}
      {criandoUsuario && (
        <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md mb-8 border-t-4 border-green-500">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold text-slate-800">Cadastrar Novo Usuário</h2>
            <button onClick={() => setCriandoUsuario(false)} className="text-slate-400 hover:text-slate-600">Cancelar</button>
          </div>
          <form onSubmit={handleCriarUsuario} className="flex flex-col gap-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nome do Usuário</label>
                <input type="text" required value={novoNome} onChange={(e) => setNovoNome(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 outline-none" placeholder="Ex: Maria" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">E-mail</label>
                <input type="email" required value={novoEmail} onChange={(e) => setNovoEmail(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 outline-none" placeholder="maria@email.com" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Senha Provisória</label>
              <input type="password" required value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 outline-none" placeholder="Crie uma senha para o usuário" />
              
              {/* Barra de Força - Novo Usuário */}
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

      {}
      <div className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md">
        <h2 className="text-xl font-bold text-slate-800 mb-4">Usuários Cadastrados no Sistema</h2>
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
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}