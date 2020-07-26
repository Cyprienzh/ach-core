package ach_core;

import java.util.ArrayList;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;

public class Core_ModuleManager {
	
	private static ArrayList<Core_ModuleManager> modules_list = new ArrayList<Core_ModuleManager>();
	
	private String module_name = "";
	private String module_directory = "";
	private String module_key = "";
	private Process module_process = null;
	private StreamGobbler module_error = null;
	private StreamGobbler module_out = null;
	
	public Core_ModuleManager(String pName) {
		this.module_name = pName;
		if(System.getProperty("os.name").equals("Windows 10")) {this.module_directory = System.getProperty("user.dir")+"\\modules\\"+pName+"\\";}
		else {this.module_directory = System.getProperty("user.dir")+"/modules/"+pName+"/";}
	}
	
	public void load_module() throws ModuleException, IOException{
		module_test();
		module_keygen();
		this.module_process = Runtime.getRuntime().exec("java -jar "+this.module_name+".jar "+this.module_key,null,new File(this.module_directory));
		modules_list.add(this);
	}
	
	public void unload_module() throws ModuleException {
		if(this.module_process != null) {
			this.module_process.destroy();
			modules_list.remove(this);
		}
	}
	
	private void module_test() throws ModuleException{
		//Cette fonction teste l'existence du module (elle sera amenée à effectuer plus de tests prochainement)
		for(Core_ModuleManager mod : modules_list) {if(mod.module_name.equals(this.module_name)) {throw new ModuleException("Module already running");}}
		File module = new File(this.module_directory+this.module_name+".jar");
		if(!module.exists()) {throw new ModuleException("No module named "+this.module_name);}
	}
	
	private void module_keygen() {
		
		//Generation de la clé module
		SecureRandom rand = new SecureRandom();
		byte[] tmp_key = new byte[24];
		rand.nextBytes(tmp_key);
		
		//Serialisation de la clé module
		this.module_key = Base64.getEncoder().encodeToString(tmp_key);
	}
	
	public String getName() {return this.module_name;}
	public String getDirectory() {return this.module_directory;}
	public Process getProcess() {return this.module_process;}
	public String getKey() {return this.module_key;}	
	public void listenProcess(boolean listen) {
		if(listen && this.module_out == null) {
			this.module_error = new StreamGobbler(this.module_process.getErrorStream(),this.module_name);
			this.module_out = new StreamGobbler(this.module_process.getInputStream(),this.module_name);
			module_error.start();
			module_out.start();
		}
		else if(!listen && this.module_out != null) {
			this.module_error.stopL();
			this.module_out.stopL();
		}
	}
	
	
	
	public static ArrayList<Core_ModuleManager> getLoadedModules() {return modules_list;}
	public static Core_ModuleManager getModuleByName(String pName) {
		for(Core_ModuleManager module : modules_list) {
			if(module.module_name.equals(pName)) {return module;}
		}
		return null;
	}
	
	
	//Classe pour afficher la sortie du module sur le CORE
	public class StreamGobbler extends Thread {
		InputStream is;
		String module_name;
		private boolean running;
		
		public StreamGobbler(InputStream is, String pName) {
			this.is = is;
			this.module_name = pName;
		}
		
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
				String line = null;
				running = true;
				while( (line = br.readLine()) != null && this.running) {
					Core.log_("("+this.module_name+") "+line, "DEBUG");
					
				}
			}
			catch(IOException e) {e.printStackTrace();}
		}
		
		public void stopL() {this.running = false;}
		
	}
	
	public class ModuleException extends Exception {
		private static final long serialVersionUID = 1L;

		public ModuleException(String errorMessage) {
			super(errorMessage);
		}
	}

}
