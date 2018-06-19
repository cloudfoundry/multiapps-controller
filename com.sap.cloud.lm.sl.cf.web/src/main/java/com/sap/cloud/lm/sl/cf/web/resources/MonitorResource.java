package com.sap.cloud.lm.sl.cf.web.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.CloudFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.monitoring.FssMonitor;
import com.sap.cloud.lm.sl.cf.web.configuration.service.FileSystemServiceInfo;
import com.sap.cloud.lm.sl.persistence.services.FileSystemFileService;

@Path("/monitor")
@Component
public class MonitorResource {
	
	@Autowired(required = false)
	private FileSystemFileService fileSystemFileService;

	@GET
	@Path("/fssFreeSpace")
	public Response checkFssFreeSpace() {
		if(fileSystemFileService == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		
		FileSystemServiceInfo fileSystemServiceInfo = (FileSystemServiceInfo) new CloudFactory().getCloud()
                .getServiceInfo("deploy-service-fss");
		String storagePath = fileSystemServiceInfo.getStoragePath();
		double freeSpace = FssMonitor.calculateFreeSpace(storagePath);
		return Response.ok().entity(freeSpace).build();
	}
}
