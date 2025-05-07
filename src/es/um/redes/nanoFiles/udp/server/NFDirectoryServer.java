package es.um.redes.nanoFiles.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFDirectoryServer {
	/**
	 * Número de puerto UDP en el que escucha el directorio
	 */
	public static final int DIRECTORY_PORT = 6868;

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	private DatagramSocket socket = null;
	/*
	 * Añadir aquí como atributos las estructuras de datos que sean necesarias
	 * para mantener en el directorio cualquier información necesaria para la
	 * funcionalidad del sistema nanoFilesP2P: ficheros publicados, servidores
	 * registrados, etc.
	 */
	/**
	 * Ficheros publicados en el directorio
	 */
	FileInfo[] filesDirectory = new FileInfo[0];

	/**
	 * Map que asocia el hash de un fichero con la lista de servidores que lo tienen
	 */
	HashMap<String, Set<InetSocketAddress>> serversByFile = new HashMap<String, Set<InetSocketAddress>>();
	/**
	 * Map que asocia cada host con sus ficheros
	 */
	HashMap<InetSocketAddress, Set<FileInfo>> filesByServer = new HashMap<>();

	/**
	 * Lista de servidores registrados en el directorio
	 */
	Set<InetSocketAddress> serversList = new HashSet<InetSocketAddress>();

	private void addServersFile(InetSocketAddress server, FileInfo[] files) {
		for (FileInfo file : files) {
			if (!serversByFile.containsKey(file.getFileHash())) {
				serversByFile.put(file.getFileHash(), new HashSet<InetSocketAddress>());
			}
			serversByFile.get(file.getFileHash()).add(server);
		}
	}

	private void addFilesServer(InetSocketAddress server, FileInfo[] files){
		// Si el servidor ya tiene ficheros subidos se actualizan
		if (filesByServer.containsKey(server)){
			filesByServer.get(server).addAll(Arrays.asList(files));
		} else{	// Si el servidor no tenia ficheros publicados se crea la entrada en el HashMap
			Set<FileInfo> fileSet = new HashSet<>(Arrays.asList(files));
			filesByServer.put(server, fileSet);
		}
	}

	private void updateFilesDirectory(){
		ArrayList<FileInfo> newFilesDirectory = new ArrayList<>();

		for (Set<FileInfo> fileSet : filesByServer.values()) {
			// Agregar todos los FileInfo del conjunto a la lista
			newFilesDirectory.addAll(fileSet);
		}

		// Se actualiza filesDirectory
		filesDirectory = newFilesDirectory.toArray(new FileInfo[0]);
	}

	



	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	private double messageDiscardProbability;

	public NFDirectoryServer(double corruptionProbability) throws SocketException {
		/*
		 * Guardar la probabilidad de pérdida de datagramas (simular enlace no
		 * confiable)
		 */
		messageDiscardProbability = corruptionProbability;
		/*
		 * (Boletín SocketsUDP) Inicializar el atributo socket: Crear un socket
		 * UDP ligado al puerto especificado por el argumento directoryPort en la
		 * máquina local,
		 */
		
		socket = new DatagramSocket(DIRECTORY_PORT);
		
		
		/*
		 * (Boletín SocketsUDP) Inicializar atributos que mantienen el estado del
		 * servidor de directorio: ficheros, etc.)
		 */
		
			


		if (NanoFiles.testModeUDP) {
			if (socket == null) {
				System.err.println("[testMode] NFDirectoryServer: code not yet fully functional.\n"
						+ "Check that all TODOs in its constructor and 'run' methods have been correctly addressed!");
				System.exit(-1);
			}
		}
	}

	public DatagramPacket receiveDatagram() throws IOException {
		DatagramPacket datagramReceivedFromClient = null;
		boolean datagramReceived = false;
		while (!datagramReceived) {
			/*
			 * (Boletín SocketsUDP) Crear un búfer para recibir datagramas y un
			 * datagrama asociado al búfer (datagramReceivedFromClient)
			 */
			
			byte[] responseBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
			datagramReceivedFromClient = new DatagramPacket(responseBuffer,responseBuffer.length);
			
			
			/*
			 * (Boletín SocketsUDP) Recibimos a través del socket un datagrama
			 */
			socket.receive(datagramReceivedFromClient);



			if (datagramReceivedFromClient == null) {
				System.err.println("[testMode] NFDirectoryServer.receiveDatagram: code not yet fully functional.\n"
						+ "Check that all TODOs have been correctly addressed!");
				System.exit(-1);
			} else {
				// Vemos si el mensaje debe ser ignorado (simulación de un canal no confiable)
				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println(
							"Directory ignored datagram from " + datagramReceivedFromClient.getSocketAddress());
				} else {
					datagramReceived = true;
					System.out
							.println("Directory received datagram from " + datagramReceivedFromClient.getSocketAddress()
									+ " of size " + datagramReceivedFromClient.getLength() + " bytes.");
				}
			}

		}

		return datagramReceivedFromClient;
	}

	public void runTest() throws IOException {

		System.out.println("[testMode] Directory starting...");

		System.out.println("[testMode] Attempting to receive 'ping' message...");
		DatagramPacket rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);

		System.out.println("[testMode] Attempting to receive 'ping&PROTOCOL_ID' message...");
		rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);
	}

	private void sendResponseTestMode(DatagramPacket pkt) throws IOException {
		/*
		 * (Boletín SocketsUDP) Construir un String partir de los datos recibidos
		 * en el datagrama pkt. A continuación, imprimir por pantalla dicha cadena a
		 * modo de depuración.
		 */
		String recivedMessage = new String(pkt.getData(),0,pkt.getLength());
		//System.out.println(recivedMessage);

		/*
		 * (Boletín SocketsUDP) Después, usar la cadena para comprobar que su
		 * valor es "ping"; en ese caso, enviar como respuesta un datagrama con la
		 * cadena "pingok". Si el mensaje recibido no es "ping", se informa del error y
		 * se envía "invalid" como respuesta.
		 */
		InetSocketAddress clientAddres = (InetSocketAddress) pkt.getSocketAddress();
		String messageToClient;
		if(recivedMessage.equals("ping")) {
			messageToClient = new String("pingok");
		}
		else if(recivedMessage.startsWith("ping&")){
			if(recivedMessage.endsWith(NanoFiles.PROTOCOL_ID)){
				messageToClient = new String("welcome");
			}
			else {
				messageToClient = new String("denied");
			}
		}
		else {
			System.err.println("[testMode] Error: El mesnaje recibido no es ping - Invalid");
			messageToClient = new String("invalid");
		}
		byte[] dataToClient = messageToClient.getBytes();
		DatagramPacket packetToClient = new DatagramPacket(dataToClient, dataToClient.length, clientAddres);
		socket.send(packetToClient);
		

		/*
		 * (Boletín Estructura-NanoFiles) Ampliar el código para que, en el caso
		 * de que la cadena recibida no sea exactamente "ping", comprobar si comienza
		 * por "ping&" (es del tipo "ping&PROTOCOL_ID", donde PROTOCOL_ID será el
		 * identificador del protocolo diseñado por el grupo de prácticas (ver
		 * NanoFiles.PROTOCOL_ID). Se debe extraer el "protocol_id" de la cadena
		 * recibida y comprobar que su valor coincide con el de NanoFiles.PROTOCOL_ID,
		 * en cuyo caso se responderá con "welcome" (en otro caso, "denied").
		 */

		String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: " + messageFromClient);
	}

	public void run() throws IOException {

		System.out.println("Directory starting...");

		while (true) { // Bucle principal del servidor de directorio
			DatagramPacket rcvDatagram = receiveDatagram();

			sendResponse(rcvDatagram);

		}
	}

	private void sendResponse(DatagramPacket pkt) throws IOException {
		/*
		 * (Boletín MensajesASCII) Construir String partir de los datos recibidos
		 * en el datagrama pkt. A continuación, imprimir por pantalla dicha cadena a
		 * modo de depuración. Después, usar la cadena para construir un objeto
		 * DirMessage que contenga en sus atributos los valores del mensaje. A partir de
		 * este objeto, se podrá obtener los valores de los campos del mensaje mediante
		 * métodos "getter" para procesar el mensaje y consultar/modificar el estado del
		 * servidor.
		 */
		String recvivedData = new String(pkt.getData(),0,pkt.getLength());
		DirMessage recivedMessage = DirMessage.fromString(recvivedData);



		/*
		 * Una vez construido un objeto DirMessage con el contenido del datagrama
		 * recibido, obtener el tipo de operación solicitada por el mensaje y actuar en
		 * consecuencia, enviando uno u otro tipo de mensaje en respuesta.
		 */
		String operation = recivedMessage.getOperation();

		/*
		 * (Boletín MensajesASCII) Construir un objeto DirMessage (msgToSend) con
		 * la respuesta a enviar al cliente, en función del tipo de mensaje recibido,
		 * leyendo/modificando según sea necesario el "estado" guardado en el servidor
		 * de directorio (atributos files, etc.). Los atributos del objeto DirMessage
		 * contendrán los valores adecuados para los diferentes campos del mensaje a
		 * enviar como respuesta (operation, etc.)
		 */

		
		DirMessage msgToSend = null;
		

		switch (operation) {
		// Proccess ping
		case DirMessageOps.OPERATION_PING: {
			boolean success = false;

			/*
			 * (Boletín MensajesASCII) Comprobamos si el protocolId del mensaje del
			 * cliente coincide con el nuestro.
			 */
			if (recivedMessage.getProtocolId().equals(NanoFiles.PROTOCOL_ID)) {
				msgToSend = new DirMessage(DirMessageOps.OPERATION_PINGOK);
				success = true;
			}
			else {
				msgToSend = new DirMessage(DirMessageOps.OPERATION_INVALID);
			}

			/*
			 * (Boletín MensajesASCII) Construimos un mensaje de respuesta que indique
			 * el éxito/fracaso del ping (compatible, incompatible), y lo devolvemos como
			 * resultado del método.
			 */
			/*
			 * (Boletín MensajesASCII) Imprimimos por pantalla el resultado de
			 * procesar la petición recibida (éxito o fracaso) con los datos relevantes, a
			 * modo de depuración en el servidor
			 */
			if (success) {
				System.out.println("[+] Login success");
				System.out.println("[+] ID: "+ NanoFiles.PROTOCOL_ID);
			}
			else {
				System.err.println("[-] Login invalid");
				System.err.println("[-] ID: "+ NanoFiles.PROTOCOL_ID);
			}
			break;
		}

		// Proccess request_file_list
		case DirMessageOps.OPERATION_REQEST_FILELIST: {

			try {
				msgToSend = DirMessage.DirMessageRequestFileListOk(DirMessageOps.OPERATION_REQEST_FILELIST_OK, filesDirectory);
				System.out.println("[+] SUCCESS on "+DirMessageOps.OPERATION_REQEST_FILELIST);
				System.out.println("[+] Sending files info...");
			} catch (Exception e) {
				msgToSend = new DirMessage(DirMessageOps.OPERATION_REQEST_FILELIST_FAIL);
				System.err.println("[-] Error by "+DirMessageOps.OPERATION_REQEST_FILELIST);
				System.err.println("[-] Sending response... "+DirMessageOps.OPERATION_REQEST_FILELIST_FAIL);
			}

			break;
		}

		// Proccess publish_files
		case DirMessageOps.OPERATION_PUBLISH_FILES: {
			try {	

				InetSocketAddress servAddress = new InetSocketAddress(pkt.getAddress(), recivedMessage.getPort());

				// Se añade el servidor a la lista de servidores registrados
				serversList.add(servAddress);
				
				// Ficheros que se desean publicar
				FileInfo[] newFiles = recivedMessage.getFilesInfo();

				// Actualizar el map de servidores por fichero
				addServersFile(new InetSocketAddress(pkt.getAddress(), recivedMessage.getPort()), newFiles);

				// Actualizar el map de ficheros por servidor
				addFilesServer(servAddress,newFiles);

				// Actualizar el filesDirectory
				updateFilesDirectory();

				msgToSend = new DirMessage(DirMessageOps.OPERATION_PUBLISH_FILES_OK);
				System.out.println("[+] SUCCESS on "+DirMessageOps.OPERATION_PUBLISH_FILES);
				System.out.println("[+] Posting files into the server...");
				System.out.println("[+] Files posted: ");
				FileInfo.printToSysout(newFiles);
			} catch (Exception e) {
				msgToSend = new DirMessage(DirMessageOps.OPERATION_PUBLISH_FILES_FAIL);
				System.err.println("[-] Error by "+DirMessageOps.OPERATION_PUBLISH_FILES);
				System.err.println("[-] Sending response... "+DirMessageOps.OPERATION_PUBLISH_FILES_FAIL);
			}

			break;
		}
		// Unregister server file
		case DirMessageOps.OPERATION_UNREGISTER_SERVER: {
			try {
				// Obtener la dirección del servidor que envió el paquete
				InetSocketAddress serverAddress = new InetSocketAddress(pkt.getAddress(), recivedMessage.getPort());

				// Eliminamos al host de la lista de servidores
				serversList.remove(serverAddress);
		
				// Actualizacion de serversByFile
				// Usar un iterador para recorrer el mapa y eliminar entradas de manera segura
				Iterator<Map.Entry<String, Set<InetSocketAddress>>> iterator = serversByFile.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Set<InetSocketAddress>> entry = iterator.next();
					Set<InetSocketAddress> serverSet = entry.getValue();

					if (serverSet.contains(serverAddress)) {
						// Eliminar el servidor del conjunto
						serverSet.remove(serverAddress);

						// Si el conjunto queda vacío, eliminar la entrada del mapa
						if (serverSet.isEmpty()) {
							iterator.remove();
						}
					}
				}

				// Actualizacoin de filesByServer
				filesByServer.remove(serverAddress);

		
				// Actualizar filesDirectory eliminando los archivos desregistrados
				updateFilesDirectory();
		
				// Enviar respuesta de éxito
				msgToSend = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_SERVER_OK);
				System.out.println("[+] SUCCESS on " + DirMessageOps.OPERATION_UNREGISTER_SERVER);
				System.out.println("[+] Files unregistered for server: " + serverAddress);
			} catch (Exception e) {
				e.printStackTrace();
				// Enviar respuesta de error
				msgToSend = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_SERVER_FAIL);
				System.err.println("[-] Error by " + DirMessageOps.OPERATION_UNREGISTER_SERVER);
				System.err.println("[-] Sending response... " + DirMessageOps.OPERATION_UNREGISTER_SERVER_FAIL);
			}
			break;
		}


		// Proccess request_servers_list
		case DirMessageOps.OPERATION_REQUEST_SERVERS_LIST: {

			// Seleccionar el hash del fichero a buscar
			String fileNameSubString = recivedMessage.getFileNameSubstring();
			String hashTarget = null;
			int contMatches = 0;
			for (FileInfo file : filesDirectory) {
				if (file.getFileName().contains(fileNameSubString)) {
					String actualHash = file.getFileHash();
					if(hashTarget==null) {
						hashTarget = actualHash;
						contMatches++;
					}
					if(hashTarget!=null && actualHash!=hashTarget){
						contMatches++;
					}
				}
			}
			// No se encuenta el fichero
			if (hashTarget == null) {
				msgToSend = new DirMessage(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL);
				System.err.println("[-] Error by "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST);
				System.err.println("[-] No file found with the substring: "+fileNameSubString);
				System.err.println("[-] Sending response... "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL);
			} else if(contMatches>1){
				try {
					msgToSend = new DirMessage(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_AMBIGUOUS);
					System.err.println("[-] Error by "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST);
					System.err.println("[-] Ambiguous filename substring. Sending response: "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_AMBIGUOUS);
				} catch (Exception e) {
					msgToSend = new DirMessage(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL);
					System.err.println("[-] Error by "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST);
					System.err.println("[-] Sending response... "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL);				
				}
			}else {
				try {
					Set<InetSocketAddress> serversSet = serversByFile.get(hashTarget);
					InetSocketAddress[] servers = serversSet.toArray(new InetSocketAddress[0]);
					msgToSend = DirMessage.DirMessageRequestServersListOk(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_OK, servers);
					System.out.println("[+] SUCCESS on "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST);
					System.out.println("[+] Sending servers list...");
				} catch (Exception e) {
					msgToSend = new DirMessage(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL);
					System.err.println("[-] Error by "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST);
					System.err.println("[-] Sending response... "+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL);				
				}
			}

			break;

		}



		default:
			System.err.println("Unexpected message operation: \"" + operation + "\"");
			System.exit(-1);
		}

		/*
		 * (Boletín MensajesASCII) Convertir a String el objeto DirMessage
		 * (msgToSend) con el mensaje de respuesta a enviar, extraer los bytes en que se
		 * codifica el string y finalmente enviarlos en un datagrama
		 */

		assert (msgToSend != null);
		InetSocketAddress clientAddres = (InetSocketAddress) pkt.getSocketAddress();

		byte[] dataToClient = msgToSend.toString().getBytes();
		DatagramPacket pktToClient = new DatagramPacket(dataToClient, dataToClient.length, clientAddres);
		socket.send(pktToClient);

	}
}
