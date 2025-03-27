package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps {

	/*
	 * (Boletín MensajesASCII) Añadir aquí todas las constantes que definen
	 * los diferentes tipos de mensajes del protocolo de comunicación con el
	 * directorio (valores posibles del campo "operation").
	 */
	public static final String OPERATION_INVALID = "invalid_operation";

	// Mensajes de petición y respuesta de ping
	public static final String OPERATION_PING = "ping";
	public static final String OPERATION_PINGOK = "ping_ok";
	//TODO mensaje de ping_fail

	// Mensajes de petición y respuesta de listado de ficheros
	public static final String OPERATION_REQEST_FILELIST = "request_file_list";
	public static final String OPERATION_REQEST_FILELIST_OK = "request_file_list_ok";
	public static final String OPERATION_REQEST_FILELIST_FAIL = "request_file_list_fail";

	// Mensajes de petición y respuesta de publicación de ficheros
	public static final String OPERATION_PUBLISH_FILES = "publish_files";
	public static final String OPERATION_PUBLISH_FILES_OK = "publish_files_ok";
	public static final String OPERATION_PUBLISH_FILES_FAIL = "publish_files_fail";

	// Mensajes de petición y respuesta de listado de servidores que comparten un fichero
	public static final String OPERATION_REQUEST_SERVERS_LIST = "request_servers_list";
	public static final String OPERATION_REQUEST_SERVERS_LIST_OK = "request_servers_list_ok";
	public static final String OPERATION_REQUEST_SERVERS_LIST_FAIL = "request_servers_list_fail";

	




}
