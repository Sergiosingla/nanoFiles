package es.um.redes.nanoFiles.tcp.server;
import java.net.Socket;

public class NFServerThread extends Thread {
	/*
	 * TODO: Esta clase modela los hilos que son creados desde NFServer y cada uno
	 * de los cuales simplemente se encarga de invocar a
	 * NFServer.serveFilesToClient con el socket retornado por el método accept
	 * (un socket distinto para "conversar" con un cliente)
	 */

	private Socket clientSocket = null;

	public NFServerThread(Socket socket) {
		clientSocket = socket;
	}	

	@Override
	public void run() {
		try {
			NFServer.serveFilesToClient(clientSocket);
		} catch (Exception e) {
			System.err.println("[-] Error during download: " + e.getMessage());
		} finally {
			try {
				clientSocket.close();
			} catch (Exception e) {
				System.err.println("[-] Error closing client socket: " + e.getMessage());
			}
		}
	}

}
