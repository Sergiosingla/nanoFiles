package es.um.redes.nanoFiles.tcp.message;

import java.util.Map;
import java.util.TreeMap;

public class PeerMessageOps {

	public static final byte OPCODE_INVALID_CODE = 0;

	/*
	 * (Boletín MensajesBinarios) Añadir aquí todas las constantes que definen
	 * los diferentes tipos de mensajes del protocolo de comunicación con un par
	 * servidor de ficheros (valores posibles del campo "operation").
	 */
	public static final byte OPCODE_NOT_FOUND = 1;
	public static final byte OPCODE_AMBIGUOUS_NAME = 2;
	public static final byte OPCODE_DOWNLOAD_FILE = 3;
	public static final byte OPCODE_DOWNLOAD_APROVE = 4;
	public static final byte OPCODE_GET_CHUNCK = 5;
	public static final byte OPCODE_SEND_CHUNK = 6;
	public static final byte OPCODE_CORRUPT_DOWNLOAD = 9;
	public static final byte OPCODE_UPLOAD = 10;
	public static final byte OPCODE_UPLOAD_APROVE = 11;
	public static final byte OPCODE_UPLOAD_DENY = 12;
	public static final byte OPCODE_ERROR = 13;



	/*
	 * (Boletín MensajesBinarios) Definir constantes con nuevos opcodes de
	 * mensajes definidos anteriormente, añadirlos al array "valid_opcodes" y añadir
	 * su representación textual a "valid_operations_str" EN EL MISMO ORDEN.
	 */
	private static final Byte[] _valid_opcodes = { OPCODE_INVALID_CODE,
			OPCODE_NOT_FOUND,
			OPCODE_AMBIGUOUS_NAME,
			OPCODE_DOWNLOAD_FILE,
			OPCODE_DOWNLOAD_APROVE,
			OPCODE_GET_CHUNCK,
			OPCODE_SEND_CHUNK,
			OPCODE_CORRUPT_DOWNLOAD,
			OPCODE_UPLOAD,
			OPCODE_UPLOAD_APROVE,
			OPCODE_UPLOAD_DENY,
			OPCODE_ERROR,
	};
	private static final String[] _valid_operations_str = { "INVALID_OPCODE",
			"NOT_FOUND",
			"AMBIGUOUS_NAME",
			"DOWNLOAD_FILE",
			"DOWNLOAD_APROVE",
			"GET_CHUNCK",
			"SEND_CHUNK",
			"CORRUPT_DOWNLOAD",
			"UPLOAD",
			"UPLOAD_APROVE",
			"UPLOAD_DENY",
			"ERROR",
	};

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) {
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte operationToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OPCODE_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	public static String opcodeToOperation(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}
}
