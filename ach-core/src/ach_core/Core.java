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
import java.util.Scanner;

import javax.net.ssl.SSLServerSocket;

import org.json.JSONObject;

import ach_core.Core_ModuleManager.ModuleException;

public class Core {

	final static int MODULE_PORT = 9999;
	final static boolean LISTEN_MODULES = true;
	final static ArrayList<String> CORE_MODULES = new ArrayList<String>(Arrays.asList());
	final static ArrayList<String> LOG_FILTER = new ArrayList<String>(Arrays.asList("DEBUG","INFOS","ERROR","INIT","HELP"));
	
	static boolean CORE_RUNNING = false;
	static SSLServerSocket module_socket;
	
	public static void main(String[] args) {
		log_("Lancement du noyau Autonomous and Connected Hab (ACH)", "INIT");
		
		//Ouverture du port d'écoute pour les modules
		try {
			module_socket = SSLServerSocketKeystoreFactory.getServerSocketWithCert(MODULE_PORT, Core.class.getResourceAsStream("/PRIVATE_OWNET.jks"), "OwnetPassword", SSLServerSocketKeystoreFactory.ServerSecureType.TLSv1_2);
			CORE_RUNNING = true;
			log_("Ouverture du port d'écoute MODULES ("+MODULE_PORT+") complétée","INIT");
		}
		catch(IOException e) {e.printStackTrace();} 
		catch (KeyManagementException e) {e.printStackTrace();}
		catch (UnrecoverableKeyException e) {e.printStackTrace();}
		catch (NoSuchAlgorithmException e) {e.printStackTrace();} 
		catch (CertificateException e) {e.printStackTrace();}
		catch (KeyStoreException e) {e.printStackTrace();}
		
		//Chargement des modules
		startup_modules_load("startup.list");
		
		//Lancement du Thread d'écoute des modules
		Core_ModuleThread module_thread = new Core_ModuleThread(module_socket);
		module_thread.start();
		
		//Ouverture du flux IN (entrée clavier)
		Scanner input_user = new Scanner(System.in);
		
		//Boucle du terminal de commande
		while(CORE_RUNNING) {
			System.out.print("core-command > ");
			String[] input_arg = input_user.nextLine().split(" ");
			
			switch(input_arg[0]) {
			
			case "command":
				//Envoi une commande à un module. Usage : command <module> ..argument(s)
				if(input_arg.length > 2) {
					Core_ModuleListener module_listener_com = Core_ModuleListener.getListenerByName(input_arg[1]);
					if(module_listener_com != null) {  //Verification de l'existence du module
						String tmp_st = input_arg[2];
						//Concatenation de la commande à envoyer
						for(int i=3; i<input_arg.length;i++) {tmp_st = tmp_st.concat(" "+input_arg[i]);}
						
						//Creation du paquet commande
						JSONObject tmp_json = new JSONObject();
						tmp_json.put("type", "packet.command");
						tmp_json.put("command.args", tmp_st);
						
						//Envoi du paquet commande
						module_listener_com.sendData(tmp_json.toString());
					}
					else {log_("Module introuvable","ERROR");}
				}
				else {log_("Merci de spécifier les arguments nécessaires","ERROR");}
				break;
				
			case "exit":
				//Fermeture du noyau ACH # A COMPLETER POUR UNE MEILLEURE FERMETURE #
				CORE_RUNNING = false;
				System.exit(0);
				break;
				
			case "load_module" :
				//Charge un module apparenté au noyau ACH. Usage : load_module <module>
				try {
					if(input_arg.length == 2) {
						
						//Chargement du module
						Core_ModuleManager module = new Core_ModuleManager(input_arg[1]);
						module.load_module();
						log_("Lancement du module "+input_arg[1], "INFOS");
						//Réglage par défault des paramètres d'écoute du module (en fonction du paramètre global LISTEN_MODULES)
						module.listenProcess(LISTEN_MODULES);
					}
					else {log_("Utilisation : load_module <module>","HELP");}
				}
				catch(ModuleException e) {log_("Module Introuvable ou déjà en cours d'exécution", "ERROR");}
				catch(IOException e) {e.printStackTrace();}
				break;
				
			case "unload_module" :
				//Arrete un module chargé et apparenté au noyau ACH. Usage : unload_module <module>
				try {
					if(input_arg.length == 2) {
						log_("Arret du module "+input_arg[1],"INFOS");
						
						//Récupération du module chargé
						Core_ModuleManager module = Core_ModuleManager.getModuleByName(input_arg[1]);
						if(module != null) {
							//Arrêt de l'écoute du module
							module.listenProcess(false);
							Core_ModuleListener module_listener = Core_ModuleListener.getListenerByName(input_arg[1]);
							module_listener.closeConnection();
							//Arrêt du module
							module.unload_module();
						}
						else {log_("Module introuvable","ERROR");}
					}
					else {log_("Utilisation : unload_module <module>","HELP");}
				}
				catch(ModuleException e) {log_("Module introuvable","ERROR");}
				break;
				
			case "reload_module" :
				//Redémarre un module chargé et apparenté au noyau ACH. Usage : reload_module <module>
				try {
					if(input_arg.length == 2) {
						log_("Redémarrage du module "+input_arg[1],"INFOS");
						//Récupération du module chargé
						Core_ModuleManager module = Core_ModuleManager.getModuleByName(input_arg[1]);
						if(module != null) {
							//Arrêt de l'écoute du module
							module.listenProcess(false);
							Core_ModuleListener module_listener = Core_ModuleListener.getListenerByName(input_arg[1]);
							module_listener.closeConnection();
							//Arrêt du module
							module.unload_module();
							
							//Chargement du module
							Core_ModuleManager module_n = new Core_ModuleManager(input_arg[1]);
							module_n.load_module();
							//Réglage par défault des paramètres d'écoute du module (en fonction du paramètre global LISTEN_MODULES)
							module_n.listenProcess(LISTEN_MODULES);
						}
						else {log_("Module introuvable","ERROR");}
					}
					else {log_("Utilisation : reload_module <module>","HELP");}
				}
				catch(ModuleException e) {log_("Module introuvable","ERROR");}
				catch(IOException e) {e.printStackTrace();}
				break;
				
			case "ping":
				log_("pong.","INFOS");
				break;
			
			default:
				log_("La commande "+input_arg[0]+" n'existe pas.","ERROR");
				break;
				
			}
		}
		input_user.close();
		
		

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
