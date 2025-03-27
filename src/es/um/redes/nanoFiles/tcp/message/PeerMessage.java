package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {

	// Tamaño default de un chunk: 4000 bytes
	private static final int DEFAULT_CHUNK_SIZE = 4000;


	private byte opcode;

	/*
	 * (Boletín MensajesBinarios) Añadir atributos u otros constructores
	 * específicos para crear mensajes con otros campos, según sea necesario
	 * 
	 */
	private int substringLength;
	private String substring;
	private String hashCode;
	private double fileOffset;
	private int chunckSize;
	private byte[] chunckData;




	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}

	public static PeerMessage PeerMessageDownloadFile(String substring) {
		PeerMessage msg = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_FILE);
		msg.setSubstring(substring);
		return msg;
	}

	public static PeerMessage PeerMessageDownloadAprove(String hashCode) {
		PeerMessage msg = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_APROVE);
		//System.out.println("Creating download aprove message with hash: " + hashCode);
		msg.setHashCode(hashCode);
		return msg;
	}

	public static PeerMessage PeerMessageGetChunck(double _fileOffset, int _chunckSize) {
		PeerMessage msg = new PeerMessage(PeerMessageOps.OPCODE_GET_CHUNCK);
		msg.setFileOffset(_fileOffset);
		msg.setChunckSize(_chunckSize);
		return msg;
	}

	public static PeerMessage PeerMessageGetChunck(double _fileOffset) {
		return PeerMessageGetChunck(_fileOffset,DEFAULT_CHUNK_SIZE);
	}

	// Constructor donde solo se pasa la data, su longuitud se calcula
	public static PeerMessage PeerMessageSendChunk(byte[] _chunckData) {
		PeerMessage msg = new PeerMessage(PeerMessageOps.OPCODE_SEND_CHUNK);
		msg.setChunckSize(_chunckData.length);
		msg.setChunckData(_chunckData);
		return msg;
	}

	// Constructor donde se pasa la longuitud de la data y la data
	public static PeerMessage PeerMessageSendChunk(int _chunkSize,byte[] _chunckData) {
		PeerMessage msg = new PeerMessage(PeerMessageOps.OPCODE_SEND_CHUNK);
		msg.setChunckSize(_chunkSize);
		msg.setChunckData(_chunckData);
		return msg;
	}

	/*
	 * (Boletín MensajesBinarios) Crear métodos getter y setter para obtener
	 * los valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".
	 */
	public byte getOpcode() {
		return opcode;
	}

	public void setSubstring(String substring) {
		this.substring = substring;
		this.substringLength = substring.length();
	}

	public String getSubstring() {
		return substring;
	}

	public int getSubstringLength() {
		return substringLength;
	}

	public void setHashCode(String hashCode) {
		this.hashCode = hashCode;
	}

	public String getHashCode() {
		return hashCode;
	}

	public void setFileOffset(double fileOffset) {
		this.fileOffset = fileOffset;
	}

	public double getFileOffset() {
		return fileOffset;
	}

	public void setChunckSize(int chunckSize) {
		this.chunckSize = chunckSize;
	}

	public int getChunckSize() {
		return chunckSize;
	}

	public void setChunckData(byte[] chunckData) {
		this.chunckData = chunckData;
	}	

	public byte[] getChunckData() {
		return chunckData;
	}





	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El array de bytes recibido
	 * @return Un objeto de esta clase cuyos atributos contienen los datos del
	 *         mensaje recibido.
	 * @throws IOException
	 */
	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
		/*
		 * (Boletín MensajesBinarios) En función del tipo de mensaje, leer del
		 * socket a través del "dis" el resto de campos para ir extrayendo con los
		 * valores y establecer los atributos del un objeto DirMessage que contendrá
		 * toda la información del mensaje, y que será devuelto como resultado. NOTA:
		 * Usar dis.readFully para leer un array de bytes, dis.readInt para leer un
		 * entero, etc.
		 */
		PeerMessage message = new PeerMessage();
		byte opcode = dis.readByte();
		switch (opcode) {
		case PeerMessageOps.OPCODE_NOT_FOUND: {
			message = new PeerMessage(PeerMessageOps.OPCODE_NOT_FOUND);
			break;	
		}
		case PeerMessageOps.OPCODE_AMBIGUOUS_NAME: {
			message = new PeerMessage(PeerMessageOps.OPCODE_AMBIGUOUS_NAME);
			break;
		}
		case PeerMessageOps.OPCODE_DOWNLOAD_FILE: {
			int length = dis.readInt();
			String substringName = new String(dis.readNBytes(length));
			message = PeerMessageDownloadFile(substringName);
			break;
		}
		case PeerMessageOps.OPCODE_DOWNLOAD_APROVE: {
			byte[] hashBytes = new byte[40];
			dis.readFully(hashBytes);
			String hash = new String(hashBytes).trim();
			message = PeerMessageDownloadAprove(hash);
			break;
		}
		case PeerMessageOps.OPCODE_CORRUPT_DOWNLOAD: {
			message = new PeerMessage(PeerMessageOps.OPCODE_CORRUPT_DOWNLOAD);
			break;
		}
		case PeerMessageOps.OPCODE_INVALID_CODE: {
			message = new PeerMessage(PeerMessageOps.OPCODE_INVALID_CODE);
			break;
		}
		case PeerMessageOps.OPCODE_GET_CHUNCK: {
			double fileOffset = dis.readDouble();
			int chunckSize = dis.readInt();
			message = PeerMessageGetChunck(fileOffset,chunckSize);
			break;
		}
		case PeerMessageOps.OPCODE_SEND_CHUNK: {
			int chunckSize = dis.readInt();
			byte[] chunckData = new byte[chunckSize];
			dis.readFully(chunckData);
			message = PeerMessageSendChunk(chunckSize,chunckData);
			break;
		}
		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
		/*
		 * (Boletín MensajesBinarios): Escribir los bytes en los que se codifica el
		 * mensaje en el socket a través del "dos", teniendo en cuenta opcode del
		 * mensaje del que se trata y los campos relevantes en cada caso. NOTA: Usar
		 * dos.write para leer un array de bytes, dos.writeInt para escribir un entero,
		 * etc.
		 */
		dos.writeByte(opcode);
		switch (opcode) {
		case PeerMessageOps.OPCODE_INVALID_CODE: 
		case PeerMessageOps.OPCODE_NOT_FOUND:
		case PeerMessageOps.OPCODE_AMBIGUOUS_NAME:
		case PeerMessageOps.OPCODE_CORRUPT_DOWNLOAD:
			break;
		case PeerMessageOps.OPCODE_DOWNLOAD_FILE: {
			dos.writeInt(substringLength);
			dos.write(substring.getBytes());
			break;
		}
		case PeerMessageOps.OPCODE_DOWNLOAD_APROVE: {
			dos.write(hashCode.getBytes());
			break;
		}
		case PeerMessageOps.OPCODE_GET_CHUNCK: {
			dos.writeDouble(fileOffset);
			dos.writeInt(chunckSize);
			break;
		}
		case PeerMessageOps.OPCODE_SEND_CHUNK: {
			dos.writeInt(chunckSize);
			dos.write(chunckData);
			break;
		}

		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}




}
