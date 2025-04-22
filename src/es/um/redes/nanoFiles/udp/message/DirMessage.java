package es.um.redes.nanoFiles.udp.message;

import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = '·'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	/*
	 * (Boletín MensajesASCII) Definir de manera simbólica los nombres de
	 * todos los campos que pueden aparecer en los mensajes de este protocolo
	 * (formato campo:valor)
	 */
	private static final String FIELDNAME_PROTOCOL = "protocol";
	private static final String FIELDNAME_FILES = "files";
	private static final String FIELDNAME_FILENAMESUBSTRING = "filenamesubstring";
	private static final String FIELDNAME_SERVERS_LIST = "serverslist";
	private static final String FIELDNAME_SERVER_PORT = "serverport";




	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation = DirMessageOps.OPERATION_INVALID;
	/**
	 * Identificador de protocolo usado, para comprobar compatibilidad del directorio.
	 */
	private String protocolId;
	/*
	 * (Boletín MensajesASCII) Crear un atributo correspondiente a cada uno de
	 * los campos de los diferentes mensajes de este protocolo.
	 */
	private FileInfo[] files = new FileInfo[0];
	private String filenameSubstring;
	private InetSocketAddress[] serversList = new InetSocketAddress[0];
	private int serverPort;




	public DirMessage(String op) {
		operation = op;
	}

	/*
	 * (Boletín MensajesASCII) Crear diferentes constructores adecuados para
	 * construir mensajes de diferentes tipos con sus correspondientes argumentos
	 * (campos del mensaje)
	 */

	// Constructor para mensajes de comprobacion de protocolId
	public static DirMessage DirMessagePing(String op, String _protocolId){
		if(!op.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: new protocolId message called by unexpected opeartion (" + op + ")");
		}
		DirMessage msg = new DirMessage(op);
		msg.setProtocolID(_protocolId);

		return msg;
	}

	// Constructor para mensajes request_file_list_ok
	public static DirMessage DirMessageRequestFileListOk(String op, FileInfo[] _files) {
		if((!op.equals(DirMessageOps.OPERATION_REQEST_FILELIST_OK))) {
			throw new RuntimeException(
					"DirMessage: new request_file_list message / publish_files message called by unexpected opeartion (" + op + ")");
		}
		DirMessage msg = new DirMessage(op);
		msg.setFilesInfo(_files);

		return msg;
	}

	// Constructor para mensajes publish_files
	public static DirMessage DirMessagePublishFiles(String op, FileInfo[] _files, int _port) {
		if((!op.equals(DirMessageOps.OPERATION_PUBLISH_FILES))) {
			throw new RuntimeException(
					"DirMessage: new request_file_list message / publish_files message called by unexpected opeartion (" + op + ")");
		}
		DirMessage msg = new DirMessage(op);
		msg.setFilesInfo(_files);
		msg.setPort(_port);

		return msg;
	}

	// Constructor para mensajes de unregister_server
	public static DirMessage DirMessageUnregisterServer(int _port){
		DirMessage msg = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_SERVER);
		msg.setPort(_port);
		return msg;
	}

	// Constructor para mensajes request_servers_list
	public static DirMessage DirMessageRequestServersList(String op, String _filenameSubstring) {
		if(!op.equals(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST)) {
			throw new RuntimeException(
					"DirMessage: new request_servers_list message called by unexpected opeartion (" + op + ")");
		}
		DirMessage msg = new DirMessage(op);
		msg.setFileNameSubstring(_filenameSubstring);
		return msg;
	}

	// Constructor para mensajes request_server_list_ok
	public static DirMessage DirMessageRequestServersListOk(String op, InetSocketAddress[] _serversList) {
		if(!op.equals(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_OK)) {
			throw new RuntimeException(
					"DirMessage: new request_file_list_ok message called by unexpected opeartion (" + op + ")");
		}
		DirMessage msg = new DirMessage(op);
		msg.setServersList(_serversList);

		return msg;
	}





	public String getOperation() {
		return operation;
	}

	/*
	 * (Boletín MensajesASCII) Crear métodos getter y setter para obtener los
	 * valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".
	 */
	public void setProtocolID(String protocolIdent) {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		protocolId = protocolIdent;
	}

	public void setFileNameSubstring(String _filenameSubstring) {
		if (!operation.equals(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST)) {
			throw new RuntimeException(
					"DirMessage: setFileNameSubstring called for message of unexpected type (" + operation + ")");
		}
		filenameSubstring = _filenameSubstring;
	}

	public void setFilesInfo(FileInfo[] _files) {
		if (!operation.equals(DirMessageOps.OPERATION_REQEST_FILELIST_OK)&&!operation.equals(DirMessageOps.OPERATION_PUBLISH_FILES)) {
			throw new RuntimeException(
					"DirMessage: setFilesInfor called for message of unexpected type (" + operation + ")");
		}
		files = _files;
	}

	public void setServersList(InetSocketAddress[] _serversList) {
		if (!operation.equals(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_OK)) {
			throw new RuntimeException(
					"DirMessage: setServersList called for message of unexpected type (" + operation + ")");
		}
		serversList = _serversList;
	}

	public void setPort(int _port){
		serverPort = _port;
	}



	public int getPort(){
		return serverPort;
	}

	public String getProtocolId() {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: getProtocolId called for message of unexpected type (" + operation + ")");
		}
		return protocolId;
	}

	public FileInfo[] getFilesInfo() {
		if ((!operation.equals(DirMessageOps.OPERATION_REQEST_FILELIST_OK))&&(!operation.equals(DirMessageOps.OPERATION_PUBLISH_FILES))) {
			throw new RuntimeException(
					"DirMessage: getFIlesInfo called for message of unexpected type (" + operation + ")");
		}
		return files;
	}

	public String getFileNameSubstring() {
		if (!operation.equals(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST)) {
			throw new RuntimeException(
					"DirMessage: getFileNameSubstring called for message of unexpected type (" + operation + ")");
		}
		return filenameSubstring;
	}

	public InetSocketAddress[] getServersList() {
		if (!operation.equals(DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_OK)) {
			throw new RuntimeException(
					"DirMessage: getServersList called for message of unexpected type (" + operation + ")");
		}
		return serversList;
	}




	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static DirMessage fromString(String message) {
		/*
		 * (Boletín MensajesASCII) Usar un bucle para parsear el mensaje línea a
		 * línea, extrayendo para cada línea el nombre del campo y el valor, usando el
		 * delimitador DELIMITER, y guardarlo en variables locales.
		 */

		// System.out.println("DirMessage read from socket:");
		// System.out.println(message);
		String[] lines = message.split(END_LINE + "");
		// Local variables to save data during parsing
		DirMessage m = null;

		for (String line : lines) {
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();

			switch (fieldName) {
				case FIELDNAME_OPERATION: {
					assert (m == null);
					m = new DirMessage(value);
					break;
				}
				case FIELDNAME_PROTOCOL: {
					assert (m != null);
					m.setProtocolID(value);
					break;
				}
				case FIELDNAME_FILES : {
					assert (m != null);
					m.setFilesInfo(FileInfo.fromString(value));
					break;
				}
				case FIELDNAME_FILENAMESUBSTRING: {
					assert (m != null);
					m.setFileNameSubstring(value);
					break;
				}
				case FIELDNAME_SERVERS_LIST: {
					assert (m != null);
					m.setServersList(strToInetSocketAddress(value));
					break;
				}
				case FIELDNAME_SERVER_PORT: {
					assert (m != null);
					int port = Integer.parseInt(value);
					m.setPort(port);
					break;
				}	
				

				default:
					System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
					System.err.println("Message was:\n" + message);
					System.exit(-1);
			}
		}

		return m;
	}

	private static InetSocketAddress[] strToInetSocketAddress(String str) {
		String[] servers = str.split(",");
		InetSocketAddress[] result = new InetSocketAddress[servers.length];
		for (int i = 0; i < servers.length; i++) {
			String[] server = servers[i].split(":");
			result[i] = new InetSocketAddress(server[0], Integer.parseInt(server[1]));
		}
		return result;
	}


	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo
		/*
		 * (Boletín MensajesASCII) En función de la operación del mensaje, crear
		 * una cadena la operación y concatenar el resto de campos necesarios usando los
		 * valores de los atributos del objeto.
		 */
		switch (operation) {
			case DirMessageOps.OPERATION_PING: {
				sb.append(FIELDNAME_PROTOCOL + DELIMITER + protocolId + END_LINE);
				break;
			}
			case DirMessageOps.OPERATION_REQEST_FILELIST_OK: {
				sb.append(FIELDNAME_FILES + DELIMITER);
				for (int i = 0; i < files.length; i++) {
					sb.append(files[i].toString());
				}
				sb.append(END_LINE);
				break;
			}
			case DirMessageOps.OPERATION_PUBLISH_FILES: {
				sb.append(FIELDNAME_SERVER_PORT + DELIMITER + String.valueOf(getPort())+ END_LINE);
				sb.append(FIELDNAME_FILES + DELIMITER);
				for (int i = 0; i < files.length; i++) {
					sb.append(files[i].toString());
				}
				sb.append(END_LINE);
				break;
			}
			case DirMessageOps.OPERATION_UNREGISTER_SERVER: {
				sb.append(FIELDNAME_SERVER_PORT + DELIMITER + String.valueOf(getPort())+ END_LINE);
				break;
			}
			case DirMessageOps.OPERATION_REQUEST_SERVERS_LIST: {
				sb.append(FIELDNAME_FILENAMESUBSTRING + DELIMITER + filenameSubstring + END_LINE);
				break;
			}
			case DirMessageOps.OPERATION_REQUEST_SERVERS_LIST_OK: {
				sb.append(FIELDNAME_SERVERS_LIST + DELIMITER);
				for (int i = 0; i < serversList.length; i++) {
					sb.append(serversList[i].toString());
					if(i < serversList.length - 1) {
						sb.append(",");
					}
				}
				sb.append(END_LINE);
				break;
			}
			default:
				break;
		}

		sb.append(END_LINE); // Marcamos el final del mensaje
		//System.out.println(sb);
		return sb.toString();
	}

}
