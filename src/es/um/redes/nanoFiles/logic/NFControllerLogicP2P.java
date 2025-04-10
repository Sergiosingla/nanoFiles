package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;
import java.io.File;
import java.io.IOException;
import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.application.NanoFiles;



import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.tcp.server.NFServerThread;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	/*
	 * Se necesita un atributo NFServer que actuará como servidor de ficheros
	 * de este peer
	 */
	private NFServer fileServer = null;




	protected NFControllerLogicP2P() {
	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor en un nuevo hilo creado a tal efecto.
	 * 
	 * @return Verdadero si se ha arrancado en un nuevo hilo con el servidor de
	 *         ficheros, y está a la escucha en un puerto, falso en caso contrario.
	 * 
	 */
	protected boolean startFileServer() {
		boolean serverRunning = false;
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		if (fileServer != null) {
			System.err.println("File server is already running");
		} else {

			/*
			 * (Boletín Servidor TCP concurrente) Arrancar servidor en segundo plano
			 * creando un nuevo hilo, comprobar que el servidor está escuchando en un puerto
			 * válido (>0), imprimir mensaje informando sobre el puerto de escucha, y
			 * devolver verdadero. Las excepciones que puedan lanzarse deben ser capturadas
			 * y tratadas en este método. Si se produce una excepción de entrada/salida
			 * (error del que no es posible recuperarse), se debe informar sin abortar el
			 * programa
			 * 
			 */
			try {
				fileServer = new NFServer();
				
				Thread serverThread = new Thread(() -> {
					fileServer.run(); // Ejecutar el método run del servidor
				});
				serverThread.start();
				int port = this.getServerPort();
				if (port > 0) {
					System.out.println("[+] NFServer running on " + this.getServerPort() + ".");
					serverRunning = true;
				} else {
					System.err.println("[-] Error: Failed to run file server, server socket is null or not bound to any port");
				}

			}
			catch (IOException e) {
				e.printStackTrace();
				System.err.println("[-] Error: Cannot start the file server");
				fileServer = null;;
			}
			
		}
		return serverRunning;

	}

	protected void testTCPServer() {
		assert (NanoFiles.testModeTCP);
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		assert (fileServer == null);
		try {

			fileServer = new NFServer();
			/*
			 * (Boletín SocketsTCP) Inicialmente, se creará un NFServer y se ejecutará su
			 * método "test" (servidor minimalista en primer plano, que sólo puede atender a
			 * un cliente conectado). Posteriormente, se desactivará "testModeTCP" para
			 * implementar un servidor en segundo plano, que se ejecute en un hilo
			 * secundario para permitir que este hilo (principal) siga procesando comandos
			 * introducidos mediante el shell.
			 */
			fileServer.test();
			// Este código es inalcanzable: el método 'test' nunca retorna...
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("Cannot start the file server");
			fileServer = null;
		}
	}

	public void testTCPClient() {

		assert (NanoFiles.testModeTCP);
		/*
		 * (Boletín SocketsTCP) Inicialmente, se creará un NFConnector (cliente TCP)
		 * para conectarse a un servidor que esté escuchando en la misma máquina y un
		 * puerto fijo. Después, se ejecutará el método "test" para comprobar la
		 * comunicación mediante el socket TCP. Posteriormente, se desactivará
		 * "testModeTCP" para implementar la descarga de un fichero desde múltiples
		 * servidores.
		 */

		try {
			NFConnector nfConnector = new NFConnector(new InetSocketAddress(NFServer.PORT));
			nfConnector.test();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros
	 * 
	 * @param serverAddressList       La lista de direcciones de los servidores a
	 *                                los que se conectará
	 * @param targetFileNameSubstring Subcadena del nombre del fichero a descargar
	 * @param localFileName           Nombre con el que se guardará el fichero
	 *                                descargado
	 */
	protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, String targetFileNameSubstring,
			String localFileName) {
		boolean downloaded = false;

		if (serverAddressList.length == 0) {
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}
		/*
		 * TODO: Crear un objeto NFConnector distinto para establecer una conexión TCP
		 * con cada servidor de ficheros proporcionado, y usar dicho objeto para
		 * descargar trozos (chunks) del fichero. Se debe comprobar previamente si ya
		 * existe un fichero con el mismo nombre (localFileName) en esta máquina, en
		 * cuyo caso se informa y no se realiza la descarga. Se debe asegurar que el
		 * fichero cuyos datos se solicitan es el mismo para todos los servidores
		 * involucrados (el fichero está identificado por su hash). Una vez descargado,
		 * se debe comprobar la integridad del mismo calculando el hash mediante
		 * FileDigest.computeFileChecksumString. Si todo va bien, imprimir resumen de la
		 * descarga informando de los trozos obtenidos de cada servidor involucrado. Las
		 * excepciones que puedan lanzarse deben ser capturadas y tratadas en este
		 * método. Si se produce una excepción de entrada/salida (error del que no es
		 * posible recuperarse), se debe informar sin abortar el programa
		 */
		NFConnector[] nfConnectors = new NFConnector[serverAddressList.length];
		for (int i = 0; i < serverAddressList.length; i++) {
			try {
				System.out.println("[+] Connecting to server " + serverAddressList[i].getHostString() + ":" + serverAddressList[i].getPort());
				nfConnectors[i] = new NFConnector(new InetSocketAddress(serverAddressList[i].getAddress(), serverAddressList[i].getPort()));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("[-] Error: Cannot connect to server " + serverAddressList[i].getHostString()+ ":" + serverAddressList[i].getPort());
				return downloaded;
			}
		}
		File localFile = new File(localFileName);
		if (localFile.exists()) {
			System.err.println("[-] Error: File " + localFileName + " already exists");
			return downloaded;
		}


		



		return downloaded;
	}

	/**
	 * Método para obtener el puerto de escucha de nuestro servidor de ficheros
	 * 
	 * @return El puerto en el que escucha el servidor, o 0 en caso de error.
	 */
	protected int getServerPort() {
		int port = 0;
		/*
		 * Devolver el puerto de escucha de nuestro servidor de ficheros
		 */
		port = fileServer.getPort();
		return port;
	}

	/**
	 * Método para detener nuestro servidor de ficheros en segundo plano
	 * 
	 */
	protected void stopFileServer() {
		/*
		 * Enviar señal para detener nuestro servidor de ficheros en segundo plano
		 */
		if (fileServer != null) {
			fileServer.stopServer();
			fileServer = null;
		} else {
			System.err.println("File server is not running");
		}



	}

	protected boolean serving() {
		boolean result = false;



		return result;

	}

	protected boolean uploadFileToServer(FileInfo matchingFile, String uploadToServer) {
		boolean result = false;



		return result;
	}

}
