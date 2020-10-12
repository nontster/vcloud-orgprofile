package co.nontster.vcloud.profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.vmware.vcloud.api.rest.schema.GatewayNatRuleType;
import com.vmware.vcloud.api.rest.schema.NatRuleType;
import com.vmware.vcloud.api.rest.schema.NatServiceType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.QueryResultAdminVMRecordType;
import com.vmware.vcloud.sdk.Expression;
import com.vmware.vcloud.sdk.Filter;
import com.vmware.vcloud.sdk.QueryParams;
import com.vmware.vcloud.sdk.QueryService;
import com.vmware.vcloud.sdk.RecordResult;
import com.vmware.vcloud.sdk.ReferenceResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.AdminVdc;
import com.vmware.vcloud.sdk.admin.EdgeGateway;
import com.vmware.vcloud.sdk.admin.ProviderVdc;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.admin.extensions.VcloudAdminExtension;
import com.vmware.vcloud.sdk.constants.query.ExpressionType;
import com.vmware.vcloud.sdk.constants.query.FilterType;
import com.vmware.vcloud.sdk.constants.query.QueryAdminVMField;
import com.vmware.vcloud.sdk.constants.query.QueryRecordType;
import com.vmware.vcloud.sdk.constants.query.QueryVMField;

public class ReportUtils {
	private static XSSFWorkbook workbook;
	private static FileOutputStream out;
	private static VcloudAdminExtension adminExtension;
	private static QueryService queryService;
	
