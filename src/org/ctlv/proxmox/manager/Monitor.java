package org.ctlv.proxmox.manager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.ctlv.proxmox.api.data.Node;
import org.json.JSONException;

public class Monitor implements Runnable {

	private ProxmoxAPI api;
	private Controller controller;
	
	public Monitor(ProxmoxAPI api) {
		this.api = api;
		this.controller = new Controller(api);
	}
	
	@Override
	public void run() {
		
		while(true) {
			
			try {
				Map<String, List<LXC>> myCTs = controller.findMyCTs();
				Node server1 = api.getNode(Constants.SERVER1);
				Node server2 = api.getNode(Constants.SERVER2);

				long migrationMemServer1 = (long) (server1.getMemory_total() * Constants.MIGRATION_THRESHOLD);
				long migrationMemServer2 = (long) (server2.getMemory_total() * Constants.MIGRATION_THRESHOLD);
				long droppingMemServer1 = (long) (server1.getMemory_total() * Constants.DROPPING_THRESHOLD);
				long droppingMemServer2 = (long) (server2.getMemory_total() * Constants.DROPPING_THRESHOLD);

				long maxMemServer1 = server1.getMemory_total();
				long maxMemServer2 = server2.getMemory_total();

				long memServer1 = 0;
				for(LXC ct : myCTs.get(Constants.SERVER1)) {
					memServer1 += ct.getMem();
				}
				
				long memServer2 = 0;
				for(LXC ct : myCTs.get(Constants.SERVER2)) {
					memServer2 += ct.getMem();
				}

				float percentUsedMemSrv1 = ((float) memServer1 / (float) maxMemServer1) * 100;
				float percentUsedMemSrv2 = ((float) memServer2 / (float) maxMemServer2) * 100;

				System.out.println(Constants.SERVER1 + ": ");
				System.out.println("used: " + percentUsedMemSrv1 + "%");
				System.out.println(Constants.SERVER2 + ": ");
				System.out.println("used: " + percentUsedMemSrv2 + "%");
				System.out.println();

				/* Tests on Server1 */
				// MemServer1 > migration rate && MemServer 2 <= dropping rate : Migration
				if (memServer1 > migrationMemServer1
						&& memServer2 <= migrationMemServer2) {
                    System.out.println("Migrating one CT from " + Constants.SERVER1 + " to " + Constants.SERVER2);
                    controller.migrate(Constants.SERVER1, Constants.SERVER2);
                }
				// MemServer1 et MemServer2 > dropping rate : Arrêt CT
				else if(memServer1 > droppingMemServer1 && memServer2 > droppingMemServer2) {
                    System.out.println("Stopping oldest CT on " + Constants.SERVER1);
                    controller.stopOldestCT(Constants.SERVER1);
				}
				
				/* Tests on Server2 */
				// MemServer2 > migration rate % && MemServer1 < migration rate : Migration
				if(memServer2 >= migrationMemServer2 && memServer1 < migrationMemServer1) {
                    System.out.println("Migrating one CT from " + Constants.SERVER2 + " to " + Constants.SERVER1);
                    controller.migrate(Constants.SERVER2, Constants.SERVER1);
				}
				// MemServer2 et MemServer1 > dropping rate : Arrêt CT
				else if(memServer2 > droppingMemServer2 && memServer1 > droppingMemServer1) {
                    System.out.println("Stopping oldest CT on " + Constants.SERVER2);
                    controller.stopOldestCT(Constants.SERVER2);
				}
				Thread.sleep(Constants.MONITOR_PERIOD*1000);
			} catch (IOException | InterruptedException | JSONException | LoginException e) {
				e.printStackTrace();
			}			
		}
	}

}
