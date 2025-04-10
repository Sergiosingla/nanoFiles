package es.um.redes.nanoFiles.tcp.server;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;




public class NFServer implements Runnable {

	public static final int PORT = 10000;



	private ServerSocket serverSocket = null;

	public NFServer() throws IOException {
		/*
		 * (Boletín SocketsTCP) Crear una direción de socket a partir del puerto
		 * especificado (PORT)
		 */
		InetSocketAddress socketAddress = new InetSocketAddress(PORT);


		/*
		 * (Boletín SocketsTCP) Crear un socket servidor y ligarlo a la dirección
		 * de socket anterior
		 */
		serverSocket = new ServerSocket();
		serverSocket.bind(socketAddress);
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación.
	 * 
	 */
	public void test() {
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"[fileServerTestMode] Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out
					.println("[fileServerTestMode] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		while (true) {
			/*
			 * (Boletín SocketsTCP) Usar el socket servidor para esperar conexiones de
			 * otros peers que soliciten descargar ficheros.
			 */

			boolean connection = false;
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				connection = true;
			} catch (IOException e) {
				System.err.println("[-] Error accepting connection from client");
			}
			/*
			 * (Boletín SocketsTCP) Tras aceptar la conexión con un peer cliente, la
			 * comunicación con dicho cliente para servir los ficheros solicitados se debe
			 * implementar en el método serveFilesToClient, al cual hay que pasarle el
			 * socket devuelto por accept.
			 */
			try {
				if (connection) {
					DataInputStream dis = new DataInputStream(socket.getInputStream());
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
	
					int intRecived = dis.readInt();
					System.out.println("Recived: " + intRecived);
					intRecived++;
					dos.writeInt(intRecived);
					System.out.println("Sent: " + intRecived);
	
	
					socket.close();
				}
				else {
					System.err.println("[-] Error accepting connection from client");
				}
			} catch (IOException e) {
				System.err.println("[-] Error during testTCPServer");
			}
			
		}
	}
	

	/**
	 * Método que ejecuta el hilo principal del servidor en segundo plano, esperando
	 * conexiones de clientes.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/*
		 * (Boletín SocketsTCP) Usar el socket servidor para esperar conexiones de
		 * otros peers que soliciten descargar ficheros
		 */
		
		while(true) {
			try {
				// Aceptar conexion de un cliente
				Socket socket = serverSocket.accept();

				// Iniciar hilo para atender al cliente
				NFServerThread serverThread = new NFServerThread(socket);
				serverThread.start();
				

			} catch (IOException e) {
				System.err.println("[-] Error accepting connection from client");
			}

		}
		
		/*
		 * (Boletín SocketsTCP) Al establecerse la conexión con un peer, la
		 * comunicación con dicho cliente se hace en el método
		 * serveFilesToClient(socket), al cual hay que pasarle el socket devuelto por
		 * accept
		 */
		
		
		/*
		 * (Boletín TCPConcurrente) Crear un hilo nuevo de la clase
		 * NFServerThread, que llevará a cabo la comunicación con el cliente que se
		 * acaba de conectar, mientras este hilo vuelve a quedar a la escucha de
		 * conexiones de nuevos clientes (para soportar múltiples clientes). Si este
		 * hilo es el que se encarga de atender al cliente conectado, no podremos tener
		 * más de un cliente conectado a este servidor.
		 */

		



	}
	/*
	 * TODO: (Boletín SocketsTCP) Añadir métodos a esta clase para: 1) Arrancar el
	 * servidor en un hilo nuevo que se ejecutará en segundo plano 2) Detener el
	 * servidor (stopserver) 3) Obtener el puerto de escucha del servidor etc.
	 */

	public void startServer() {
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"[fileServerTestMode] Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out
					.println("[fileServerTestMode] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}
		//NFServerThread serverThread = new NFServerThread(serverSocket);
		//serverThread.start();
	}


	public int getPort() {
		return serverSocket.getLocalPort();
	}

	public void stopServer() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("[-] Error closing server socket");
		}
	}



	/**
	 * Método de clase que implementa el extremo del servidor del protocolo de
	 * transferencia de ficheros entre pares.
	 * 
	 * @param socket El socket para la comunicación con un cliente que desea
	 *               descargar ficheros.
	 */
	public static void serveFilesToClient(Socket socket) {
		/*
		 *(Boletín SocketsTCP) Crear dis/dos a partir del socket
		 */
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException e) {
			System.err.println("[-] Error creating DataInputStream/DataOutputStream");
			return;
		}
		
		/*
		 * TODO: (Boletín SocketsTCP) Mientras el cliente esté conectado, leer mensajes
		 * de socket, convertirlo a un objeto PeerMessage y luego actuar en función del
		 * tipo de mensaje recibido, enviando los correspondientes mensajes de
		 * respuesta.
		 */
		PeerMessage peerMessage = null;

		
		/*
		 * TODO: (Boletín SocketsTCP) Para servir un fichero, hay que localizarlo a
		 * partir de su hash (o subcadena) en nuestra base de datos de ficheros
		 * compartidos. Los ficheros compartidos se pueden obtener con
		 * NanoFiles.db.getFiles(). Los métodos lookupHashSubstring y
		 * lookupFilenameSubstring de la clase FileInfo son útiles para buscar ficheros
		 * coincidentes con una subcadena dada del hash o del nombre del fichero. El
		 * método lookupFilePath() de FileDatabase devuelve la ruta al fichero a partir
		 * de su hash completo.
		 */



	}




}