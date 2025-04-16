package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;

	private DataInputStream dis;
	private DataOutputStream dos;



	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;

		// Validar el host y el puerto
		if (serverAddr.getHostString() == null || serverAddr.getHostString().isEmpty()) {
			throw new IllegalArgumentException("Invalid host: " + serverAddr);
		}
		if (serverAddr.getPort() <= 0) {
			throw new IllegalArgumentException("Invalid port: " + serverAddr.getPort());
		}
		/*
		 * (Boletín SocketsTCP) Se crea el socket a partir de la dirección del
		 * servidor (IP, puerto). La creación exitosa del socket significa que la
		 * conexión TCP ha sido establecida.
		 */
		socket = new Socket(serverAddr.getHostString(), serverAddr.getPort());


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
