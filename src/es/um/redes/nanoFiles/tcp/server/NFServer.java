package es.um.redes.nanoFiles.tcp.server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;
import es.um.redes.nanoFiles.util.FileInfo;




public class NFServer implements Runnable {

	public static final int PORT = 10000;

	private boolean stopServer = false;

	private ServerSocket serverSocket = null;

	public NFServer() throws IOException {
		/*
		 * (Boletín SocketsTCP) Crear una direción de socket a partir del puerto
		 * especificado (PORT)
		 */
		// Mejora implementada, ahora se ejecuta en un puerto cualquiera, eleguido por el sistema
		InetSocketAddress socketAddress = new InetSocketAddress(0);


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
				break;
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
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"[-] Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out
					.println("[*] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		
		while(!stopServer) {
			try {
				// Aceptar conexion de un cliente
				Socket socket = serverSocket.accept();

				// Iniciar hilo para atender al cliente
				NFServerThread serverThread = new NFServerThread(socket);
				serverThread.start();
				

			} catch (SocketException e) {
				//System.err.println("[*] Closing the socket...");
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
	 * (Boletín SocketsTCP) Añadir métodos a esta clase para: 1) Arrancar el
	 * servidor en un hilo nuevo que se ejecutará en segundo plano 2) Detener el
	 * servidor (stopserver) 3) Obtener el puerto de escucha del servidor etc.
	 */

	public void startServer() {
		Thread serverThread = new Thread(() -> {
			this.run();
		});
		serverThread.start();
	}


	public int getPort() {
		return serverSocket.getLocalPort();
	}

	public void stopServer() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		stopServer = true;
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
			return;
		}
		
		/*
		 * (Boletín SocketsTCP) Mientras el cliente esté conectado, leer mensajes
		 * de socket, convertirlo a un objeto PeerMessage y luego actuar en función del
		 * tipo de mensaje recibido, enviando los correspondientes mensajes de
		 * respuesta.
		 */
		PeerMessage recivedMessage = null;
		PeerMessage sendMessage = null;
		boolean finished = false;
		String fileToSend = null;
		while(!finished) {
			try {
				// Lecutra del mensaje del cliente 
				recivedMessage = PeerMessage.readMessageFromInputStream(dis);
			} catch (IOException e) {
				// En caso de error al enviar, se enviar OPCODE_ERROR
				//System.err.println("[-] Error reading message from input stream on [serveFilesToClient]");
				sendMessage = new PeerMessage(PeerMessageOps.OPCODE_ERROR);
				try {
					sendMessage.writeMessageToOutputStream(dos);
				} catch (IOException e1) {
					//System.err.println("[-] Error writing message to output stream on [serveFilesToClient]");
				}
				return;
			}
			
			// Actuar en función del tipo de mensaje recibido
			switch(recivedMessage.getOpcode()) {
				// Descargar corrupta por parte del cliente --> enviarmos OPCODE_ERROR
				case PeerMessageOps.OPCODE_CORRUPT_DOWNLOAD:
					finished = true;
					sendMessage = new PeerMessage(PeerMessageOps.OPCODE_ERROR);
					break;
				
				// El cliente solicita descargar un archivo
				case PeerMessageOps.OPCODE_DOWNLOAD_FILE:
					String substringName = recivedMessage.getSubstring();
					FileInfo[] files = FileInfo.lookupFilenameSubstring(NanoFiles.db.getFiles(),substringName);
					if (files.length == 0) {
						sendMessage = new PeerMessage(PeerMessageOps.OPCODE_NOT_FOUND);
						break;
					}
					else if (files.length > 1) {
						sendMessage = new PeerMessage(PeerMessageOps.OPCODE_AMBIGUOUS_NAME);
						break;
					}
					else {	// Caso de exito, se ha encontrado el fichero, se manda su hash y su tamaño

						String hash = FileDigest.computeFileChecksumString(files[0].getFilePath());
						double fileSize = (double) files[0].getFileSize();
						sendMessage = PeerMessage.PeerMessageDownloadAprove(hash,fileSize);
						fileToSend = files[0].getFilePath();
						break;
					}
				// Una vez el cliente sabe que fichero es, solicita los bytes de dicho fichero
				case PeerMessageOps.OPCODE_GET_CHUNCK:
					double fileOffset = recivedMessage.getFileOffset();
					int chunkSize = recivedMessage.getChunckSize();
					if (fileOffset == 0 && chunkSize == 0) {
						finished = true;
						break;
					}
					// Leectura de los bytes del fichero fileToSend
					byte[] data = readChunk(fileToSend, fileOffset, chunkSize);
					if (data == null) {
						sendMessage = new PeerMessage(PeerMessageOps.OPCODE_ERROR);
						finished = true;
						break;
					}
					sendMessage = PeerMessage.PeerMessageSendChunk(data);
					break;
				default:
					sendMessage = new PeerMessage(PeerMessageOps.OPCODE_INVALID_CODE);
					finished = true;
					break;
			}

			// Enviar el mensaje de respuesta al cliente
			try {
				if (sendMessage != null) {
					sendMessage.writeMessageToOutputStream(dos);
				}
			} catch (IOException e) {	// Si hay fallo al enviar se prueba a enviar OPCODE_ERROR
				//System.err.println("[-] Error writing message to output stream on [serveFilesToClient]");
				sendMessage = new PeerMessage(PeerMessageOps.OPCODE_ERROR);
				try {
					sendMessage.writeMessageToOutputStream(dos);
				} catch (IOException e1) {
					//System.err.println("[-] Error writing message to output stream on [serveFilesToClient]");
				}
				finished = true;
			}
		}
	}

	private static byte[] readChunk(String filePath, double _fileOffset, int _chunkSize) {
		byte[] data = new byte[_chunkSize];

		RandomAccessFile raf = null;

		try {
			raf = new RandomAccessFile(filePath, "r");
			long fileOffset = (long) _fileOffset;
			raf.seek(fileOffset);
			int readBytes = raf.read(data, 0, _chunkSize);

			// Si se han leído menos bytes de los esperados, redimensionar el array
			if (readBytes < _chunkSize) {
				byte[] newData = new byte[readBytes];
				System.arraycopy(data, 0, newData, 0, readBytes);
				data = newData;
			}

			
		} catch (IOException e) {
			//System.err.println("[-] Error reading chunk from file: " + e.getMessage());
			data = null;
			
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					//System.err.println("[-] Error closing file: " + e.getMessage());
				}
			}
		}
	
		return data;
	}




}