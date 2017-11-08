package org.ctlv.proxmox.tester;

import java.io.IOException;
import java.util.*;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.ctlv.proxmox.api.data.Node;
import org.json.JSONException;

public class Main {

	public static void main(String[] args) throws LoginException, JSONException, IOException {

		ProxmoxAPI api = new ProxmoxAPI();		
		
		// api.createCT("srv-px3", "11720", "ct-tpgei-ctlv-A17-ct20", 512);
		// Thread.sleep(1000);
		// api.startCT("srv-px3", "11720");

		List<String> srvs = api.getNodes();
		for(String srvName : srvs) {
			Node srv = api.getNode(srvName);
						
			System.out.println("server name: " + srvName);
			List<LXC> cts = api.getCTs(srvName);
			for (LXC lxc : cts) {
				float percentCpu = lxc.getCpu();
				float percentDisk = ((float)lxc.getDisk()/(float)lxc.getMaxdisk()) * 100;
				float percentMemory = ((float)lxc.getMem()/(float)lxc.getMaxmem()) * 100;
				
				System.out.println("ct name: " +  lxc.getName() + " | status : " + lxc.getStatus() + " | cpu%: " + percentCpu + " | disk%: " + percentDisk + " | memory%: " + percentMemory);
				System.out.println(lxc.getMem());
			}
			System.out.println();
		}



		Random rndRAM = new Random(new Date().getTime());
		for (int i = 0; i<10; i++) {
			System.out.println(rndRAM.nextInt(3));
		}

	}

}
