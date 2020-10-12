package co.nontster.vcloud.profile;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.vmware.vcloud.model.VCloudOrganization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.admin.AdminVdc;
import com.vmware.vcloud.sdk.admin.EdgeGateway;
import com.vmware.vcloud.sdk.admin.VcloudAdmin;
import com.vmware.vcloud.sdk.constants.Version;


public class CloudProfile 
{
	private static VcloudClient client;
	private static VcloudAdmin admin;

	private static VCloudOrganization vCloudOrg;
	private static AdminVdc adminVdc;
	private static EdgeGateway edgeGateway;

	private static String vcdurl;
	private static String username;
	private static String password;

	private static Properties prop = new Properties();
	
    public static void main( String[] args )
    {
		Options options = new Options();
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new DefaultParser();
		Console cnsl = null;	
		
		Option optHelp = Option.builder("h").longOpt("help").desc("print usage").hasArg(false).required(false)
				.argName("help").build();

		Option optVcdurl = Option.builder("l").longOpt("vcdurl").desc("vCloud Director public URL").hasArg(true)
				.required(false).argName("url").build();

		Option optUsername = Option.builder("u").longOpt("user").desc("username").hasArg(true).required(true)
				.argName("username").build();

		Option optPassword = Option.builder("p").longOpt("password").desc("password").hasArg(true).required(false)
				.argName("password").build();

		options.addOption(optVcdurl);
		options.addOption(optUsername);
		options.addOption(optPassword);
		options.addOption(optHelp);

		if (args.length < 1) {
			System.err.println("Type CloudProfile -h or --help for usage.");
			return;
		} else if(args[0].equals("-h") || args[0].equals("--help")){
			formatter.printHelp("cloudprofile", options); 
			return;
		}
		
		try {
			// load a properties file
			File file = new File(System.getProperty("lazybee.confdir")+"config.properties");
				
			// If config.properties in conf folder not found find another from classpath
			if(file.exists() && !file.isDirectory()) { 
				System.out.println("Loading configuration from conf folder");
				FileInputStream fs = new FileInputStream(file);	
				prop.load(fs);
			} else {
				System.out.println("Loading configuration from classpath");
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				InputStream input = classLoader.getResourceAsStream("config.properties");
				prop.load(input);
			}
									
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("vcdurl")) {
				vcdurl = cmd.getOptionValue("vcdurl");
			}

			if (cmd.hasOption("user")) {
				username = cmd.getOptionValue("user");

				int index = username.indexOf('@');

				if (index < 0)
					username += "@System";
			}

			if (cmd.hasOption("password")) {
				password = cmd.getOptionValue("password");
			}

			if (vcdurl == null) {
				vcdurl = prop.getProperty("url");
			}

			if (password == null) {
				// creates a console object
				cnsl = System.console();

				// if console is not null
				if (cnsl != null) {
					// read password into the char array
					char[] pwd = cnsl.readPassword("Enter your password: ");
					password = new String(pwd);
				}
			}


			VcloudClient.setLogLevel(Level.OFF);
			System.out.println("vCloud Login");
			client = new VcloudClient(vcdurl, Version.V5_5);

			client.login(username, password);
			System.out.println("	Login Success\n");

			System.out.println("Get vCloud Admin");
			admin = client.getVcloudAdmin();
			System.out.println("	" + admin.getResource().getHref() + "\n");
			
			System.out.println(admin.getAdminOrgRefs().size()+ " organizations");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			Date now = new Date();
			
			ReportUtils.generateReport(client, admin, sdf.format(now)+"_vcloud_vm_list.xlsx");

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("File not found: " + e.getMessage());
		} catch (VCloudException e) {
			// TODO Auto-generated catch block
			if(e.getMessage().equalsIgnoreCase("Unauthorized"))
				System.err.println("Invalid username or password");
			else{
				System.err.println("vCloud exception: \n" + e.getMessage());
				e.printStackTrace();
				} 
		} 
		
    }
}
