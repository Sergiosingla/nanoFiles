package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.application.NanoFiles;



import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.util.FileDigest;
import es.um.redes.nanoFiles.util.FileInfo;

/*
 * Clase auxiliar para poder controlar la descarga concurrente
 * 
 * Cuando uno de los hilos durante la descarga tiene algun problema este lanza una DownloadException para poder aborar la descarga y comunicarlo
 * al resto de hilos
 */
class DownloadException extends Exception {}

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
				
				fileServer.startServer();
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
	private final Object lock = new Object();		// Objecto para la sincronizacion de los hilos concurrentess
	private volatile boolean downloadFail = false;	// Variable para controlar los errores durante la descarga

	protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, String targetFileNameSubstring,
			String localFileName) {
		
		boolean downloaded = false;

		if(serverAddressList==null){	// Ambigous filenameSubstring case
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}

		if (serverAddressList.length == 0) {
			System.err.println("[-] No file currently being served matches the specified file name \""+targetFileNameSubstring+"\".");
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}
		/*
		 * Crear un objeto NFConnector distinto para establecer una conexión TCP
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

		// Lista de NFConnector para la descarga
		ArrayList<NFConnector> nfConnectors = new ArrayList<>();
		for (InetSocketAddress serverAddress : serverAddressList) {
			try {
				// Obtener la dirección IP o el nombre del host
				String host = serverAddress.getHostString();

				// Validar que el host no sea nulo o vacío
				if (host == null || host.isEmpty()) {
					throw new IllegalArgumentException("Invalid host: " + serverAddress);
				}

				// Crear el NFConnector con la dirección y el puerto
				System.out.println("[+] Connecting to server " + serverAddress);
				NFConnector connector = new NFConnector(serverAddress);
				nfConnectors.add(connector);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("[-] Error: Cannot connect to server " + serverAddress.getHostString() + ":" + serverAddress.getPort());
			}
		}

		// Creacion de la carpeta para descargas y archivo a descargar
		File theDir = new File(NanoFiles.DEFAULT_DOWNLOADS_DIRNAME);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		// Fichero que se va descargar
		File localFile = new File(theDir, localFileName);


		if (localFile.exists()) {
			System.err.println("[-] Error: File \"" + localFileName + "\" already exists");
			return downloaded;
		}
		else {
			try {
				if (localFile.createNewFile()) {
					System.out.println("[*] Creating file " + localFile );
				} else {
					System.err.println("[-] Error: Could not create file " + localFile.getAbsolutePath());
					return downloaded;
				}
			} catch (IOException e) {
				System.err.println("[-] Error creating file: " + e.getMessage());
				return downloaded;
			}
		}

		// Solicitud de descarga del fichero targetFileNameSubstring
		PeerMessage msgDownloadFile = PeerMessage.PeerMessageDownloadFile(targetFileNameSubstring);
		PeerMessage msgRecivedFromPeer;

		// Hash y tamaño del archivo a descargar
		String expectedFileHash = null;
		double expectedFileSize = -1;

		// Depuracion de la lista de host que sirven el fichero, se eliminar aquellos que no estan disponibles
		Iterator<NFConnector> it = nfConnectors.iterator();
		while(it.hasNext()){
			NFConnector connector = it.next();
			msgRecivedFromPeer = connector.sendAndRecive(msgDownloadFile);

			if (msgRecivedFromPeer != null) {
				// Si la solicitud de descarga para este nfconnector no es aprobada los quitamos de la lista
			if (msgRecivedFromPeer.getOpcode()!=PeerMessageOps.OPCODE_DOWNLOAD_APROVE) {
				switch (msgRecivedFromPeer.getOpcode()) {
					case PeerMessageOps.OPCODE_NOT_FOUND: {
						System.err.println("\t[-] Host "+connector.getServerAddr()+" did not found the file. Removing from hosts list");
						it.remove();
						break;
					}
					case PeerMessageOps.OPCODE_AMBIGUOUS_NAME: {
						System.err.println("\t[-] Host "+connector.getServerAddr()+" has more than one file that matches "+targetFileNameSubstring+". Removing from hosts list");
						it.remove();
						break;
					}
					default:{
						System.out.println("\t[-] Host "+connector.getServerAddr()+" did not aprove download. Removing from hosts list");
						it.remove();
						break;
					}
				}
			}
			else{
				// Si es la primera aprobación, guardamos el hash y el tamaño esperados
				if (expectedFileHash == null && expectedFileSize == -1) {
					expectedFileHash = msgRecivedFromPeer.getHashCode();
					expectedFileSize = msgRecivedFromPeer.getFileSize();
					System.out.println("[+] Host " + connector.getServerAddr() + " approved download with consistent data.");
				} else {
					// Verificar si el hash o el tamaño no coinciden
					if (!expectedFileHash.equals(msgRecivedFromPeer.getHashCode()) || expectedFileSize != msgRecivedFromPeer.getFileSize()) {
						System.err.println("\t[-] Host " + connector.getServerAddr() + " returned inconsistent file data (Hash/Size mismatch). Removing from hosts list");
						it.remove();
					} else {
						System.out.println("[+] Host " + connector.getServerAddr() + " approved download with consistent data.");
					}
				}
			}
			} else {
				System.err.println("[-] Host has not response. Removing from hosts list.");
				it.remove();
				localFile.delete();
        		return downloaded;
			}			
		}
		// Si no hay hosts disponibles abortamos
		if (nfConnectors.isEmpty()) {
			System.err.println("[-] No hosts available for download. Aborting...");
			localFile.delete();
        	return downloaded;
		}

		System.out.println("[*] Stating download from "+nfConnectors.size()+ " hosts....");

		/* LOGICA DE DESCARGA */

		int numHosts = nfConnectors.size();
		int defChunkSize = NanoFiles.DEFAULT_CHUNK_SIZE;			// Tamaño default de un chunk
		int numChunks = (int) (expectedFileSize/defChunkSize);		// Numero de chunks a falta de comprobar si hace falta uno que no del mismo tamaño (lastChunk)
		int lastChunkSize = (int) (expectedFileSize%defChunkSize);	// Tamaño del utlimo chunk, en caso de que el ultimo chunk no tenga el mismo tamaño que el resto

		// Array de hilos de descarga (descarga en paralelo)
		Thread[] downloadThreads = new Thread[numHosts];
		
		// Contador de numero de chunks por host
		int[] hostChunkCount = new int[numHosts];

		try(RandomAccessFile raf = new RandomAccessFile(localFile, "rw")){

			int totalChunks = (lastChunkSize>0) ? numChunks+1 : numChunks;

			boolean[] chunksDownloaded = new boolean[totalChunks];		// Array para saber si cierto chunks se ha descargado o no
			
			for (int i=0; i<numHosts; i++){
				final int hostIndex = i;
				downloadThreads[i] = new Thread (() -> {
					try {
						for(int chunkIndex=hostIndex;chunkIndex<totalChunks; chunkIndex += numHosts) {
							// Comprobar que no ha habido ningun error durante la descarga en otro hilo
							if(downloadFail){
								throw new DownloadException();
							}

							// Comprobar que el chunk no ha sido descargado ya
							synchronized (lock) {
								if(chunksDownloaded[chunkIndex]){
									continue;
								}
								// Si no se ha descargado lo marcamos para que nadie mas lo pueda descargar
								chunksDownloaded[chunkIndex] = true;
							}

				
							NFConnector downloadConnector = nfConnectors.get(hostIndex);
							// Calculo del offset y chunkSize actual
							long fileOffset = chunkIndex * defChunkSize;
							int localChunkSize = (chunkIndex == numChunks) ? lastChunkSize : defChunkSize;
							// Solicitud del chunk
							PeerMessage msgGetChunk = PeerMessage.PeerMessageGetChunck(fileOffset,localChunkSize);
							PeerMessage msgChunkResponse = downloadConnector.sendAndRecive(msgGetChunk);

							// Si hay un corte en la conexion, no se recibe mensaje por lo que se debe salir de manera controlada
							if(msgChunkResponse==null) {
								System.err.println("[-] Failed to download, the conexion has been closed or the host have not replied. Aborting download.... ");
								throw new DownloadException();
							}

							if (msgChunkResponse.getOpcode()==PeerMessageOps.OPCODE_SEND_CHUNK){
								byte[] chunckData = msgChunkResponse.getChunckData();
								
								synchronized(raf){
									// Esciribir en el fichero
									//System.out.println("OFFSET: "+fileOffset+" - SIZE: "+chunckData.length);  //! DEPURADO
									raf.seek(fileOffset);
									raf.write(chunckData);
								}
								hostChunkCount[hostIndex]++;
								
							} else {
								System.err.println("[-] Failed to download chunk " + chunkIndex + " from host " + downloadConnector.getServerAddr());
								throw new DownloadException();
							}

						}
					}catch(DownloadException e) {
						// Si alguna hilo falla durante la descarga debe informar a los otros que no sigan
						downloadFail = true;
						return;
					}catch(IOException e) {
						System.err.println("[-] Error writing to local file: " + e.getMessage());
						downloadFail = true;
						return;
					}
				});
				downloadThreads[i].start();
				//System.out.println("START THREAD: "+i);	// ! DEPURADO
			}

			// Punto de reunion de los hilos
			for (Thread thread : downloadThreads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					System.err.println("[-] Thread interrupted: " + e.getMessage());
				}
			}
			// Si ha fallado la descarga borramos el fichero que se creo y devolvemos false
			if(downloadFail){
				System.err.println("[-] Download canceled due to errors in one or more threads.");
				localFile.delete();
				return false;
			}

			// Una vez finalizada la descarga avisamos al servidor que ya hemos terminado
			for(NFConnector connector : nfConnectors){
				PeerMessage msgGetChunk = PeerMessage.PeerMessageGetChunck(0,0);	// Mensaje que indica el final
				connector.sendAndRecive(msgGetChunk);	// Solo nos interesa decirle a cada server que hemos terminado
			}
			
			// Comprobacion del nuevo hash para que coincida
			// Si el hash es diferente ha habido alguna mutacion
			if (!FileDigest.computeFileChecksumString(localFile.getAbsolutePath()).equals(expectedFileHash)){
				System.err.println("[-] Error: File integrity check failed. The downloaded file is corrupt. The computed hash does not match the expected hash.");
				localFile.delete();
				return false;
			}

			downloaded = true;	// Se ha completado la descarga con exito
			System.out.println("[*] File download successfully.");
			System.out.println("[*] Summary:");
			printSummary(nfConnectors,hostChunkCount,totalChunks);

		}catch(IOException e){
			System.err.println("[-] Error writing to local file: " + e.getMessage());
			localFile.delete();
			return false;
		}catch(NullPointerException e) {
			localFile.delete();
			return false;
		}
		return downloaded;
	}


	private void printSummary(ArrayList<NFConnector> connectors, int[] summary, int totalChunks){
		for (int i=0; i<summary.length; i++) {
			double percentage = Math.floor(((double)summary[i]/(double)totalChunks)*100);
			System.out.println("\t Host "+connectors.get(i).getServerAddr()+" downloaded "+percentage+"% ("+summary[i]+" chunks)" );
		}
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
		return (fileServer==null) ? false : true;
	}

	protected boolean uploadFileToServer(FileInfo matchingFile, String uploadToServer) {
		boolean result = false;



		return result;
	}

}
