package org.ctlv.proxmox.manager;

import org.ctlv.proxmox.api.ProxmoxAPI;

public class ManagerMain {
	
	public static void main(String[] args) {
		Thread monitorThread = new Thread(new Monitor(new ProxmoxAPI()));
		monitorThread.start();
	}
}
