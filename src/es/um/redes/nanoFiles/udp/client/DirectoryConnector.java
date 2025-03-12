package es.um.redes.nanoFiles.udp.client;

import java.awt.image.ImagingOpException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;
	/**
	 * Nombre/IP del host donde se ejecuta el directorio
	 */
	private String directoryHostname;


	/**
	 * Variable para controlar el login
	 */
	private boolean alreadyLogin = false;


	public DirectoryConnector(String hostname) throws IOException {
		// Guardamos el string con el nombre/IP del host
		directoryHostname = hostname;
		/*
		 * (Boletín SocketsUDP) Convertir el string 'hostname' a InetAddress y
		 * guardar la dirección de socket (address:DIRECTORY_PORT) del directorio en el
		 * atributo directoryAddress, para poder enviar datagramas a dicho destino.
		 */
		InetAddress serverIP = InetAddress.getByName(directoryHostname);
		directoryAddress = new InetSocketAddress(serverIP,DIRECTORY_PORT);
		
		
		
		/*
		 * (Boletín SocketsUDP) Crea el socket UDP en cualquier puerto para enviar
		 * datagramas al directorio
		 */
		
		socket = new DatagramSocket();
		
		// Se establece un timeout al socket
		socket.setSoTimeout(TIMEOUT);


	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
		/*
		 * (Boletín SocketsUDP) Enviar datos en un datagrama al directorio y
		 * recibir una respuesta. El array devuelto debe contener únicamente los datos
		 * recibidos, *NO* el búfer de recepción al completo.
		 */
		DatagramPacket packetToServer = new DatagramPacket(requestData, requestData.length, directoryAddress);
		DatagramPacket packetFromServer = new DatagramPacket(responseData, responseData.length);
		
		boolean successfulReceive = false;
		int attempts = 0;
		int maxRetries = 5;
		while(!successfulReceive && attempts < maxRetries) {
			try {
				socket.send(packetToServer);
			} catch (IOException e) {
				System.err.println("IOException al enviar paquete.");
				System.exit(-1);
			}
			
			/*
			 * (Boletín SocketsUDP) Una vez el envío y recepción asumiendo un canal
			 * confiable (sin pérdidas) esté terminado y probado, debe implementarse un
			 * mecanismo de retransmisión usando temporizador, en caso de que no se reciba
			 * respuesta en el plazo de TIMEOUT. En caso de salte el timeout, se debe volver
			 * a enviar el datagrama y tratar de recibir respuestas, reintentando como
			 * máximo en MAX_NUMBER_OF_ATTEMPTS ocasiones.
			 */
			
			
			try {
				socket.receive(packetFromServer);
				successfulReceive = true;
				
			// Se captura el timeout y se maneja
			} catch (SocketTimeoutException e) {
				System.err.println("[*] Se alcanzo el timeout...");
				System.err.println("[*] Reenviando paquete...");
				
				// Aumentamos el contador de intentos
				attempts++;
			} catch (IOException e) {
				System.err.println("IOException al recibir paquete.");
				System.exit(-1);
			}
		}
		if(!successfulReceive) {
			System.err.println("[*] Error: No se pudo recibir el PING. Abortando...");
			System.exit(-1);
		}
		
		
		/*
		 * (Boletín SocketsUDP) Las excepciones que puedan lanzarse al
		 * leer/escribir en el socket deben ser capturadas y tratadas en este método. Si
		 * se produce una excepción de entrada/salida (error del que no es posible
		 * recuperarse), se debe informar y terminar el programa.
		 */
		response = Arrays.copyOf(responseData, packetFromServer.getLength());
		
		/*
		 * NOTA: Las excepciones deben tratarse de la más concreta a la más genérica.
		 * SocketTimeoutException es más concreta que IOException.
		 */


		if (response != null && response.length == responseData.length) {
			System.err.println("Your response is as large as the datagram reception buffer!!\n"
					+ "You must extract from the buffer only the bytes that belong to the datagram!");
		}
		return response;
	}

	/**
	 * Método para probar la comunicación con el directorio mediante el envío y
	 * recepción de mensajes sin formatear ("en crudo")
	 * 
	 * @return verdadero si se ha enviado un datagrama y recibido una respuesta
	 */
	public boolean testSendAndReceive() {
		/*
		 * (Boletín SocketsUDP) Probar el correcto funcionamiento de
		 * sendAndReceiveDatagrams. Se debe enviar un datagrama con la cadena "ping" y
		 * comprobar que la respuesta recibida empieza por "pingok". En tal caso,
		 * devuelve verdadero, falso si la respuesta no contiene los datos esperados.
		 */
		boolean success = false;
		
		byte[] requestData = new String("ping").getBytes();
		byte[] responseData = sendAndReceiveDatagrams(requestData);
		
		if(responseData != null) {
			String responseString = new String(responseData,0,responseData.length);
			System.out.println("[testMode] Mensaje recibido: "+responseString);
			if(responseString.startsWith("pingok")) {
				success = true;
			}
		}


		return success;
	}

	public String getDirectoryHostname() {
		return directoryHostname;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que
	 * usa un protocolo compatible. Este método no usa mensajes bien formados.
	 * 
	 * @return Verdadero si
	 */
	public boolean pingDirectoryRaw() {
		boolean success = false;
		/*
		 * (Boletín EstructuraNanoFiles) Basándose en el código de
		 * "testSendAndReceive", contactar con el directorio, enviándole nuestro
		 * PROTOCOL_ID (ver clase NanoFiles). Se deben usar mensajes "en crudo" (sin un
		 * formato bien definido) para la comunicación.
		 * 
		 * PASOS: 1.Crear el mensaje a enviar (String "ping&protocolId"). 2.Crear un
		 * datagrama con los bytes en que se codifica la cadena : 4.Enviar datagrama y
		 * recibir una respuesta (sendAndReceiveDatagrams). : 5. Comprobar si la cadena
		 * recibida en el datagrama de respuesta es "welcome", imprimir si éxito o
		 * fracaso. 6.Devolver éxito/fracaso de la operación.
		 */

		byte[] requestData = new String("ping&"+NanoFiles.PROTOCOL_ID).getBytes();
		byte[] responseData = sendAndReceiveDatagrams(requestData);
		
		if(responseData!=null) {
			String responseString = new String(responseData,0,responseData.length);
			System.out.println("[testMode] Mensaje recibido: "+responseString);
			if(responseString.startsWith("welcome")) {
				success = true;
			}
		}

		return success;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que es
	 * compatible.
	 * 
	 * @return Verdadero si el directorio está operativo y es compatible
	 */
	public boolean pingDirectory() {
		boolean success = false;
		/*
		 * (Boletín MensajesASCII) Hacer ping al directorio 1.Crear el mensaje a
		 * enviar (objeto DirMessage) con atributos adecuados (operation, etc.) NOTA:
		 * Usar como operaciones las constantes definidas en la clase DirMessageOps :
		 * 2.Convertir el objeto DirMessage a enviar a un string (método toString)
		 * 3.Crear un datagrama con los bytes en que se codifica la cadena : 4.Enviar
		 * datagrama y recibir una respuesta (sendAndReceiveDatagrams). : 5.Convertir
		 * respuesta recibida en un objeto DirMessage (método DirMessage.fromString)
		 * 6.Extraer datos del objeto DirMessage y procesarlos 7.Devolver éxito/fracaso
		 * de la operación
		 */
		
		DirMessage pingMessage = DirMessage.DirMessagePing(DirMessageOps.OPERATION_PING, NanoFiles.PROTOCOL_ID);
		byte[] requestData = pingMessage.toString().getBytes();

		byte[] responseData = sendAndReceiveDatagrams(requestData);
		if (responseData != null) {
			try {
				DirMessage responseMessage = DirMessage.fromString(new String(responseData,0,responseData.length));
				if(responseMessage.getOperation().equals(DirMessageOps.OPERATION_PINGOK)) {
					success = true;
				}
			} catch (Exception e) {
				System.err.println("[-] Error parsing message.");
				return success;
			}

			
		}

		// Momento en el que se establece ONLINE el NanoFiles
		if (success) {
			alreadyLogin = true;
		}

		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado y
	 * publicar los ficheros que este peer servidor está sirviendo.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @param files      La lista de ficheros que este peer está sirviendo.
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	public boolean registerFileServer(int serverPort, FileInfo[] files) {

		// Comprobacion de logeo
		if (!alreadyLogin) {
			throw new RuntimeException("Trying to getFileList before login in.");
		}

		boolean success = false;

		DirMessage registerFileServerMessage = DirMessage.DirMessagePublishFiles(DirMessageOps.OPERATION_PUBLISH_FILES,files);
		byte[] requestData = registerFileServerMessage.toString().getBytes();

		// Enviar peticion
		byte[] responseData = sendAndReceiveDatagrams(requestData);
		if (responseData==null) {
			throw new RuntimeException("[-] No data recived in registerFileServer");
		}

		DirMessage responseMessage = null;

		try {
			responseMessage = DirMessage.fromString(new String(responseData,0,responseData.length));
			//Testing commits

		} catch (Exception e) {
			System.err.println("[-] Error parsing message.");
			System.exit(-1);
		}

		// Tratamiento de la respuesta
		assert(responseMessage!=null);
		switch (responseMessage.getOperation()) {
			case DirMessageOps.OPERATION_PUBLISH_FILES_OK: {
				success = true;
				break;
			}
			case DirMessageOps.OPERATION_PUBLISH_FILES_FAIL: {
				System.err.println("[-] Error in registerFileServer. Response ("+DirMessageOps.OPERATION_PUBLISH_FILES_FAIL+")");
				break;
			}
			default:
				break;
		}
		return success;
	}

	/**
	 * Método para obtener la lista de ficheros que los peers servidores han
	 * publicado al directorio. Para cada fichero se debe obtener un objeto FileInfo
	 * con nombre, tamaño y hash. Opcionalmente, puede incluirse para cada fichero,
	 * su lista de peers servidores que lo están compartiendo.
	 * 
	 * @return Los ficheros publicados al directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() {

		// Comprobacion de logeo
		if (!alreadyLogin) {
			throw new RuntimeException("Trying to getFileList before login in.");
		}



		FileInfo[] filelist = new FileInfo[0];
		// Ver TODOs en pingDirectory y seguir esquema similar
		
		DirMessage getFileListMessage = new DirMessage(DirMessageOps.OPERATION_REQEST_FILELIST);
		byte[] requestData = getFileListMessage.toString().getBytes();

		// Enviar request_file_list
		byte[] responseData = sendAndReceiveDatagrams(requestData);
		if (responseData==null) {
			throw new RuntimeException("[-] No data recived in getFileList");
		}

		System.out.println(new String(responseData,0,responseData.length));

		DirMessage responseMessage = null;
		try {
			responseMessage = DirMessage.fromString(new String(responseData,0,responseData.length));
		} catch (Exception e) {
			System.err.println("[-] Error parsing message.");
			System.exit(-1);
		}

		// Tratamiento de la respuesta
		assert (responseMessage!=null);
		switch (responseMessage.getOperation()) {
			case DirMessageOps.OPERATION_REQEST_FILELIST_OK: {
				filelist = responseMessage.getFilesInfo();
				break;
			}
			case DirMessageOps.OPERATION_REQEST_FILELIST_FAIL: {
				System.err.println("[-] Error in getFileList. Response ("+DirMessageOps.OPERATION_REQEST_FILELIST_FAIL+")");
				break;
			}
			default:
				break;
		}

		return filelist;
	}

	/**
	 * Método para obtener la lista de servidores que tienen un fichero cuyo nombre
	 * contenga la subcadena dada.
	 * 
	 * @filenameSubstring Subcadena del nombre del fichero a buscar
	 * 
	 * @return La lista de direcciones de los servidores que han publicado al
	 *         directorio el fichero indicado. Si no hay ningún servidor, devuelve
	 *         una lista vacía.
	 */
	public InetSocketAddress[] getServersSharingThisFile(String filenameSubstring) {

		// Comprobacion de logeo
		if (!alreadyLogin) {
			throw new RuntimeException("Trying to getFileList before login in.");
		}

		//Ver TODOs en pingDirectory y seguir esquema similar
		InetSocketAddress[] serversList = new InetSocketAddress[0];

		DirMessage getServersSharingThisFileMessage = DirMessage.DirMessageRequestServersList(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST,filenameSubstring);
		byte[] requestData = getServersSharingThisFileMessage.toString().getBytes();

		// Enviar request_server_list
		byte[] responseData = sendAndReceiveDatagrams(requestData);
		if (responseData==null) {
			throw new RuntimeException("[-] No data recived in getServersSharingThisFile");
		}

		DirMessage responseMessage = null;
		try {
			responseMessage = DirMessage.fromString(new String(responseData,0,responseData.length));
		} catch (Exception e) {
			System.err.println("[-] Error parsing message.");
			System.exit(-1);
		}

		// Tratamiento de la respuesta
		assert (responseMessage!=null);

		switch (responseMessage.getOperation()) {
			case DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_OK: {
				serversList = responseMessage.getServersList();
				break;
			}
			case DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL: {
				System.err.println("[-] Error in getServersSharingThisFile. Response ("+DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_FAIL+")");
				break;
			}
			default:
				break;
		}
		

		return serversList;
	}

	/**
	 * Método para darse de baja como servidor de ficheros.
	 * 
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y ha dado de baja sus ficheros.
	 */
	public boolean unregisterFileServer() {
		boolean success = false;




		return success;
	}




}
