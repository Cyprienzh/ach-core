package ach_core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.net.ssl.SSLServerSocket;

import ach_core.Core_ModuleManager.ModuleException;

public class Core {

	final static int MODULE_PORT = 9999;
	final static boolean LISTEN_MODULES = true;
	final static ArrayList<String> CORE_MODULES = new ArrayList<String>(Arrays.asList());
	final static ArrayList<String> LOG_FILTER = new ArrayList<String>(Arrays.asList("DEBUG"));
	
	static boolean CORE_RUNNING = false;
	static SSLServerSocket module_socket;
	
	public static void main(String[] args) {
		log_("Lancement du noyau Autonomous and Connected Hab (ACH)", "INIT");
		
		//Ouverture du port d'écoute pour les modules
		try {
			module_socket = SSLServerSocketKeystoreFactory.getServerSocketWithCert(MODULE_PORT, Core.class.getResourceAsStream("/PRIVATE_OWNET.jks"), "OwnetPassword", SSLServerSocketKeystoreFactory.ServerSecureType.TLSv1_2);
		}
		catch(IOException e) {e.printStackTrace();} 
		catch (KeyManagementException e) {e.printStackTrace();}
		catch (UnrecoverableKeyException e) {e.printStackTrace();}
		catch (NoSuchAlgorithmException e) {e.printStackTrace();} 
		catch (CertificateException e) {e.printStackTrace();}
		catch (KeyStoreException e) {e.printStackTrace();}
		
		//Chargement des modules
		startup_modules_load("startup.list");
		
		

	}
	
	public static void startup_modules_load(String filename) {
		try {
			//Chargement des modules CORE
			for(String module : CORE_MODULES) {
				log_("Lancement du module "+module+" (CORE)","INIT");
				Core_ModuleManager mod = new Core_ModuleManager(module);
				mod.load_module();
				mod.listenProcess(LISTEN_MODULES);
			}
		
			//Chargement des modules STARTUP
			File startup_file = new File(filename);
			if(startup_file.exists()) {
				BufferedReader st_fs = new BufferedReader(new FileReader(startup_file));
				String line;
				while((line = st_fs.readLine()) != null) {
					log_("Lancement du module "+line,"INIT");
					Core_ModuleManager mod = new Core_ModuleManager(line);
					mod.load_module();
				}
				st_fs.close();
			}
		}
		catch(ModuleException e) {e.printStackTrace();}
		catch(IOException e) {e.printStackTrace();}
		
	}
	
	
	public static void log_(String message, String type) {
		if(LOG_FILTER.contains(type)) {
			Date now = new Date();
			SimpleDateFormat formater = new SimpleDateFormat("dd/MM/yy HH:mm:ss:SS");
			System.out.println(formater.format(now)+" -> ["+type+"] : "+message);
			}	
		
	}

}