	static void generateReport(VcloudClient client, VcloudAdmin admin, String fileName)
			throws VCloudException, IOException {

		// Getting the VcloudAdminExtension
		adminExtension = client.getVcloudAdminExtension();
		
		// Getting the Admin Extension Query Service.
		queryService = adminExtension.getExtensionQueryService();
		
		System.out.println("Generating report...");
		
		workbook = new XSSFWorkbook();

		XSSFSheet custSheet = workbook.createSheet(" Customers Info ");

		// This data needs to be written (Object[])
		Map<Integer, Object[]> custInfo = new TreeMap<>();

		custInfo.put(1,
				new Object[] { "Organization", "Org. filter", "Org. full name", "Org. Description","Organization VDC", "vApp", "VM", "vCPU","GHz/vCPU","Memory (GB)","Storage (GB)", "OS", "VM Status","VM Description","Created","Host","DataStore","Snapshot", "PVDC", "Farm", "Private IP", " DNAT Private/Public IP Map"});
		

		int j = 2;

		Iterator<ReferenceType> orgRefIter = admin.getAdminOrgRefs().iterator();
		AdminOrganization adminOrg = null;
		AdminVdc adminVdc = null;

		int loopCount = 0;
		
		while (orgRefIter.hasNext()) {
			int orgCount = 0;
			
			ReferenceType orgRef = orgRefIter.next();
			
			// Skip organize AIS_OPERATION
			if(orgRef.getName().equalsIgnoreCase("AIS_OPERATION"))
				continue;
			
			System.out.println(orgRef.getName());
			//System.out.println(orgRef.getHref());
			
			adminOrg = AdminOrganization.getAdminOrgByReference(client, orgRef);
			String orgDesc = adminOrg.getResource().getDescription();
			
			Iterator<ReferenceType> vdcIter = adminOrg.getResource().getVdcs().getVdc().iterator();

			while (vdcIter.hasNext()) {
				int vdcCount = 0;
				
				ReferenceType vdcRef = vdcIter.next();
				adminVdc = AdminVdc.getAdminVdcByReference(client, vdcRef);
				
				Long cpuMhz = adminVdc.getResource().getVCpuInMhz();
				
				ReferenceType providerVdcRef = adminVdc.getProviderVdcRef();
				ProviderVdc pvdc = ProviderVdc.getProviderVdcByReference(client, providerVdcRef);
				
				// Edge Gateway
				HashMap<String, String> dNatMap = new HashMap<String, String>();
				ReferenceResult refResult = adminVdc.getEdgeGatewayRefs();

				Iterator <ReferenceType> refTypeIter = refResult.getReferences().iterator();
				String edgeName = null;
				String edgeSize = null;
				
				while(refTypeIter.hasNext()){
					ReferenceType refType = refTypeIter.next();
					EdgeGateway edgeGateway = EdgeGateway.getEdgeGatewayByReference(client, refType);
					
					NatServiceType natService = NetworkUtils.getNatService(edgeGateway);
					
					if(natService == null)
						break;
					
					Iterator <NatRuleType> natRuleTypeIter = natService.getNatRule().iterator();
					
					while(natRuleTypeIter.hasNext()){
						NatRuleType natRuleType = natRuleTypeIter.next();
						
						String natDescription = (natRuleType.getDescription() != null)? natRuleType.getDescription() : "";
						String interfaceName = null;
						String originalIp = null;
						String translatedIp = null;
						String originalPort = null;
						String translatedPort = null;
						String protocol = null; 
						
						if (natRuleType.getGatewayNatRule() != null){
							
							GatewayNatRuleType gwNatRule = natRuleType.getGatewayNatRule();
							originalIp = gwNatRule.getOriginalIp();
							translatedIp = gwNatRule.getTranslatedIp();
							
							if(natRuleType.getRuleType().equalsIgnoreCase("DNAT")){
								dNatMap.put(translatedIp, originalIp);
							}
						}
						
					}
				}
				
				// vApp
				Iterator<ReferenceType> vappRefIter = adminVdc.getVappRefs().iterator();
				while (vappRefIter.hasNext()) {
					int vappCount = 0;
					
					ReferenceType vappRef = vappRefIter.next();
					Vapp vApp = Vapp.getVappByReference(client, vappRef);
					Iterator<VM> vmIter = vApp.getChildrenVms().iterator();

					while (vmIter.hasNext()) {
						int vmCount = 0;
						
						VM vm = vmIter.next();

						List<String> vmIPList = new ArrayList<String>();
						vm.getIpAddressesById().entrySet().forEach(entry -> {
							vmIPList.add(entry.getValue());
						});
						
						HashMap<String, String> validDNatMap = new HashMap<String, String>();
						for (String vmIP : vmIPList) {
							String key = NetworkUtils.getKeyByValue(dNatMap, vmIP);
							if (key != null)
								validDNatMap.put(key, dNatMap.get(key));
							else
								validDNatMap.put(vmIP, null);
						}
						
						StringBuffer ipStr = new StringBuffer();
						validDNatMap.forEach((k, v) -> {
							if (v != null)
								ipStr.append(k + "←" + v + ", ");
//							else
//								ipStr.append(k + ", ");
						});					
												
						String farmName = null;
						String serverName = null;
						String dataStoreName = null;
						
						QueryParams<QueryAdminVMField> vmQueryParams = new QueryParams<QueryAdminVMField>();
						vmQueryParams.setPageSize(1024);
						
						Expression adminVMExpression = new Expression(
								QueryVMField.ID, vm.getReference().getId(),
								ExpressionType.EQUALS);

						List<Filter> filters = new ArrayList<Filter>();
						filters.add(new Filter(adminVMExpression));
						vmQueryParams.setFilter(new Filter(filters, FilterType.AND)); 
						
						RecordResult<QueryResultAdminVMRecordType> vmRecordResult = queryService.queryRecords(QueryRecordType.ADMINVM, vmQueryParams);
						
						QueryResultAdminVMRecordType vmRecord = vmRecordResult.getRecords().get(0);
						serverName = vmRecord.getHostName();
						farmName = (serverName.matches("^pvcresx.*"))? "IBM":"SimpliVity"; 
						dataStoreName = vmRecord.getDatastoreName();
						
						BigInteger totalVMDisk = BigInteger.ZERO;
						List <VirtualDisk> vmDisks = vm.getDisks();
						
						for(VirtualDisk virtualDisk : vmDisks ){
							if(virtualDisk.isHardDisk())
								totalVMDisk = totalVMDisk.add(virtualDisk.getHardDiskSize());
						}
						
						SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm");
						String createdDate = sdf.format(vm.getResource().getDateCreated().toGregorianCalendar().getTime());
						
					
						String snapshotCreatedDate = (Optional.ofNullable(vm.getSnapshotSection().getSnapshot()).isPresent())? sdf.format(vm.getSnapshotSection().getSnapshot().getCreated().toGregorianCalendar().getTime()):"";

						
						custInfo.put(j++,
								new Object[] { (orgCount == 0)? orgRef.getName(): "", 
												orgRef.getName(),
												(orgCount == 0)? adminOrg.getResource().getFullName():"",
										       (orgCount == 0)? orgDesc:"",
										       (vdcCount == 0)? vdcRef.getName():"", 
											   (vappCount == 0)? vappRef.getName():"",
										       (vmCount == 0)? vm.getResource().getName():"", 
										       String.valueOf(vm.getCpu().getNoOfCpus()),	
										       String.valueOf(cpuMhz/1000),
										       String.valueOf(vm.getMemory().getMemorySize().divide(BigInteger.valueOf(1024))),
										       String.valueOf(totalVMDisk.divide(BigInteger.valueOf(1024))),
										       vm.getOperatingSystemSection().getDescription().getValue(),
										       vm.getVMStatus().toString(),
										       vm.getResource().getDescription(),
										       createdDate,
										       serverName,
										       dataStoreName,
										       snapshotCreatedDate,
										       pvdc.getResource().getName(),
										       farmName,
										       vmIPList.toString().replace("[", "").replace("]", ""), 
										       (ipStr != null)? ipStr.toString().replaceAll(", $", ""):"",});
						
						orgCount++;
						vdcCount++;
						vappCount++;
						vmCount++;
					}

				}
				
			}
			
			++loopCount; 
			//if(loopCount == 4) break;
		}

		// Create style
		CellStyle style = workbook.createCellStyle();
		CellStyle styleHead = workbook.createCellStyle();

		// Create font
		Font font = workbook.createFont();
		// Make font bold
		font.setBold(true);
		// set it to bold
		style.setFont(font);
		style.setAlignment(HorizontalAlignment.RIGHT);

		// Set style for header
		styleHead.setFont(font);
		styleHead.setAlignment(HorizontalAlignment.CENTER);

		// Iterate over data and write to sheet
		Set<Integer> keyid = custInfo.keySet();

		// Create row object
		XSSFRow nrow;
		int rowid = 0;
		for (Integer key : keyid) {
			nrow = custSheet.createRow(rowid++);
			Object[] objectArr = custInfo.get(key);
			int cellid = 0;
			for (Object obj : objectArr) {
				Cell cell = nrow.createCell(cellid++);

				if (rowid == 1) {
					cell.setCellStyle(styleHead);
					cell.setCellValue((String) obj);
				} else if(cellid == 8 || cellid == 9 || cellid == 10 || cellid == 11) {
					String cellStr = (String) obj;
					cell.setCellValue(Integer.parseInt(cellStr));
				} else
					cell.setCellValue((String) obj);				
			}
		}

		// Auto size all columns
		for(int i = 0; i < custSheet.getRow(0).getPhysicalNumberOfCells(); i++)
			custSheet.autoSizeColumn(i);	

		// Add auto filter to sheet
		custSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, custSheet.getRow(0).getPhysicalNumberOfCells()-1));

		out = new FileOutputStream(new File(fileName));
		workbook.write(out);
		out.close();

		System.out.println("Done!");
	}

}
