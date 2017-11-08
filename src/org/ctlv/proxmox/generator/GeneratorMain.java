package org.ctlv.proxmox.generator;

import java.io.IOException;
import java.util.*;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.ctlv.proxmox.manager.Controller;
import org.json.JSONException;

public class GeneratorMain {
	
	private static Random rndTime = new Random(new Date().getTime());

	private static int getNextEventPeriodic(int period) {
		return period*1000*60;
	}

	public static int getNextEventUniform(int max) {
		return rndTime.nextInt(max);
	}

	public static int getNextEventExponential(int inv_lambda) {
		float next = (float) (- Math.log(rndTime.nextFloat()) * inv_lambda);
		return (int)next;
	}
	
	public static void main(String[] args) throws InterruptedException, LoginException, JSONException, IOException {
		
		ProxmoxAPI api = new ProxmoxAPI();
		Controller controller = new Controller(api);

		long currentCtIndex = 0;

		Map<String, List<LXC>> myCTsPerServer = controller.findMyCTs();

		Random rndServer = new Random(new Date().getTime());
		Random rndRAM = new Random(new Date().getTime()); 
		
		long memAllowedOnServer1 = (long) (api.getNode(Constants.SERVER1).getMemory_total() * Constants.MAX_THRESHOLD);
		long memAllowedOnServer2 = (long) (api.getNode(Constants.SERVER2).getMemory_total() * Constants.MAX_THRESHOLD);
		
		while (true) {
			
			// amout of used memory on each server
			long memOnServer1 = 0;
			for (LXC lxc : myCTsPerServer.get(Constants.SERVER1)) {
				memOnServer1 += lxc.getMem();
			}

			long memOnServer2 = 0;
			for (LXC lxc : myCTsPerServer.get(Constants.SERVER2)) {
				memOnServer2 += lxc.getMem();
			}

			int rdnRamIndex = rndRAM.nextInt(3);
			if ( (memOnServer1 + Constants.RAM_SIZE[rdnRamIndex]) < memAllowedOnServer1
					&& (memOnServer2 + Constants.RAM_SIZE[rdnRamIndex]) < memAllowedOnServer2) {
				// choose a random server with specified ratio 66% vs 33%
				String serverName;
				if (rndServer.nextFloat() < Constants.CT_CREATION_RATIO_ON_SERVER1) {
					serverName = Constants.SERVER1;
				}
				else {
					serverName = Constants.SERVER2;
				}

				// check if a CT with the same name already exists on server
				String ctName = Constants.CT_BASE_NAME + currentCtIndex;
				List<String> cts = api.getCTList(serverName);

				while (cts.contains(ctName)) {
					currentCtIndex++;
					ctName = Constants.CT_BASE_NAME + currentCtIndex;
				}

				String ctID = Long.toString((Constants.CT_BASE_ID + currentCtIndex));

				api.createCT(serverName, ctID, ctName, Constants.RAM_SIZE[rdnRamIndex]);
				Thread.sleep(Constants.GENERATION_WAIT_TIME*1000);
				api.startCT(serverName, ctID);

				LXC ctCreated = api.getCT(serverName, ctID);
				myCTsPerServer.get(serverName).add(ctCreated);

				// planify next creation
				int timeToWait = getNextEventPeriodic(1);
				
				// wait until next event
				currentCtIndex++;
				Thread.sleep(timeToWait);
			}
			else {
				System.out.println("Servers are loaded, waiting some more seconds ...");
				Thread.sleep(Constants.GENERATION_WAIT_TIME * 1000);
			}
		}
	}

}
