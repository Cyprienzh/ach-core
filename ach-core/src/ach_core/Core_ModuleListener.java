package ach_core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.ArrayList;

import javax.net.ssl.SSLSocket;

import org.json.JSONException;
import org.json.JSONObject;

public class Core_ModuleListener extends Thread{
	
	private SSLSocket module_socket;
	private int module_state;
	private String module_name;
	private PrintWriter os;
	private BufferedReader is;
	private boolean running;
	
	public final static int UNIDENTIFIED = 0;
	public final static int IDENTIFIED = 1;
	public final static int DISCONNECTED = 2;
	
	public static ArrayList<Core_ModuleListener> modules_list = new ArrayList<Core_ModuleListener>();
	
	public Core_ModuleListener(SSLSocket pSocket) {
		this.module_socket = pSocket;
		this.module_state = UNIDENTIFIED;
		this.module_name = "";
	}
	
	public void run() {
		try {
			//OUVERTURE DES FLUX IN ET OUT
			this.os = new PrintWriter(new OutputStreamWriter(this.module_socket.getOutputStream()));
			this.is = new BufferedReader(new InputStreamReader(this.module_socket.getInputStream()));
			String tmp_message;
			
			while(this.module_state != DISCONNECTED) {
				tmp_message = is.readLine();
				try {
					JSONObject json_message = new JSONObject(tmp_message);
					
					switch(json_message.getString("type")) {
					
					//PACKET MESSAGE_MODULE
					case "packet.message_module" :
						Core_ModuleListener mod = getListenerByName(json_message.getString("message_module.dest"));
						if(mod != null) {
							mod.os.println(json_message.getString("message_module.content"));
							mod.os.flush();
						}
						else {sendError("error.message","no destination module named "+json_message.getString("message_module.dest"));}
						break;
					
					//PAQUET IDENTITE
					case "packet.identity" :
						Core_ModuleManager module = Core_ModuleManager.getModuleByName(json_message.getString("identity.module_name"));
						if(module != null) {
							if(module.getKey().equals(json_message.getString("identity.module_key"))) {
								this.module_state = IDENTIFIED;
								this.module_name = module.getName();
								modules_list.add(this);
								sendAck();
								Core.log_("Liaison du module "+module.getName()+" avec le noyau établie", "INFOS");
								}
							else {sendError("error.auth","wrong module key");}
						}
						else {sendError("error.auth","unknown module");}
						break;
					
					//PAQUET LOG
					case "packet.log" :
						if(this.module_state == IDENTIFIED) {Core.log_("("+this.module_name+") "+json_message.getString("log.message"), "MODULE");}
						break;
						
					}
				}
				catch(JSONException e) {e.printStackTrace();}
			}
		}
		catch(IOException e) {modules_list.remove(this);}
	}
	
	public static Core_ModuleListener getListenerByName(String pName) {
		for(Core_ModuleListener module : modules_list) {
			if(module.module_name.equals(pName)) {return module;}
		}
		return null;
	}
	
	public void closeConnection() {
		this.module_state = DISCONNECTED;
		try {this.module_socket.close();}
		catch(IOException e) {e.printStackTrace();}
	}
	
	public void sendData(String json_tmp) {
		this.os.println(json_tmp);
		this.os.flush();
	}
	
	private void sendError(String type_error, String reason_error) {
		JSONObject tmp_json = new JSONObject();
		tmp_json.put("type", "packet.error");
		tmp_json.put("error.type", type_error);
		tmp_json.put("error.reason", reason_error);
		
		this.os.println(tmp_json);
		this.os.flush();
	}
	
	private void sendAck() {
		JSONObject tmp_json = new JSONObject();
		tmp_json.put("type", "packet.ack");
		
		this.os.println(tmp_json);
		this.os.flush();
	}

}
