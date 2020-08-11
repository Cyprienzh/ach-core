package ach_core;

import java.io.IOException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class Core_ModuleThread extends Thread{
	
	private SSLServerSocket modules_socket;
	
	public Core_ModuleThread(SSLServerSocket pSoc) {
		this.modules_socket = pSoc;
	}
	
	public void run() {
		SSLSocket module_socket = null;
		while(Core.CORE_RUNNING) {
			try {
				module_socket = (SSLSocket) modules_socket.accept();
				module_socket.startHandshake();
				Core_ModuleListener module_listener = new Core_ModuleListener(module_socket);
				module_listener.start();
			}
			catch(SSLException e) {Core.log_("Probleme de connexion avec le module "+e.getLocalizedMessage(),"ERROR");}
			catch(IOException e) {e.printStackTrace();}
		}

	}

}
