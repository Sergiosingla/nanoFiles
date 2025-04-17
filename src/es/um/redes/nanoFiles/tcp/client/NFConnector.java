package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;

	private DataInputStream dis;
	private DataOutputStream dos;



	public NFConnector(InetSocketAddress fserverAddr) throws IOException {
		serverAddr = fserverAddr;

		// Validar el host y el puerto
		// Validar el puerto
		if (serverAddr.getPort() <= 0) {
			throw new IllegalArgumentException("Invalid port: " + serverAddr.getPort());

		}
		/*
		 * (Boletín SocketsTCP) Se crea el socket a partir de la dirección del
		 * servidor (IP, puerto). La creación exitosa del socket significa que la
		 * conexión TCP ha sido establecida.
		 */
		// Obtener el host
		String host = serverAddr.getAddress() != null
		? serverAddr.getAddress().getHostAddress() // Dirección IP sin prefijo "/"
		: serverAddr.getHostString(); // Nombre del host como respaldo

		// Eliminar el prefijo "/" si está presente
		if (host != null && host.startsWith("/")) {
			host = host.substring(1);
		}

		socket = new Socket(host,serverAddr.getPort());


		/*
		 * (Boletín SocketsTCP) Se crean los DataInputStream/DataOutputStream a
		 * partir de los streams de entrada/salida del socket creado. Se usarán para
		 * enviar (dos) y recibir (dis) datos del servidor.
		 */
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());


	}

	public void test() {
		/*
		 * (Boletín SocketsTCP) Enviar entero cualquiera a través del socket y
		 * después recibir otro entero, comprobando que se trata del mismo valor.
		 */

		 int intToSend = 42;
		 int intRecived;

		try {
			System.out.println("Sending integer: " + intToSend);
			dos.writeInt(intToSend);
			intRecived = dis.readInt();
			System.out.println("Recived integer: " + intRecived);
		} catch (IOException e) {
			System.err.println("Error sending/receiving integer");
		}
	}

	public PeerMessage sendAndRecive(PeerMessage msgToSend){
		PeerMessage msgRecive = null;

		try {
			msgToSend.writeMessageToOutputStream(dos);
			msgRecive = PeerMessage.readMessageFromInputStream(dis);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("[-] Error during sending data to "+serverAddr);
			msgRecive = null;
			return msgRecive;
		}
		return msgRecive;
	}





	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

}
