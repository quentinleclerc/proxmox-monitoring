package org.ctlv.proxmox.manager;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.json.JSONException;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {

    private ProxmoxAPI api;

    public Controller(ProxmoxAPI api) {
        this.api = api;
    }

    public Map<String, List<LXC>> findMyCTs() throws LoginException, JSONException, IOException {
        Map<String, List<LXC>> result = new HashMap<String, List<LXC>>();

        String[] servers = new String[] {
                Constants.SERVER1,
                Constants.SERVER2
        };

        for (String server : servers) {
            List<LXC> allCTs = api.getCTs(server);
            List<LXC> myCTs = new ArrayList<>();
            for (LXC ct : allCTs) {
                if (ct.getName().startsWith(Constants.CT_BASE_NAME)) {
                    myCTs.add(ct);
                }
            }
            result.put(server, myCTs);
        }
        return result;
    }
    
    public void migrate(String node1, String node2) throws LoginException, JSONException, IOException, InterruptedException {
        Map<String, List<LXC>> cts = findMyCTs();
        LXC ct = cts.get(node1).get(0);
        String vmid = ct.getVmid();

        if (ct.getStatus().equals("running")) {
            api.stopCT(node1, vmid);
        }
        Thread.sleep(5000);
        api.migrateCT(node1, vmid, node2);
        Thread.sleep(Constants.GENERATION_WAIT_TIME*1000);
        api.startCT(node2, vmid);
    }
    
    public void stopOldestCT(String node) throws LoginException, JSONException, IOException {
    	List<LXC> cts = findMyCTs().get(node);
    	String oldestCT = null;
    	long CTlifeTime = Long.MIN_VALUE;
    	for(LXC ct : cts) {
    		if(ct.getUptime() > CTlifeTime) {
    			CTlifeTime = ct.getUptime();
    			oldestCT = ct.getVmid();
    		}
    	}
    	if(oldestCT != null) {
    		api.stopCT(node, oldestCT);
    	}
    }

}
