import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Utilizador{
	String username;
	String password;

	List<String> mensagens;

	Utilizador (String username, String password){
		this.username = username;
		this.password = password;
		this.mensagens = new ArrayList<>();
	}

	String getUsername(){
		return this.username;
	}

	String getPassword(){
		return this.password;
	}

	synchronized void adicionarMensagem(String mensagem){
		mensagens.add(mensagem);
	}

	synchronized String getMensagens(){
		if (mensagens.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		Iterator<String> it = mensagens.iterator();
		while(it.hasNext()){
			String s = it.next();
			sb.append(s);
			sb.append("\n");
			it.remove();
		}
		return sb.toString();
	}
}

class Leilao{
	String descricao;
	int id;
	Utilizador vendedor;
	List<Licitacao> licitacoes;
	Licitacao maisAlta;

	Leilao (String descricao, int id, Utilizador vendedor){
		this.descricao = descricao;
		this.id = id;
		this.vendedor = vendedor;
		this.licitacoes = new ArrayList<>();
		this.maisAlta = null;
	}

	int getID(){
		return this.id;
	}

	Utilizador getVendedor(){
		return this.vendedor;
	}

	Utilizador getMaisAlta() throws SemLicitacoesException{
		if (maisAlta == null)
			throw new SemLicitacoesException ("S"); // S -> sem licitacoes

		return this.maisAlta.getUtilizador();
	}

	void licitar (Utilizador comprador, double valor) throws LicitacaoInvalidaException{
		if (comprador.getUsername().equals(this.vendedor.getUsername()))
			throw new LicitacaoInvalidaException ("P"); // P -> proprio leilao

		if (valor <= 0){
			throw new LicitacaoInvalidaException ("V"); // V -> valor invalido
		}
		if (this.maisAlta != null && valor <= this.maisAlta.getValor()){
			throw new LicitacaoInvalidaException ("V"); // V -> valor invalido
		}
		Licitacao l = new Licitacao (comprador, valor);
		this.maisAlta = l;
		this.licitacoes.add(l);
	}

	String listar(){
		StringBuilder sb = new StringBuilder();
        String s;
        s = Integer.toString(this.id);
		sb.append(s);
		sb.append(" - ");
		sb.append(descricao);
		sb.append(" - ");
		if (this.maisAlta == null){
			sb.append ("0");
		}
		else {
            s = Double.toString(this.maisAlta.getValor());
            sb.append(s);
       	}
		return sb.toString();
	}

	String terminar() throws LeilaoInvalidoException, SemLicitacoesException{
		if (this.maisAlta == null)
			throw new SemLicitacoesException ("S"); // S - > sem licitacoes

		List<Utilizador> list = new ArrayList<>();

		Utilizador vencedor = this.maisAlta.getUtilizador();
		String mensagem = this.mensagem(vencedor);

		for (Licitacao l: licitacoes){
			Utilizador comprador = l.getUtilizador();
			Iterator<Utilizador> it = list.iterator();
			boolean existe = false;
			while(it.hasNext() && !existe){
				Utilizador u = it.next();
				if (u.getUsername().equals(comprador.getUsername()))
					existe = true;

			}
			if (!existe){
				comprador.adicionarMensagem(mensagem);
				list.add(comprador);
			}
		}
		return mensagem;
	}

	private String mensagem (Utilizador vencedor){
		StringBuilder sb = new StringBuilder();
                String s;
                s = Integer.toString(this.id);
		sb.append(s);
		sb.append(" - ");
		sb.append(this.descricao);
		sb.append(" - ");
		sb.append(vencedor.getUsername());
		sb.append(" - ");
                s = Double.toString(this.maisAlta.getValor());
		sb.append(s);
		return sb.toString();
	}

	private class Licitacao{
		Utilizador comprador;
		double valor;

		Licitacao (Utilizador comprador, double valor){
			this.comprador = comprador;
			this.valor = valor;
		}

		double getValor(){
			return valor;
		}

		Utilizador getUtilizador(){
			return comprador;
		}
	}
}

class ServicoImpl implements Servico{
	Map<String, Utilizador> utilizadores;
	Map<Integer, Leilao> leiloes;
	int n;

	ServicoImpl (){
		this.utilizadores = new HashMap<>();
		this.leiloes = new HashMap<>();
		n = 0;
	}

	public Utilizador registar (String username, String password) throws UtilizadorInvalidoException{
		synchronized (utilizadores){
			Utilizador u = this.utilizadores.get(username);
			if (u != null){
				throw new UtilizadorInvalidoException ("E"); // E - > existe
			}
			u = new Utilizador (username, password);
			this.utilizadores.put(username, u);
			return u;
		}
	}

	public Utilizador autenticar (String username, String password) throws UtilizadorInvalidoException{
		synchronized (utilizadores){
			Utilizador u = this.utilizadores.get(username);
			if (u == null){
				throw new UtilizadorInvalidoException ("N"); // N -> nao existe
			}
			if (u.getPassword().equals(password)){
				return u;
			}
			else throw new UtilizadorInvalidoException ("P"); // P -> password errada
		}
	}

	public int iniciar (String descricao, Utilizador vendedor){
		synchronized (leiloes){
			this.n++;
			Leilao l = new Leilao (descricao, this.n, vendedor);
			this.leiloes.put(this.n, l);
			return this.n;
		}
	}

	public String listar (Utilizador utilizador){
		synchronized (leiloes){
			StringBuilder sb = new StringBuilder();
			for (Leilao l: leiloes.values()){
				sb.append(l.listar());
				Utilizador vendedor = l.getVendedor();
				if (vendedor.getUsername().equals(utilizador.getUsername())){
					sb.append(" *");
				}
				try{
					Utilizador maisAlta = l.getMaisAlta();
					if (maisAlta.getUsername().equals(utilizador.getUsername())){
						sb.append(" +");
					}
				}
				catch (SemLicitacoesException e){}
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	public void licitar (int id, double valor, Utilizador comprador) throws LicitacaoInvalidaException{
		synchronized (leiloes){
			Leilao l = this.leiloes.get(id);
			if (l == null){
				throw new LicitacaoInvalidaException ("N"); // N -> nao existe
			}
			l.licitar(comprador, valor);
		}
	}

	public String terminar (int id, Utilizador vendedor) throws LeilaoInvalidoException, SemLicitacoesException{
            Leilao l = null;
            synchronized (leiloes){
            	l = this.leiloes.get(id);
            	if (l == null){
            		throw new LeilaoInvalidoException ("N"); // N -> nao existe
            	}
            	Utilizador u = l.getVendedor();
            	if (!u.getUsername().equals(vendedor.getUsername())){
            		throw new LeilaoInvalidoException ("P"); // P -> proprio
            	}
            	this.leiloes.remove(id);
            }
            return l.terminar();
	}

	String mensagens (Utilizador utilizador){
		return utilizador.getMensagens();
	}
}

class UtilizadorInvalidoException extends Exception{

	UtilizadorInvalidoException (String msg){
		super (msg);
	}
}

class LicitacaoInvalidaException extends Exception{

	LicitacaoInvalidaException (String msg){
		super (msg);
	}
}

class LeilaoInvalidoException extends Exception{

	LeilaoInvalidoException (String msg){
		super(msg);
	}
}

class SemMensagensException extends Exception{

	SemMensagensException (String msg){
		super (msg);
	}
}

class SemLicitacoesException extends Exception{

	SemLicitacoesException (String msg){
		super (msg);
	}
}

class Cliente{

	public static void main(String[] args) throws IOException, NumberFormatException{
		ServicoStub servico;
		if (args.length <= 1)
			servico = new ServicoStub();

		else servico = new ServicoStub (args[0], args[1]);

		Utilizador utilizador = null;
		String str = "null";
		while (utilizador == null){
			System.out.print("\n> ");
			str = System.console().readLine();
			if (str == null)
				break;

			switch(str){
				case "registar":	utilizador = registar(servico);
									break;
				case "autenticar":	utilizador = autenticar(servico);
									break;
				case "iniciar":
				case "listar":
				case "licitar":
				case "terminar":
				case "mensagens":	System.out.println("Utilizador nao autenticado!");
									break;
				default:	System.out.println("Comando invalido!");
			}
		}
		while (str != null){
			System.out.print("\n> ");
			str = System.console().readLine();	
			if (str == null)
				break;

			switch (str){
				case "iniciar":	iniciar(servico, utilizador);
								break;
				case "listar":	listar(servico, utilizador);
								break;
				case "licitar":	licitar(servico, utilizador);
								break;
				case "terminar":	terminar(servico, utilizador);
									break;
				case "mensagens":	mensagens(servico, utilizador);
									break;
				case "registar":
				case "autenticar": 	System.out.println("Ja esta autenticado!");
									break;
				default:	System.out.println("Comando invalido!");
			}
		}
		servico.close();
	}

	static Utilizador registar(ServicoStub servico){
		Utilizador utilizador = null;
		System.out.print("Username: ");
		String username = System.console().readLine();
		if (username == null)
			return utilizador;

		System.out.print("Password: ");
		String password = System.console().readLine();
		if (password == null){
			return utilizador;
		}
		try{
			utilizador = servico.registar(username, password);
			System.out.println("Utilizador registado e autenticado!");
		}
		catch (UtilizadorInvalidoException e){
			System.out.println(e.getMessage());
		}
		return utilizador;
	}

	static Utilizador autenticar (ServicoStub servico){
		Utilizador utilizador = null;
		System.out.print("Username: ");
		String username = System.console().readLine();
		if (username == null)
			return utilizador;

		System.out.print("Password: ");
		String password = System.console().readLine();
		if (password == null)
			return utilizador;

		try{
			utilizador = servico.autenticar(username, password);
			System.out.println("Utilizador autenticado!");
		}
		catch (UtilizadorInvalidoException e){
			System.out.println(e.getMessage());
		}
		return utilizador;
	}

	static void iniciar (ServicoStub servico, Utilizador utilizador){
		System.out.print("Descricao: ");
		String descricao = System.console().readLine();
		if (descricao == null)
			return;

		int id = servico.iniciar(descricao, utilizador);
		System.out.println("Leilao criado com o numero " + id);
	}

	static void listar (ServicoStub servico, Utilizador utilizador){
		String str = servico.listar(utilizador);
		System.out.println("ID - Descricao - Valor - Info");
		System.out.print(str);
	}

	static void licitar (ServicoStub servico, Utilizador utilizador) throws NumberFormatException{
		String str;
		System.out.print("Numero: ");
		str = System.console().readLine();
		if (str == null)
			return;

		int id = Integer.parseInt(str);
		System.out.print("Valor: ");
		str = System.console().readLine();
		if (str == null)
			return;

		double valor = Double.parseDouble(str);
		try{
			servico.licitar (id, valor, utilizador);
			System.out.println("Licitacao efetuada!");
		}
		catch (LicitacaoInvalidaException e){
			System.out.println(e.getMessage());
		}	
	}

	static void terminar (ServicoStub servico, Utilizador utilizador) throws NumberFormatException{
		System.out.print("Numero: ");
		String str = System.console().readLine();
		if (str == null)
			return;

		int id = Integer.parseInt(str);
		try{
			String s = servico.terminar(id, utilizador);
			System.out.println("ID - Descricao - Vencedor - Valor");
			System.out.println(s);
		}
		catch (LeilaoInvalidoException | SemLicitacoesException e){
			System.out.println(e.getMessage());
		}
	}

	static void mensagens (ServicoStub servico, Utilizador utilizador){
		String str = servico.mensagens(utilizador);
		System.out.println("ID - Descricao - Vencedor - Valor");
		System.out.print(str);
	}
}

class ServicoStub implements Servico{

	Socket socket;
	BufferedReader in;
	PrintWriter out;

	ServicoStub() throws IOException{
		this("localhost", "12345");
	}

	ServicoStub (String host, String str) throws IOException, NumberFormatException{
		int port = Integer.parseInt(str);
		this.socket = new Socket (host, port);
		this.in = new BufferedReader (new InputStreamReader (socket.getInputStream()));
		this.out = new PrintWriter (socket.getOutputStream());

	}

	public Utilizador registar(String username, String password) throws UtilizadorInvalidoException{
		StringBuilder sb = new StringBuilder();
		sb.append("R "); // R -> registar
		sb.append(username);
		sb.append(" ");
		sb.append(password);
		out.println(sb.toString());
		out.flush();
		try{
			String str = in.readLine();
			if (str.equals("E"))
				throw new UtilizadorInvalidoException ("Utilizador ja existe!");

			return new Utilizador (username, password);
		}
		catch (IOException e){
			System.out.println(e.getMessage());
			return null;
		}
	}

	public Utilizador autenticar(String username, String password) throws UtilizadorInvalidoException{
		StringBuilder sb = new StringBuilder();
		sb.append("A "); // A -> autenticar
		sb.append(username);
		sb.append(" ");
		sb.append(password);
		out.println(sb.toString());
		out.flush();
		try{
			String str = in.readLine();
			if (str.equals("P"))
				throw new UtilizadorInvalidoException ("Password errada!");

			if (str.equals("N"))
				throw new UtilizadorInvalidoException ("Username nao existe!");

			return new Utilizador (username, password);
		}
		catch (IOException e){
			System.out.println(e.getMessage());
			return null;
		}
	}

	public int iniciar (String descricao, Utilizador vendedor){
		StringBuilder sb = new StringBuilder();
		sb.append("I "); // I -> iniciar
		sb.append(descricao);
		out.println(sb.toString());
		out.flush();
		try{
			String str = in.readLine();
			return Integer.parseInt(str);
		}
		catch (IOException e){
			System.out.println(e.getMessage());
			return -1;
		}
	}

	public String listar(Utilizador utilizador){
		out.println("Lis"); // Lis -> listar
		out.flush();
		try{
			StringBuilder sb = new StringBuilder();
			String str = in.readLine();
			while (!str.equals("")){
				sb.append(str);
				sb.append("\n");
				str = in.readLine();
			}
			return sb.toString();
		}
		catch (IOException e){
			System.out.println(e.getMessage());
			return "";
		}
	}

	public void licitar(int id, double valor, Utilizador comprador) throws LicitacaoInvalidaException{
		StringBuilder sb = new StringBuilder();
        String s;
		sb.append("Lic "); // Lic -> licitar
        s = Integer.toString(id);
		sb.append(s);
		sb.append(" ");
        s = Double.toString(valor);
		sb.append(s);
		out.println(sb.toString());
		out.flush();
		try{
			String str = in.readLine();
			if (str.equals("P"))
				throw new LicitacaoInvalidaException ("Nao pode licitar no proprio leilao!");

			if (str.equals("V"))
				throw new LicitacaoInvalidaException ("Valor invalido!");

			if (str.equals("N"))
				throw new LicitacaoInvalidaException ("Leilao nao existe!");
		}
		catch (IOException e){
			System.out.println(e.getMessage());
		}
	}

	public String terminar(int id, Utilizador vendedor) throws LeilaoInvalidoException, SemLicitacoesException{
		StringBuilder sb = new StringBuilder();
                String s;
		sb.append("T "); // T -> terminar
                s = Integer.toString(id);
		sb.append(s);
		out.println(sb.toString());
		out.flush();
		try{
			String str = in.readLine();
			if (str.equals("P"))
				throw new LeilaoInvalidoException ("So pode terminar os proprios leiloes!");

			if (str.equals("N"))
				throw new LeilaoInvalidoException ("Leilao nao existe!");

			if (str.equals("S"))
				throw new SemLicitacoesException ("Leilao sem licitacoes");
                        
                        return str;
		}
		catch (IOException e){
			System.out.println(e.getMessage());
                        return "";
		}
	}

	String mensagens(Utilizador utilizador){
		out.println("M"); // M -> mensagem
		out.flush();
		try{
			StringBuilder sb = new StringBuilder();
			String str = in.readLine();
			while (!str.equals("")){
				sb.append(str);
				sb.append("\n");
				str = in.readLine();
			}
			return sb.toString();
		}
		catch (IOException e){
			System.out.println(e.getMessage());
			return "";
		}
	}

	void close(){
		out.close();
	}
}

class Servidor{
	public static void main(String[] args) throws IOException, NumberFormatException{
		int port;
		if (args.length < 1){
			port = 12345;
		}
		else {
			String str = args[0];
			port = Integer.parseInt(str);
		}
		ServerSocket srv = new ServerSocket (port);

		ServicoImpl servico = new ServicoImpl();
		while(true){
			Socket cli = srv.accept();
			Thread t = new Thread (new ClientHandler(cli, servico));
			t.start();
		}
	}
}

class ClientHandler implements Runnable{

	Socket cli;
	BufferedReader in;
	PrintWriter out;
	ServicoImpl servico;
	Utilizador utilizador;

	ClientHandler (Socket cli, ServicoImpl servico) throws IOException{
		this.cli = cli;
		this.in = new BufferedReader (new InputStreamReader (cli.getInputStream()));
		this.out = new PrintWriter (cli.getOutputStream());
		this.servico = servico;
	}

	public void run(){
		try{
			String str = "null";
			while (str != null){
				str = in.readLine();
				if (str == null)
					break;

				String[] strs = str.split(" ");
				switch (strs[0].trim()){
					case "R":	this.registar(strs[1].trim(), strs[2].trim());
								break;
					case "A":	this.autenticar(strs[1].trim(), strs[2].trim());
								break;
					case "I":	this.iniciar(strs[1].trim());
								break;
					case "Lis":	this.listar();
								break;
					case "Lic":	this.licitar(strs[1].trim(), strs[2].trim());
								break;
					case "T":	this.terminar(strs[1].trim());
								break;
					case "M":	this.mensagens();
								break;
				}
			}
			out.close();
			cli.close();
		}
		catch (IOException e){
			System.out.println(e.getMessage());
		}
	}

	void registar(String username, String password) throws IOException{
		try{
			this.utilizador = servico.registar(username, password);
			out.println("R"); // R -> registado
		}
		catch (UtilizadorInvalidoException e){
			out.println(e.getMessage());
		}
		out.flush();
	}

	void autenticar(String username, String password) throws IOException{
		try{
			this.utilizador = servico.autenticar(username, password);
			out.println("A"); // A -> autenticado
		}
		catch (UtilizadorInvalidoException e){
			out.println(e.getMessage());
		}
		out.flush();
	}

	void iniciar(String descricao) throws IOException{
		int id = servico.iniciar(descricao, utilizador);
		out.println(Integer.toString(id));
		out.flush();
	}

	void listar(){
		String str = servico.listar(utilizador);
		out.println(str);
		out.flush();
	}

	void licitar(String str1, String str2) throws IOException{
		int id = Integer.parseInt(str1);
		double valor = Double.parseDouble(str2);
		try{
			servico.licitar(id, valor, utilizador);
			out.println("E"); // E -> efetuada
		}
		catch (LicitacaoInvalidaException e){
			out.println(e.getMessage());
		}
		out.flush();
	}

	void terminar(String str) throws IOException{
		int id = Integer.parseInt(str);
		try{
			String s = servico.terminar(id, utilizador);
			out.println(s); // T -> terminado
		}
		catch (LeilaoInvalidoException | SemLicitacoesException e){
			out.println(e.getMessage());
		}
		out.flush();
	}

	void mensagens(){
		String str = servico.mensagens(utilizador);
		out.println(str);
		out.flush();
	}
}

interface Servico{
	Utilizador registar (String username, String password) throws UtilizadorInvalidoException;
	Utilizador autenticar (String username, String password) throws UtilizadorInvalidoException;
	int iniciar (String descricao, Utilizador vendedor);
	String listar(Utilizador utilizador);
	void licitar(int id, double valor, Utilizador comprador) throws LicitacaoInvalidaException;
	String terminar(int id, Utilizador vendedor) throws LeilaoInvalidoException, SemLicitacoesException;
}