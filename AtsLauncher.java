import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor.Version;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AtsLauncher {

	/**
	 * This script is a Java class used as a simple script file with Java >= 20
	 * It will will try to update ATS tools from https://actiontestscript.org server and it will launch ATS suite tests using ATS components downloaded or already installed
	 * <p>
	 * Available options for launching ATS suites :
	 * <p>
	 * 'clean' : Clean all downloaded ATS components (libs + drivers) already installed on current system
	 * 'prepareMaven' : Prepare 'build.properties' file that maven can use to find ATS tools for ATS tests executions
	 * 'buildEnvironment' : Only try to get ATS tools path and create 'build.properties' file that can be used by Maven launch test process
	 * 'suiteXmlFiles' : Comma separated names of ATS suites xml files in 'exec' folder of current project, to be launched by this script
	 *   exemple :
	 *       suiteXmlfiles=suite  -> will launch 'suite.xml' file in 'exec' folder of current project
	 *       suiteXmlfiles=suite1,suite2,suite3 -> will launch 'suite1.xml', 'suite2.xml' and 'suite3.xml' files in 'exec' folder of current project*
	 * 'atsReport' : Report details level
	 *     1 - Simple execution report
	 *     2 - Detailed execution report
	 *     3 - Detailed execution report with screen-shot
	 * 'validationReport' : Generate proof of functional execution with screen-shot
	 *   exemple :
	 *   validationReport=true -> will generate proof of functional execution with screen-shot
	 *   validationReport=false -> will not generate proof of functional execution with screen-shot (default value)
	 * 'atsListScripts' : List of ats scripts that can be launched using a temp suite execution
	 * 'tempSuiteName' : If 'atsListScripts' option is defined this option override default suite name ('tempSuite')
	 * 'disableSsl' : Disable trust certificat check when using ActionTestScript tools server
	 * 'atsUrl' : Alternative url path to ActionTestScript server. directory schemas : exemple : localhost:8080/ats
	 *            /releases
	 *               -> ats-libs  : [version].zip
	 *               -> ats-drivers
	 *                   -> linux
	 *                       -> system : [version].tgz
	 *                   -> windows
	 *                       -> system : [version].zip
	 *            /tools
	 *               -> jdk
	 *                   -> linux : [version].tgz
	 *                   -> windows : [version].zip
	 *
	 * 'atsToolsUrl' : Alternative url path to ActionTestScript tools server. directory schemas : exemple : localhost:8080/ats/tools
	 *               /jdk
	 *                   -> linux : [version].tgz
	 *                   -> windows : [version].zip
	 *
	 * 'jenkinsUrl' : Url of a Jenkins server with saved ATS tools archives, tools will be available at [Jenkins_Url_Server]/userContent/tools using 'version.csv' files with names, versions and path of ATS tools
	 * 'reportsDirectory' (or 'output') : This is the output folder for all files generated during execution of ATS tests suites
	 * 'outbound' : By default, this script will try to contact ActionTestScript tools server.
	 *
	 * 'systemdriverurl' or (system-driver-url) Url or IP address of a remote system driver server with or without port. ex systemdriverurl=192.168.0.1:9700
	 *
	 * In priority, this script will try to find ATS tools on local installation using following ordered methods :
	 * - 'atsToolsFolder' property in command line. exemple : (linux) atsToolsFolder=/home/user/ats/tools || (windows) atsToolsFolder=C:/ats/tools
	 * - 'ATS_TOOLS' environment variable set on current machine : (linux) export ATS_TOOLS=/home/user/ats/tools || (windows) set ATS_TOOLS=C:/ats/tools
	 * - 'atsToolsFolder' property in '.atsProjectProperties' file in current project folder
	 * default location : (windows) %appdata%/roaming/ats/tools || (linux) ~/ats/tools
	 *
	 * In priority, this script will try to find ATS on local installation using following ordered methods :
	 * - 'ATS_PATH' environment variable set on current machine : (linux) export ATS_PATH=/home/user/ats || (windows) set ATS_TOOLS=C:/ats
	 * the strucutre directory d'ATS_PATH must be :
	 * 	/libs
	 * 		-> unzip ats-libs
	 * 	/drivers
	 * 		-> unzip ats-drivers (system)
	 *
	 * In priority, this script will try to find ATS and ATS Tools on local installation using following ordered methods :
	 * - 'atsFolder' property in command line. exemple : (linux) atsToolsFolder=/home/user/ats || (windows) atsToolsFolder=C:/ats
	 * atsFolder is priority on ATS_PATH and ATS_TOOLS and atsToolsFolder
	 *  the strucutre directory d'ATS_PATH must be :
	 * 	 /libs
	 * 	  	-> unzip ats-libs
	 * 	 /drivers
	 * 	 	-> unzip ats-drivers (system)
	 * 	 /tools
	 * 	 	-> jdk	: files jdk
	 * default location : (windows) %appdata%/roaming/ats/cache/<ATS_VERSION> || (linux) ~/ats/cache/<ATS_VERSION>
	 * <p>
	 * About '.atsProjectProperties' file in current project folder in xml format :
	 * - if tag 'atsToolsFolder' found : the value will define the local folder path of ATS tools
	 * - if tag 'atsToolsUrl' found : the standard ATS tools url server will be overwritten
	 * - if tag 'outbound' found : if the value is false, off or 0, no request will be send to get ATS tools
	 *
	 * In order to override environment variables used in the ATS project you can use '-A:' prefix
	 * example to override 'web-browser' variable defined in the suite 'demo', this is the command line : java AtsLauncher.java suite=demo -A:web-browser=chrome
	 *
	 *
	 *------------------------------------------------------------------------------------------------------------
	 * Versions and urls of ATS components
	 *------------------------------------------------------------------------------------------------------------
	 * ATS library version 			: 3.2.7
	 * ATS system driver version 	: 1.8.2
	 * https://actiontestscript.org/releases/ats-libs/3.2.7.zip
	 * https://actiontestscript.org/releases/ats-drivers/windows/system/1.8.2.zip
	 * https://actiontestscript.org/releases/ats-drivers/linux/system/1.8.2.tgz
	 *------------------------------------------------------------------------------------------------------------
	 */

	//------------------------------------------------------------------------------------------------------------
	// Statics variables
	//------------------------------------------------------------------------------------------------------------

	private static final String ATS_SERVER = "https://actiontestscript.org";

	private static final String ATS_LAUNCHER_VERSION = "2.7.9";
	private static final String DEFAULT_ATS_VERSION = "3.2.7";
	private static final String OS_TAG = "#OS#";
	
	private static final String ATS_VERSION = System.getenv("ATS_VERSION");
	private static final String OS = System.getProperty("os.name").toLowerCase();

	private static final int MINIMUM_JAVA_JDK_VERSION = 15;

	private static final String ATS_RELEASES_DIRECTORY_SERVER =  "/releases";

	private static final String ATS_TOOLS_DIRECTORY_SERVER = "/tools/";
	private static final String ATS_JENKINS_TOOLS = "userContent/tools/versions.csv";

	private static final String TARGET = "target";
	private static final String SRC_EXEC = "src/exec";
	private static final String ATS_OUTPUT = "ats-output";
	private static final String BUILD_PROPERTIES = "build.properties";
	private static final String ATS_PROJECT_PROPERTIES = ".atsProjectProperties";

	private static final String LINUX = "linux";
	private static final String WINDOWS = "windows";
	private static final String MACOS = "macos";
	private static final String MACOS_AARCH64 = "macos_arm";
	private static final String MACOS_X86_64 = "macos";
	private static final String ARCH_INTEL = "x86_64";
	private static final String ARCH_ARM = "aarch64";

	private static final String LINUX_DRIVER_NAME = "linuxdriver";
	private static final String MACOS_DRIVER_NAME = "macosdriver";

	private static final String TGZ = "tgz";
	private static final String ZIP = "zip";

	private static final String ATS = "ats";
	private static final String JDK = "jdk";

	private static final List<String> TRUE_LIST = Arrays.asList(new String[]{"on", "true", "1", "yes", "y"});
	private static final List<String> FALSE_LIST = Arrays.asList(new String[]{"off", "false", "0", "no", "n"});
	private static final List<String> REPORT_LEVEL_LIST = Arrays.asList(new String[]{"1", "2", "3" });

	//------------------------------------------------------------------------------------------------------------
	// Execution variables
	//------------------------------------------------------------------------------------------------------------

	private static String operatingSystem = LINUX;
	private static String operatingSystemPath = LINUX;
	private static String arch = ARCH_INTEL;

	private static String suiteFiles = "";
	private static String atsScripts = "";
	private static String tempSuiteName = "tempSuite";
	private static String reportLevel = "";
	private static String validationReport = "0";
	private static String systemDriverUrl = "";
	private static String outputBase = TARGET + "/" + ATS_OUTPUT;
	private static String output = null;
	private static boolean asSuiteExecution = true;

	private static String atsToolsFolderProperty = "atsToolsFolder";
	private static String atsToolsUrlProperty = "atsToolsUrl";
	private static String outboundProperty = "outbound";
	private static String disableSSLParam = "disableSSL";

	private static String htmlReportParam = "1";

	private static String atsToolsFolder = null;
	private static Boolean atsToolsFolderIsDefine = false;
	private static String atsUrl = null;
	private static String atsToolsUrl = null;
	private static Proxy proxy = null;

	private static String atsServerReleasesUrl = ATS_SERVER + ATS_RELEASES_DIRECTORY_SERVER;

	private static String atsHomePath = null;
	private static String projectAtsVersion = null;
	private static final String atsEnvHome = System.getenv("ATS_HOME");
	private static String jdkHomePath = null;
	private static String atsFolder = null;
	private static Map<String, String> atsToolsList = new HashMap<String, String>();
	private static ArrayList<AtsToolEnvironment> atsToolsEnvLocal = null;

	private static Map<String, String> atsExecEnv = new HashMap<String, String>();

	private static ArrayList<AtsToolEnvironment> atsToolsEnv = new ArrayList<AtsToolEnvironment>();

	private static String atsHomeInstall = System.getProperty("user.home");
	private static String atsToolsInstall = "/ats/tools";
	private static String atsCacheInstall = "/ats/cache";
	private static String jenkinsToolsUrl = null;

	private static HashMap<String, String> helpMap = new HashMap<>();
	private static Set<PosixFilePermission> posixFilePermission = null;
	private static Path projectFolderPath = null;

	private static final Pattern SYS_VERSION_PATTERN = Pattern.compile("<a href\\s?=\\s?\"([^\"]+\\.(zip|tgz))\">");
	private static String systemDriverVersion = "";

	//------------------------------------------------------------------------------------------------------------
	// Main script execution
	//------------------------------------------------------------------------------------------------------------

	public static void main(String[] args) throws Exception, InterruptedException {
		helpMap.put("general","This script is a Java class used as a simple script file with Java >= " + MINIMUM_JAVA_JDK_VERSION + "\n"+
				"It will will try to update ATS tools from https://actiontestscript.org server and it will launch ATS suite tests\n"+
				"using ATS components downloaded or already installed.\n"+
				"for more detail information <command> help : example java AtsLauncher.java atsReport help\n"+
				"Available options for launching ATS suites :\n" +
				"'clean' : Clean all downloaded ATS components (libs + drivers) already installed on current system\n" +
				"'prepareMaven' : Prepare 'build.properties' file that maven can use to find ATS tools for ATS tests executions\n" +
				"'buildEnvironment' : Only try to get ATS tools path and create 'build.properties' file that can be used by Maven launch test process\n" +
				"'suiteXmlFiles' : Comma separated names of ATS suites xml files in 'exec' folder of current project, to be launched by this script\n" +
				"'atsReport' : Report details level\n" +
				"'validationReport' : Generate proof of functional execution with screen-shot\n" +
				"'atsListScripts' : List of ats scripts that can be launched using a temp suite execution\n" +
				"'tempSuiteName' : If 'atsListScripts' option is defined this option override default suite name ('tempSuite')\n" +
				"'disableSsl' : Disable trust certificat check when using ActionTestScript tools server\n" +
				"'atsUrl' : Alternative url path to ActionTestScript server.\n" +
				"'atsToolsUrl' : Alternative url path to ActionTestScript tools server.\n" +
				"'jenkinsUrl' : Url of a Jenkins server with saved ATS tools archives, tools will be available at [Jenkins_Url_Server]/userContent/tools using 'version.csv' files with names, versions and path of ATS tools\n" +
				"'reportsDirectory' (or 'output') : This is the output folder for all files generated during execution of ATS tests suites\n" +
				"'outbound' : By default, this script will try to contact ActionTestScript tools server.\n" +
				"'systemdriverurl' or (system-driver-url) Url or IP address of a remote system driver server with or without port. ex systemdriverurl=192.168.0.1:9700\n" +
				"'proxyurl' Url or IP address of proxy server without authentication example : proxyurl=http://www.proxy.test:8080 ; proxyurl=https://www.proxy.test:8080 ; proxyurl=http://username:password@www.proxy.test:8080 ;proxyurl=https://username:password@www.proxy.test:8080\n" +
				"\n" +
				"In priority, this script will try to find ATS tools on local installation using following ordered methods :\n" +
				"- 'atsToolsFolder' property in command line. exemple : (linux) atsToolsFolder=/home/user/ats/tools || (windows) atsToolsFolder=C:/ats/tools\n" +
				"- 'ATS_TOOLS' environment variable set on current machine : (linux) export ATS_TOOLS=/home/user/ats/tools || (windows) set ATS_TOOLS=C:/ats/tools\n" +
				"- 'atsToolsFolder' property in '.atsProjectProperties' file in current project folder\n" +
				//				"default location : (windows) %appdata%/roaming/ats/tools || (linux) ~/ats/tools\n" +
				"\n" +
				"In priority, this script will try to find ATS on local installation using following ordered methods :\n" +
				"- 'ATS_PATH' environment variable set on current machine : (linux) export ATS_PATH=/home/user/ats || (windows) set ATS_TOOLS=C:/ats\n" +
				"\n" +
				"About '.atsProjectProperties' file in current project folder in xml format :\n" +
				"- if tag 'atsToolsFolder' found : the value will define the local folder path of ATS tools\n" +
				"- if tag 'atsToolsUrl' found : the standard ATS tools url server will be overwritten\n" +
				"- if tag 'outbound' found : if the value is false, off or 0, no request will be send to get ATS tools");


		helpMap.put("clean", "Clean all downloaded ATS components (libs + drivers) already installed on current system");
		helpMap.put("preparemaven", "Prepare 'build.properties' file that maven can use to find ATS tools for ATS tests executions");
		helpMap.put("buildenvironment", "Only try to get ATS tools path and create 'build.properties' file that can be used by Maven launch test process");
		helpMap.put("suitexmlfiles", "Comma separated names of ATS suites xml files in 'exec' folder of current project, to be launched by this script"+
				"  exemple :\n" +
				"      suiteXmlfiles=suite  -> will launch 'suite.xml' file in 'exec' folder of current project\n" +
				"      suiteXmlfiles=suite1,suite2,suite3 -> will launch 'suite1.xml', 'suite2.xml' and 'suite3.xml' files in 'exec' folder of current project*\n" );
		helpMap.put("atsreport", "Report details level\n" +
				"1 - Simple execution report\n" +
				"2 - Detailed execution report\n" +
				"3 - Detailed execution report with screen-shot");
		helpMap.put("validationreport", "Generate proof of functional execution with screen-shot\n" +
				"exemple :\n" +
				"validationReport=true -> will generate proof of functional execution with screen-shot\n" +
				"validationReport=false -> will not generate proof of functional execution with screen-shot (default value)");
		helpMap.put("atslistscripts", "List of ats scripts that can be launched using a temp suite execution");
		helpMap.put("tempsuitename", "If 'atsListScripts' option is defined this option override default suite name ('tempSuite')");
		helpMap.put("disablessl", "Disable trust certificat check when using ActionTestScript tools server");
		helpMap.put("atsurl", "Alternative url path to ActionTestScript server. directory schemas : example : localhost:8080/ats\n" +
				"           /releases\n" +
				"              -> ats-libs  : [version].zip\n" +
				"              -> ats-drivers\n" +
				"                  -> linux\n" +
				"                      -> system : [version].tgz\n" +
				"                  -> windows\n" +
				"                      -> system : [version].zip\n" +
				"           /tools\n" +
				"              -> jdk\n" +
				"                  -> linux : [version].tgz\n" +
				"                  -> windows : [version].zip\n");
		helpMap.put("atstoolsurl", "Alternative url path to ActionTestScript tools server. directory schemas : exemple : localhost:8080/ats/tools\n" +
				"              /jdk\n" +
				"                  -> linux : [version].tgz\n" +
				"                  -> windows : [version].zip\n");
		helpMap.put("jenkinsurl", "Url of a Jenkins server with saved ATS tools archives, tools will be available at [Jenkins_Url_Server]/userContent/tools using 'version.csv' files with names, versions and path of ATS tools");
		helpMap.put("reportsdirectory", "(or 'output') : This is the output folder for all files generated during execution of ATS tests suites");
		helpMap.put("outbound","By default, this script will try to contact ActionTestScript tools server.");
		helpMap.put("atstoolsfolder","In priority, this script will try to find ATS tools on local installation using following ordered methods :\n" +
				"- 'atsToolsFolder' property in command line. exemple : (linux) atsToolsFolder=/home/user/ats/tools || (windows) atsToolsFolder=C:/ats/tools\n" +
				"- 'ATS_TOOLS' environment variable set on current machine : (linux) export ATS_TOOLS=/home/user/ats/tools || (windows) set ATS_TOOLS=C:/ats/tools\n" +
				"- 'atsToolsFolder' property in '.atsProjectProperties' file in current project folder\n");
		//				"default location : (windows) %appdata%/roaming/ats/tools || (linux) ~/ats/tools");
		helpMap.put("ats_tools","Environment variable set on current machine : (linux) export ATS_PATH=/home/user/ats || (windows) set ATS_TOOLS=C:/ats\n"+
				"the strucutre directory d'ATS_PATH must be :\n" +
				" 	/libs\n" +
				" 		-> unzip ats-libs\n" +
				" 	/drivers\n" +
				" 		-> unzip ats-drivers (system)\n");
		helpMap.put("atsfolder","In priority, this script will try to find ATS and ATS Tools on local installation using following ordered methods :\n" +
				"- 'atsFolder' property in command line. example : (linux) atsToolsFolder=/home/user/ats || (windows) atsToolsFolder=C:/ats\n" +
				"atsFolder is priority on ATS_PATH and ATS_TOOLS and atsToolsFolder\n" +
				"the strucutre directory d'ATS_PATH must be :\n" +
				" 	/libs\n" +
				" 		-> unzip ats-libs\n" +
				" 	/drivers\n" +
				" 		-> unzip ats-drivers (system)\n" +
				" 	/tools\n" +
				" 		-> jdk	: files jdk\n" );
		//				"default location : (windows) %appdata%/roaming/ats/cache/<ATS_VERSION> || (linux) ~/ats/cache/<ATS_VERSION>");
		helpMap.put("xml_atsprojectproperties","About '.atsProjectProperties' file in current project folder in xml format :\n" +
				"- if tag 'atsToolsFolder' found : the value will define the local folder path of ATS tools\n" +
				"- if tag 'atsToolsUrl' found : the standard ATS tools url server will be overwritten\n" +
				"- if tag 'outbound' found : if the value is false, off or 0, no request will be send to get ATS tools");
		helpMap.put("systemdriverurl","Url or IP address of a remote system driver server with or without port example : \nsystemdriverurl=192.168.0.1:9700 \nsystemdriverurl=192.168.0.1\nsystemdriverurl=http://192.168.0.1\nsystemdriverurl=192.168.0.1:9700");
		helpMap.put("proxyurl","Url or IP address of proxy server example : \nproxyurl=http://www.proxy.test:8080 \nproxyurl=https://www.proxy.test:8080\nproxyurl=http://username:password@www.proxy.test:8080 \nproxyurl=https://username:password@www.proxy.test:8080");
		checkHelp(args,helpMap);

		System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");

		String toolsServerUrl = ATS_SERVER + ATS_TOOLS_DIRECTORY_SERVER;
		arch = System.getProperty("os.arch");

		if(OS.contains("win")) {

			atsHomeInstall = System.getenv("APPDATA");
			toolsServerUrl = toolsServerUrl.replace(OS_TAG, "windows");

			operatingSystem = WINDOWS;
			operatingSystemPath = WINDOWS;
			atsToolsList.put(JDK, "jdk/windows");
		}
		else {
			if(OS.contains("linux")) {
				toolsServerUrl = toolsServerUrl.replace(OS_TAG, "linux");
				atsToolsList.put(JDK, "jdk/linux/");
			}
			else if(OS.contains("mac")){
				String pathMacOs = MACOS_X86_64;
				operatingSystem = MACOS;
				operatingSystemPath = MACOS;
				if(arch.equals(ARCH_ARM)) {
					pathMacOs = MACOS_AARCH64;
					operatingSystemPath = MACOS_AARCH64;
				}


				toolsServerUrl = toolsServerUrl.replace(OS_TAG, pathMacOs);
				atsToolsList.put(JDK, "jdk/"+pathMacOs+"/");
			}

			posixFilePermission = new HashSet<>();
			posixFilePermission.add(PosixFilePermission.OWNER_READ);
			posixFilePermission.add(PosixFilePermission.OWNER_WRITE);
			posixFilePermission.add(PosixFilePermission.OWNER_EXECUTE);

			posixFilePermission.add(PosixFilePermission.OTHERS_READ);
			posixFilePermission.add(PosixFilePermission.OTHERS_WRITE);
			posixFilePermission.add(PosixFilePermission.OTHERS_EXECUTE);

			posixFilePermission.add(PosixFilePermission.GROUP_READ);
			posixFilePermission.add(PosixFilePermission.GROUP_WRITE);
			posixFilePermission.add(PosixFilePermission.GROUP_EXECUTE);
		}

		atsToolsInstall = atsHomeInstall + atsToolsInstall;
		atsCacheInstall = atsHomeInstall + atsCacheInstall;

		final Integer javaVersion = Runtime.version().version().get(0);

		if (javaVersion < MINIMUM_JAVA_JDK_VERSION) {
			final String errorMessage = 
					new StringBuilder("Java version ")
					.append(javaVersion)
					.append(" found, minimum version ")
					.append(MINIMUM_JAVA_JDK_VERSION)
					.append(" is needed to execute this script !")
					.toString();
			
			printError(errorMessage);
			throw new Exception(errorMessage);
		}

		final File script = new File(AtsLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		projectFolderPath = Paths.get(script.getParent().replace("%20", " "));
		Path propFilePath = projectFolderPath.resolve(ATS_PROJECT_PROPERTIES).toAbsolutePath();

		if(!Files.exists(propFilePath)) {
			projectFolderPath = Path.of("").toAbsolutePath();
			propFilePath = projectFolderPath.resolve(ATS_PROJECT_PROPERTIES).toAbsolutePath();

			if(!Files.exists(propFilePath)) {
				printError("Unable to find ATS project properties file, this script will stop now");
				System.exit(0);
			}
		}

		printLog("AtsLauncher script version -> " + ATS_LAUNCHER_VERSION);
		printLog("Operating system detected -> " + operatingSystem);
		printLog("Java JDK version -> " + javaVersion);
		printLog("Current user -> " + System.getProperty("user.name"));
		printLog("Current folder -> " + System.getProperty("user.dir"));
		printLog("ATS project folder -> " + projectFolderPath.toString());

		final Path targetFolderPath = projectFolderPath.resolve(TARGET);

		boolean buildEnvironment = false;
		boolean outboundTraffic = true;
		boolean disableSSLTrust = false;

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		//-------------------------------------------------------------------------------------------------
		// Read atsProjectProperties file
		//-------------------------------------------------------------------------------------------------

		try (InputStream is = new FileInputStream(propFilePath.toString())) {

			final Document doc = dbf.newDocumentBuilder().parse(is);

			if (doc.hasChildNodes()) {
				final Node root = doc.getChildNodes().item(0);
				if (root.hasChildNodes()) {
					final NodeList childs = root.getChildNodes();
					for (int i = 0; i < childs.getLength(); i++) {

						final String nodeName = childs.item(i).getNodeName();
						final String textContent = childs.item(i).getTextContent().trim();

						if (atsToolsFolderProperty.equalsIgnoreCase(nodeName)) {
							atsToolsFolder = textContent;
						} else if (atsToolsUrlProperty.equalsIgnoreCase(nodeName)) {
							atsToolsUrl = textContent;
						} else if (outboundProperty.equalsIgnoreCase(nodeName)) {
							outboundTraffic = FALSE_LIST.indexOf(textContent.toLowerCase()) == -1;
						} else if (disableSSLParam.equalsIgnoreCase(nodeName)) {
							disableSSLTrust = TRUE_LIST.indexOf(textContent.toLowerCase()) > -1;
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		//-------------------------------------------------------------------------------------------------
		// Read command line arguments
		//-------------------------------------------------------------------------------------------------

		final List<String> definitions = new ArrayList<String>();
		boolean installOnly = false;

		for (int i = 0; i < args.length; i++) {

			final String allArgs = args[i].trim();

			if("-A:".equals(allArgs.substring(0, 3))) {
				definitions.add("-D" + allArgs.substring(3));
			}
			else {

				String firstArg = allArgs.toLowerCase();
				final int equalPos = firstArg.indexOf("=");

				if("clean".equals(firstArg)) {
					deleteDirectory(Paths.get(atsCacheInstall));
					deleteDirectory(Paths.get(atsToolsInstall));
				}

				if (equalPos == -1) {
					firstArg = allArgs.toLowerCase().startsWith("-") ? allArgs.substring(1).toLowerCase() : allArgs.toLowerCase();
					if ("buildenvironment".equals(firstArg) || "preparemaven".equals(firstArg)) {
						buildEnvironment = true;
						asSuiteExecution = false;
					} else if ("disablessl".equals(firstArg)) {
						disableSSLTrust = true;
					}else if ("install".equals(firstArg) || "installtools".equals(firstArg)) {
						installOnly = true;
						asSuiteExecution = false;
					}
				} else {
					final String argName = firstArg.substring(0, equalPos).replaceAll("\\-", "");
					final String argValue = allArgs.substring(equalPos + 1).trim();

					switch (argName) {

					case "atsagenturl":
					case "ats.agent.url":
					case "ats-agent-url":
					case "systemdriverurl":
					case "system.driver.url":
					case "system-driver-url":

						if(checkSystemDriverUrl(argValue)){
							systemDriverUrl = addDefaultSystemeDriverPort(argValue);
						}
						else{
							printLog("Invalid value for systemDriverUrl -> " + argValue);
						}
						break;

					case "suite":
					case "suites":
					case "suitefile":
					case "suitefiles":
					case "suitesfiles":
					case "suitexmlfiles":
					case "suite-xml-files":
					case "suite.xml.files":
						suiteFiles = argValue;
						if(suiteFiles.startsWith("=")){
							suiteFiles = suiteFiles.substring(1);
						}
						break;

					case "atslistscripts":
						atsScripts = argValue;
						break;

					case "tempsuitename":
						if (argValue.length() > 0) {
							tempSuiteName = argValue;
						}
						break;

					case "preparemaven":
						buildEnvironment = TRUE_LIST.indexOf(argValue.toLowerCase()) != -1;
						asSuiteExecution = false;
						break;

					case "atsreport":
					case "reportlevel":
					case "report.level":
					case "ats.report":
					case "ats-report":
					case "atsreportlevel":
					case "ats-report-level":
					case "ats.report.level":
						if(argValue.startsWith("=")){
							reportLevel = argValue.substring(1);
						}else{
							reportLevel = argValue;
						}

						if(!REPORT_LEVEL_LIST.contains(reportLevel) ) {
							reportLevel = "";
							printLog("Invalid value for "+argName+" -> " + argValue+ " use default value");
						}
						break;

					case "validationreport":
					case "validation-report":
					case "validation.report":
						if(isValidBooleanString(argValue)) {
							validationReport = (isFalseOrTrue(argValue)) ? "1" : "0";
						}else {
							printLog("Invalid value for validationReport -> " + argValue+ " use default value");
						}
						break;

					case "htmlplayer":
					case "html-player":
					case "html.player":
						if(isValidBooleanString(argValue)) {
							htmlReportParam = (isFalseOrTrue(argValue)) ? "1" : "0";
						}else {
							printLog("Invalid value for htmlplayer -> " + argValue);
						}
						break;

					case "reportsdirectory":
					case "reports-directory":
					case "reports.directory":
					case "reports-output":
					case "reports.output":
					case "output":
						output = argValue;
						if (output.endsWith("/")) { output = output.substring(0, output.length() - 1);}
						isValidOutputDirectory(output, argName);
						break;

					case "atsurl":
						atsUrl = argValue;
						if(!isValidCharactersUrl(atsUrl)){
							printLog("Invalid characters in atsUrl -> " + atsUrl);
							System.exit(0);
						}
						break;

					case "atstoolsurl":
					case "ats-tools-url":
					case "ats.tools.url":
						atsToolsUrl = argValue;
						if(!isValidCharactersUrl(atsToolsUrl)){
							printLog("Invalid characters in atsToolsUrl -> " + atsToolsUrl);
							System.exit(0);
						}
						break;

					case "atstoolsfolder":
					case "ats-tools-folder":
					case "ats.tools.folder":
						if(argValue != null && argValue.length() > 0) {
							atsToolsFolder = argValue;
							atsToolsFolderIsDefine = true;
						}
						break;

					case "atsfolder":
					case "ats-folder":
					case "ats.folder":
						if(argValue != null && argValue.length() > 0) {
							atsFolder = argValue;
						}
						break;

					case "outbound":
						outboundTraffic = FALSE_LIST.indexOf(argValue.toLowerCase()) == -1;
						break;

					case "disablessl":
					case "disable-ssl":
					case "disable.ssl":
						disableSSLTrust = TRUE_LIST.indexOf(argValue.toLowerCase()) > -1;
						break;

					case "enablessl":
					case "enable-ssl":
					case "enable.ssl":
						toolsServerUrl = toolsServerUrl.replace("http:", "https:");
						atsServerReleasesUrl = atsServerReleasesUrl.replace("http:", "https:");
						break;

					case "jenkinsurl":
					case "jenkins-url":
					case "jenkins.url":
						jenkinsToolsUrl = argValue;
						if (!jenkinsToolsUrl.endsWith("/")) {
							jenkinsToolsUrl += "/";
						}
						jenkinsToolsUrl += ATS_JENKINS_TOOLS;
						break;

					case "proxyurl":
					case "proxy-url":
					case "proxy.url":
						if(checkProxyUrl(argValue)){

							printLog("Using proxy url -> " + argValue);

							final URI proxyUri = new URI(argValue);
							proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort()));

							if(argValue.startsWith("https://")){
								atsExecEnv.put("HTTPS_PROXY", argValue);
							}else {
								atsExecEnv.put("HTTP_PROXY", argValue);
							}
						}
						else{
							printLog("Invalid value for proxyUrl -> " + argValue);
						}
						break;
					}
				}
			}
		}

		final String atsLibsRemoteUrl = atsServerReleasesUrl + "/ats-libs/";

		//-------------------------------------------------------------------------------------------------
		// check if server http others actiontestscript
		//-------------------------------------------------------------------------------------------------
		if(jenkinsToolsUrl != null){
			if(!checkIsJenkinsOk(atsLibsRemoteUrl, jenkinsToolsUrl)) jenkinsToolsUrl = null ;
		}

		if(atsUrl != null){
			if(!atsUrl.startsWith("http://") && !atsUrl.startsWith("https://")) atsUrl = "http://" + atsUrl;
			atsServerReleasesUrl = atsUrl + ATS_RELEASES_DIRECTORY_SERVER;
			if(atsToolsUrl == null) atsToolsUrl = atsUrl + ATS_TOOLS_DIRECTORY_SERVER;
		}

		if( ATS_VERSION == null || ATS_VERSION.isEmpty() || ATS_VERSION.trim().isEmpty()){

			//-------------------------------------------------------------------------------------------------
			// Read pom.xml file
			//-------------------------------------------------------------------------------------------------

			projectAtsVersion = getAtsVersion(atsLibsRemoteUrl, dbf.newDocumentBuilder(), projectFolderPath.resolve("pom.xml").toAbsolutePath().toString());

			if(projectAtsVersion != null) {
				printLog("ATS library version -> " + projectAtsVersion);
				if (atsFolder != null) {
					//none download for atsfolder set
					executeAtsFolder();
				}else {

					int install = 0;

					final Path currentAtsFolder = Paths.get(atsCacheInstall).resolve(projectAtsVersion);
					Path currentLibsFolder = currentAtsFolder.resolve("libs");
					Path currentDriversFolder = currentAtsFolder.resolve("drivers");

					if(atsEnvHome != null){
						currentLibsFolder = Paths.get(atsEnvHome).resolve("libs");
						currentDriversFolder = Paths.get(atsEnvHome).resolve("drivers");
					}

					if ( jenkinsToolsUrl != null || isDirectoryExistAndNoEmpty(currentLibsFolder)) {
						install = 1;
					} else {

						printLog("ATS releases server -> " + atsServerReleasesUrl);
						if(!Files.exists(currentLibsFolder)) {
							Files.createDirectories(currentLibsFolder);
						}

						final String url = atsLibsRemoteUrl + projectAtsVersion + ".zip";
						downloadAndExtract(url, currentLibsFolder, "Ats libs", projectAtsVersion,true);
						install = 1;
					}

					if (install > 0) {

						if ( jenkinsToolsUrl != null || isDirectoryExistAndNoEmpty(currentDriversFolder)) {
							install++;
						} else {
							if(!Files.exists(currentDriversFolder)) {
								Files.createDirectories(currentDriversFolder);
							}

							final String versionUrl = getLastVersionUrl(atsServerReleasesUrl + "/ats-drivers/" + operatingSystemPath + "/system");

							if (versionUrl != null) {
								downloadAndExtract(versionUrl, currentDriversFolder, "Ats system driver", systemDriverVersion,true);
							}
							install++;
						}
					}

					final AtsToolEnvironment atsTool = new AtsToolEnvironment(ATS);
					if (install > 1) {
						if(atsEnvHome != null) {
							atsTool.update(Paths.get(atsEnvHome));
						} else {
							atsTool.update(currentAtsFolder);
						}
					}

					atsToolsEnv.add(atsTool);
					atsToolsEnv.add(new AtsToolEnvironment(JDK));
				}
			} else {
				printLog("Unable to fin ATS library version defined in pom.xml !!");
			}

		} else {

			projectAtsVersion = ATS_VERSION;

			printLog("ATS library version defined by environement variable -> " + projectAtsVersion);

			final AtsToolEnvironment atsTool = new AtsToolEnvironment(ATS);
			atsTool.update(Paths.get(atsCacheInstall).resolve(projectAtsVersion));

			atsToolsEnv.add(atsTool);
			atsToolsEnv.add(new AtsToolEnvironment(JDK));
		}

		//-------------------------------------------------------------------------------------------------
		// Check if SSL certificates trust is disabled
		//-------------------------------------------------------------------------------------------------

		if (disableSSLTrust) {
			disableSSL();
		}

		//-------------------------------------------------------------------------------------------------
		// Check and delete output directories
		//-------------------------------------------------------------------------------------------------

		boolean isOutputBase = true;
		if(output == null) {
			output = outputBase;
			isOutputBase=false;
		}

		Path atsOutput = Paths.get(output);
		if (!atsOutput.isAbsolute()) {
			atsOutput = projectFolderPath.resolve(output);
		} else if(!isDirectoryWritableOrCreatable(output)) {
			printLog("Output directory is not writable or creatable -> " + atsOutput.toString());
			atsOutput = projectFolderPath.resolve(Paths.get(outputBase));
		}

		//Display only output directory if not default
		if(isOutputBase) printLog("Output directory -> " + atsOutput.toString());

		printLog("Delete directory: " + targetFolderPath.toString());
		deleteDirectory(targetFolderPath);
		
		printLog("Empty directory: " + atsOutput.toString());
		emptyFolder(atsOutput);

		final Path testOutput = projectFolderPath.resolve("test-output");
		printLog("Delete directory: " + testOutput.toString());
		deleteDirectory(testOutput);

		//-------------------------------------------------------------------------------------------------
		// Check list ATS scripts
		//-------------------------------------------------------------------------------------------------

		String[] suiteFilesList = new String[0];

		if (atsScripts != null && atsScripts.trim().length() > 0) {

			Files.createDirectories(Paths.get(TARGET));
			suiteFiles = TARGET + "/" + tempSuiteName + ".xml";

			final StringBuilder builder = new StringBuilder("<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">\n");
			builder.append("<suite name=\"").append(tempSuiteName).append("\" verbose=\"0\">\n<test name=\"testMain\" preserve-order=\"true\">\n<classes>\n");

			final Stream<String> atsScriptsList = Arrays.stream(atsScripts.split(","));
			atsScriptsList.forEach(a -> addScriptToSuiteFile(builder, a));

			builder.append("</classes>\n</test></suite>");

			try (PrintWriter out = new PrintWriter(suiteFiles)) {
				out.println(builder.toString());
				out.close();
			}

			suiteFilesList = new String[] {suiteFiles};

		}else if(suiteFiles != null && suiteFiles.trim().length() > 0) {
			String[] arr = suiteFiles.split(",");
			IntStream.range (0, arr.length).forEach (i -> {arr[i] = getSuitePath(arr[i]);});

			suiteFilesList = arr;
		}
		else if(asSuiteExecution){
			List<String> defaultAtsSuite = getAtsDefaultSuite(dbf.newDocumentBuilder(), projectFolderPath.resolve("pom.xml").toAbsolutePath().toString());
			if(defaultAtsSuite != null && !defaultAtsSuite.isEmpty()) {
				suiteFilesList = defaultAtsSuite.toArray(new String[0]);
			}
			else {
				printLog("No suite file defined, this script will stop now");
				System.exit(0);
			}
		}
		checkSuiteFileExists(suiteFilesList);

		//-------------------------------------------------------------------------------------------------
		// if ATS server url has not been set using default url
		//-------------------------------------------------------------------------------------------------

		if (atsToolsUrl == null) {
			atsToolsUrl = toolsServerUrl;
		}

		//-------------------------------------------------------------------------------------------------
		// try to get environment value
		//-------------------------------------------------------------------------------------------------

		if (atsToolsFolder == null) {
			atsToolsFolder = System.getenv("ATS_TOOLS");
			if(atsToolsFolder != null) atsToolsFolderIsDefine=true;
		}

		if (reportLevel.isEmpty()) {
			String reportParam = System.getenv("ATS_REPORT");
			if(reportParam != null && REPORT_LEVEL_LIST.contains(reportParam) ) {
				reportLevel = reportParam;
				printLog("ATS_REPORT environment variable found -> " + reportParam);
			}
		}

		//-------------------------------------------------------------------------------------------------
		// if ats folder not defined using 'userprofile' home directory
		//-------------------------------------------------------------------------------------------------

		if (atsToolsFolder == null) {
			atsToolsFolder = atsToolsInstall;
		}

		//-------------------------------------------------------------------------------------------------

		final List<String> envList = new ArrayList<String>();

		boolean serverFound = false;
		String serverNotReachable = "ATS tools server is not reachable";

		if (outboundTraffic) {
			if (jenkinsToolsUrl != null) {
				serverFound = checkAtsToolsVersions(true, jenkinsToolsUrl);
			} else {
				serverFound=false;
				if(atsFolder == null ) {
					if(!atsToolsFolderIsDefine) {
						serverFound = checkAtsToolsVersions(false, atsToolsUrl);
					}
				}
			}
		} else {
			serverNotReachable += " (outbound traffic has been turned off by user)";
		}

		if (!serverFound) {
			if(!outboundTraffic ) printLog(serverNotReachable);

			if(atsToolsEnvLocal != null) {
				atsToolsEnv = atsToolsEnvLocal;
			}

			atsToolsEnv.stream().forEach(e -> installAtsTool(e, envList, Paths.get(atsToolsFolder)));
			if(atsToolsFolderIsDefine) {
				//check if not null
				final boolean[] errorFolderTools={false};
				atsToolsEnv.stream().forEach(e->{
					if(e.folder == null) {
						printLog("ATS tools "+e.name+" not found in folder -> " + atsToolsFolder);
						errorFolderTools[0] = true;
					} else if(e.name.equals(JDK)){
						//check version java
						String commandPath = Paths.get(e.folder.toString(), "bin", "java").toString();
						ProcessBuilder processBuilder = new ProcessBuilder(commandPath, "--version");
						try {
							Process process = processBuilder.start();
							BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
							Pattern regex = Pattern.compile("\\b(\\d+)\\.?");
							String line;
							String majorVersionStr = null;
							while ((line = reader.readLine()) != null) {
								Matcher matcher = regex.matcher(line);
								if (matcher.find()) {
									majorVersionStr = matcher.group(1);
									break;
								}
							}
							if(majorVersionStr != null) {
								int majorVersion = Integer.parseInt(majorVersionStr);
								if(majorVersion < MINIMUM_JAVA_JDK_VERSION) {
									printLog("ATS tools "+ e.name +" version "+ majorVersion +" not compatible with ATS version "+projectAtsVersion);
									errorFolderTools[0] = true;
								}
							}
						}
						catch (IOException exc) {
							exc.printStackTrace();
						}
					}
				});

				if(errorFolderTools[0]) {
					System.exit(0);
				}
			}

			if (atsToolsEnv.size() != envList.size()) {
				printLog("ATS tools not found in folder -> " + atsToolsFolder);
				System.exit(0);
			}

		} else {
			atsToolsEnv.stream().forEach(e -> installAtsTool(e, envList));
		}

		if(installOnly) {
			System.out.println("====================================================");
			printLog("ATS tools and components installed !");
			System.out.println("====================================================");
			System.exit(0);
		}

		if (buildEnvironment) {

			final Path p = projectFolderPath.resolve(BUILD_PROPERTIES);
			Files.deleteIfExists(p);

			Files.write(p, String.join("\n", envList).getBytes(), StandardOpenOption.CREATE);

			printLog("Build properties file created : " + p.toFile().getAbsolutePath());

		} else {

			final File projectDirectoryFile = projectFolderPath.toFile();
			final Path generatedPath = targetFolderPath.resolve("generated");
			final File generatedSourceDir = generatedPath.toFile();
			final String generatedSourceDirPath = generatedSourceDir.getAbsolutePath();

			generatedSourceDir.mkdirs();

			printLog("Generate java files -> " + generatedSourceDirPath);

			final FullLogConsumer logConsumer = new FullLogConsumer();

			final String javaRunCommand = new StringBuilder(Paths.get(jdkHomePath).toAbsolutePath().toString()).append("/bin/java").toString();

			String[] command = 
					new String[]{
							javaRunCommand,
							"-cp",
							atsHomePath + "/libs/*",
							"com.ats.generator.Generator",
							"-prj",
							projectFolderPath.toString(),
							"-dest",
							targetFolderPath.toString() + "/generated",
							"-force"
			};

			execute(command,
					projectDirectoryFile,
					logConsumer,
					logConsumer);

			final ArrayList<String> files = listJavaClasses(generatedSourceDirPath.length() + 1, generatedSourceDir);

			//------------------------ Display environment variables -----------------------------------------
			if(definitions.size() > 0) {
				List<String> newDefinitions = definitions.stream()
						.map(str -> str.replaceAll("^-D", ""))
						.collect(Collectors.toList());
				printLog("Environment variables passed : " + String.join(" ; ", newDefinitions));
			}

			//------------------------ Compile classes ------------------------------------------------------
			final Path classFolder = targetFolderPath.resolve("classes").toAbsolutePath();
			final Path classFolderAssets = classFolder.resolve("assets");
			classFolderAssets.toFile().mkdirs();

			copyFolder(projectFolderPath.resolve("src").resolve("assets"), classFolderAssets);

			//----------------------------------------------------------------------------------------

			printLog("Compile classes to folder -> " + classFolder.toString());
			Files.write(generatedPath.resolve("JavaClasses.list"), String.join("\n", files).getBytes(), StandardOpenOption.CREATE);

			command = new String[]{
					javaRunCommand + "c",
					"-cp",
					"../../libs/*" + File.pathSeparator + atsHomePath + "/libs/*",
					"-d",
					classFolder.toString(),
					"@JavaClasses.list"
			};

			execute(command,
					generatedPath.toAbsolutePath().toFile(),
					logConsumer,
					logConsumer);

			//----------------------------------------------------------------------------------------

			printLog("Launch suite(s) execution -> " + suiteFiles);

			command = new String[]{
					javaRunCommand,
					"-Dats-report=" + reportLevel,
					"-Dvalidation-report=" + validationReport,
					"-Dhtmlplayer=" + htmlReportParam,
					"-Doutbound-traffic=" + outboundTraffic,
					"-Dats.home=" + atsHomePath,
					"-Dsystem-driver-url=" + systemDriverUrl
			};

			command = concatWithArrayCopy(command, definitions.toArray(String[]::new));

			String[] atsCommand = new String[]{
					"-cp",
					atsHomePath + "/libs/*" + File.pathSeparator + targetFolderPath.toString() + "/classes" + File.pathSeparator + "libs/*",
					"org.testng.TestNG",
					"-d",
					atsOutput.toString()
			};

			command = concatWithArrayCopy(command, atsCommand);
			command = concatWithArrayCopy(command, suiteFilesList);

			execute(command,
					projectDirectoryFile,
					logConsumer,
					new TestNGLogConsumer(),
					atsExecEnv);
			
			if(!Files.exists(atsOutput.resolve("ats-results.json"))) {
				String errorMessage = "result file 'ats-results.json' not found";
				printError(errorMessage + " (test was not executed)");
				throw new Exception(errorMessage);
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------
	// Functions
	//------------------------------------------------------------------------------------------------------------

	private static boolean checkProxyUrl(String argValue){
		String regex = "^(https?)://(?:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+@)?[a-zA-Z0-9.-]+:\\d+$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(argValue);
		return matcher.matches();
	}
	private static String addDefaultSystemeDriverPort(String value){
		if (!value.matches(".*:\\d+$")) {
			return value + ":9700";
		}
		return value;
	}

	private static boolean checkSystemDriverUrl(String value){
		String regex = "^(http[s]?://)?(\\d{1,3}\\.){3}\\d{1,3}(:\\d{1,5})?(/)?$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(value);
		if(matcher.matches()){
			int lastIndex = value.lastIndexOf(":");

			if(lastIndex != -1 && value.matches(".*:\\d+$")){
				String strPort = value.substring(lastIndex+1);
				int port=0;
				if(strPort.length() > 0 && strPort.length() < 6){
					try{
						port = Integer.parseInt(strPort);
						return (port > 1023 && port < 65536);
					}catch (NumberFormatException e){
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	private static boolean isFalseOrTrue(String value){
		if(value == null || value.isEmpty()) return false;
		return TRUE_LIST.contains(value.toLowerCase()) ;
	}

	private static boolean isValidBooleanString(String value) {
		if(value != null && value.length() > 0) {
			return TRUE_LIST.indexOf(value.toLowerCase()) != -1 || FALSE_LIST.indexOf(value.toLowerCase()) != -1;
		}
		return false;
	}

	private static boolean isValidCharactersUrl(String url){
		String illegalCharacters = "<>\"%{}|\\^~[]` ";
		for (char c : url.toCharArray()) {
			if (illegalCharacters.indexOf(c) != -1) {
				return false;
			}
		}
		return true;
	}

	private static String cleanArgument(String arg) {
		String[] prefixes = {"--", "-", "/"};
		for (String prefix : prefixes) {
			if (arg.startsWith(prefix)) {
				return arg.substring(prefix.length()).toLowerCase();
			}
		}
		return arg.toLowerCase();
	}

	private static void checkHelp(String[] args, HashMap<String, String> helpMap){
		String currentArg = null;
		String previousArg=null;
		if ( args.length < 2 && args.length > 0) {
			currentArg = cleanArgument(args[0]);
			if(currentArg.equals("help") || currentArg.equals("?")){
				printHelp("general");
				System.exit(0);
			}
		} else if(args.length > 1 ) {
			currentArg = cleanArgument(args[args.length - 1]);
			if(currentArg.equals("help") || currentArg.equals("?")){
				previousArg = cleanArgument(args[args.length - 2]);
				int indexOf = previousArg.indexOf('=');
				if(indexOf != -1) previousArg = previousArg.substring(0, indexOf);
				printHelp( previousArg);
				System.exit(0);
			}
		}
	}

	private static void printHelp(String key) {
		switch(key){
		case "general" :
			System.out.println(helpMap.get("general"));
			break;

		case "clean" :
		case "preparemaven" :
		case "buildenvironment" :
		case "suitexmlfiles" :
		case "atsreport" :
		case "validationreport" :
		case "atslistscripts" :
		case "tempsuitename" :
		case "disablessl" :
		case "atsurl" :
		case "atstoolsurl" :
		case "jenkinsurl" :
		case "reportsdirectory" :
		case "outbound" :
		case "atstoolsfolder" :
		case "ats_tools" :
		case "atsfolder" :
		case "xml_atsprojectproperties" :
			System.out.println(helpMap.get(key));
			break;

		default :
			System.out.println("No help found for key : "+key);
			System.out.println(helpMap.get("general"));
		}
	}

	private static void executeAtsFolder() {
		final Path atsPath = Paths.get(atsFolder);

		final Path driversPath = atsPath.resolve("drivers");
		final Path libsPath = atsPath.resolve("libs");
		final Path toolsPath = ((atsToolsFolder == null) ? atsPath.resolve("tools") : Paths.get(atsToolsFolder));
		final Path jdkPath = toolsPath.resolve("jdk");

		if (Files.exists(driversPath) && Files.exists(libsPath) && Files.exists(jdkPath) ) {
			atsToolsEnvLocal = new ArrayList<AtsToolEnvironment>(
					Arrays.asList(new AtsToolEnvironment(ATS, atsPath),
							new AtsToolEnvironment(JDK, jdkPath)
							));
		} else {
			printLog("ATS folder is not valid structure");
			printHelp( "atsfolder");
			System.exit(0);
		}
	}

	private static <T> String[] concatWithArrayCopy(String[] array1, String[] array2) {
		String[] result = Arrays.copyOf(array1, array1.length + array2.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);

		return Arrays.stream(result).filter(x -> !isStringBlank(x)).toArray(String[]::new);
	}

	private static boolean isStringBlank(String value) {
		return value == null || "".equals(value);
	}

	private static String getSuitePath(String name) {

		name = name.replaceAll("\"", "");

		if(!name.startsWith(SRC_EXEC) && !name.endsWith("ats__temp__suite__.xml")) {
			name = SRC_EXEC + "/" + name;
		}

		if(!name.endsWith(".xml")) {
			name += ".xml";
		}

		return name;
	}

	private static String getLastVersionUrl(String folderUrl) throws MalformedURLException, IOException, URISyntaxException {
		final HttpURLConnection con = createHttpConnection(folderUrl, true);

		if(con != null && con.getResponseCode() == 200) {

			final InputStream inputStream = con.getInputStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			final StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			inputStream.close();

			final ArrayList<String> versions = new ArrayList<String>();

			final Matcher matcher = SYS_VERSION_PATTERN.matcher(builder.toString());
			int index = 0;
			while (matcher.find(index)) {
				versions.add(matcher.group(1));
				index = matcher.end();
			}

			if(versions.size() > 0) {
				final Version version = versions.stream()
						.map(Version::parse)
						.sorted(Collections.reverseOrder())
						.findFirst().get();

				if(version != null) {
					systemDriverVersion = version.toString();
					return folderUrl + "/" + systemDriverVersion;
				}
			}
		}

		return null;
	}

	private static void addScriptToSuiteFile(StringBuilder builder, String scriptName) {
		scriptName = scriptName.replaceAll("\\/", ".");
		if (scriptName.endsWith(".ats")) {
			scriptName = scriptName.substring(0, scriptName.length() - 4);
		}

		if (scriptName.startsWith(".")) {
			scriptName = scriptName.substring(1);
		}

		builder.append("<class name=\"").append(scriptName).append("\"/>\n");
	}

	private static Map<String, String[]> getServerToolsVersion(String serverUrl) {

		final Map<String, String[]> versions = new HashMap<String, String[]>();
		try {

			HttpURLConnection.setFollowRedirects(false);
			final HttpURLConnection yc = getConnection(serverUrl);

			yc.setRequestMethod("GET");
			yc.setRequestProperty("Connection", "Keep-Alive");
			yc.setRequestProperty("Cache-Control", "no-cache");
			yc.setRequestProperty("User-Agent", "AtsLauncher-" + operatingSystem);

			yc.setUseCaches(false);
			yc.setDoOutput(true);

			final BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				String[] lineData = inputLine.split(",");
				versions.put(lineData[0], lineData);
			}

			in.close();

		} catch (IOException | URISyntaxException e) {
			printLog("AtsLauncher error -> " + e.getMessage());
		}

		return versions;
	}

	private static String removeExtension(String filename) {
		if (filename == null) return null;
		int lastIndex = filename.lastIndexOf('.');
		if (lastIndex > 0) return filename.substring(0, lastIndex);
		return filename;
	}

	private static String getFilename(String urlFileName) {
		if(urlFileName == null) return null;
		int lastIndex = urlFileName.lastIndexOf('/');
		if (lastIndex > 0) return urlFileName.substring(lastIndex+1);
		return urlFileName;
	}

	private static String getExtension(String filename) {
		if (filename == null) return null;
		int lastIndex = filename.lastIndexOf('.');
		if (lastIndex > 0) return filename.substring(lastIndex+1);
		return filename;
	}

	private static Boolean checkAtsToolsVersions(boolean localServer, String server) {

		Map<String, String[]> versions = new HashMap<String, String[]>();
		if(!getFilename(server).equals("versions.csv")) {
			String url="";
			for(Map.Entry<String, String> atsTool : atsToolsList.entrySet()) {
				server = server.endsWith("/") ? server : server + "/";
				server = server.startsWith("http") ? server : "http://" + server;
				url = server + atsTool.getValue();
				url = url.endsWith("/") ? url : url + "/";
				try {
					final String fileLastVersion=getFilename(getLastVersionUrl(url));
					if(fileLastVersion == null){
						printLog("Unable to get last version of " + atsTool.getKey() + " on this server -> " + url);
						System.exit(0);
					}
					final String lastVersion= removeExtension(fileLastVersion);
					final String folderLastVersion =atsTool.getKey() + "-" + lastVersion;
					final String urlLastVersion = url + fileLastVersion;
					versions.put(atsTool.getKey(), new String[] {atsTool.getKey(), lastVersion, folderLastVersion, urlLastVersion});
				}
				catch (IOException | URISyntaxException e){}
			}

			if (versions.size() < atsToolsEnv.size() && localServer) {
				printLog("Unable to get all ATS tools on this server -> " + server);
				versions.putAll(getServerToolsVersion(atsToolsUrl));
				if (versions.size() < atsToolsEnv.size()) {
					return false;
				}
			}
		} else{  //For jenkinsUrl
			versions = getServerToolsVersion(server);
			atsToolsEnv.forEach(t -> { if ("ats".equals(t.name)) t.check = true;});
		}

		for (AtsToolEnvironment t : atsToolsEnv) {
			if(t.check) {
				final String[] toolData = versions.get(t.name);
				if (toolData != null) {
					final String folderName = toolData[2];
					t.folderName = folderName;
					final File toolFolder = ((t.name.equals(ATS)) ? Paths.get(atsCacheInstall).resolve(folderName).toFile() :  Paths.get(atsToolsFolder).resolve(folderName).toFile());

					if (toolFolder.exists()) {
						t.folder = toolFolder.getAbsolutePath();
					} else {
						t.url = toolData[3];
					}
				}
			}
		}
		return true;
	}

	private static void toolInstalled(AtsToolEnvironment tool, List<String> envList) {

		if (ATS.equals(tool.name)) {

			if(atsFolder==null && atsEnvHome != null) {
				final Path atsPath = Paths.get(atsEnvHome);
				if(Files.exists(atsPath) && Files.exists(atsPath.resolve("drivers")) && Files.exists(atsPath.resolve("libs"))) {
					tool.update(atsPath);
				}
			}

			atsHomePath = tool.folder;

		} else if (JDK.equals(tool.name)) {
			jdkHomePath = tool.folder;
		}

		envList.add(tool.envName + "=" + tool.folder);
		printLog("Set environment variable [" + tool.envName + "] -> " + tool.folder);

		atsExecEnv.put(tool.envName, tool.folder);
	}

	private static void installAtsTool(AtsToolEnvironment tool, List<String> envList, Path toolsPath) {
		if(!tool.check) {
			toolInstalled(tool, envList);
		} else {	
			try (Stream<Path> stream = Files.walk(toolsPath, 1)) {
				final List<String> folders = stream
						.filter(file -> file != toolsPath && Files.isDirectory(file) && file.getFileName().toString().startsWith(tool.name))
						.map(Path::getFileName)
						.map(Path::toString)
						.collect(Collectors.toList());

				if (folders.size() > 0) {
					folders.sort(Collections.reverseOrder());

					tool.folder = toolsPath.resolve(folders.get(0)).toAbsolutePath().toString();
					toolInstalled(tool, envList);
				}

			} catch (Exception e) {}
		}
	}

	private static void installAtsTool(AtsToolEnvironment tool, List<String> envList) {
		if(!tool.check) {
			toolInstalled(tool, envList);
		} else {
			boolean downloadAts = false;
			if(tool.name.equals(ATS) && jenkinsToolsUrl != null) {
				final File[] files = Paths.get(atsCacheInstall).toFile().listFiles();
				String ats_version = tool.folderName.replaceFirst("^ats-","");
				if(files != null) {
					Arrays.sort(files, Comparator.comparingLong(File::lastModified));
					for (File f : files) {
						if (f.getName().startsWith(ats_version)) {
							tool.folderName = f.getName();
							tool.folder = f.getAbsolutePath();
							downloadAts = true;
							break;
						}
					}
				}
			}

			if (tool.folderName == null && !tool.name.equals(ATS)) {
				final File[] files = Paths.get(atsToolsFolder).toFile().listFiles();
				if(files != null) {
					Arrays.sort(files, Comparator.comparingLong(File::lastModified));
					for (File f : files) {
						if (f.getName().startsWith(tool.name)) {
							tool.folderName = f.getName();
							tool.folder = f.getAbsolutePath();
							break;
						}
					}
				}
			} else if(tool.url != null && !downloadAts) {

				printLog("Download ATS tool -> " + tool.url);

				final String urlArchiveExtension = getExtension(getFilename(tool.url));
				final HttpURLConnection connection = createHttpConnection(tool.url, true);

				if(connection != null) {

					try {
						final File tmpZipFile = download(connection, tool.name);

						if(urlArchiveExtension.equals(TGZ) ) {

							final Path path = Paths.get(atsToolsFolder);
							Files.createDirectories(path);

							try {
								execute(
										new String[]{
												"tar",
												"-xzf",
												tmpZipFile.getAbsolutePath(),
												"-C",
												path.toFile().getAbsolutePath()
										});
							}catch(Exception e) {}
						} else {
							if(tool.name.equals(ATS)){
								unzipArchive(tmpZipFile, Paths.get(atsCacheInstall));
								File oldFolderName=Paths.get(atsCacheInstall).resolve(tool.folderName).toFile();
								tool.folderName = tool.folderName.replaceFirst("^ats-","");
								File newFolderName=Paths.get(atsCacheInstall).resolve(tool.folderName).toFile();
								oldFolderName.renameTo(newFolderName);
							}
							else unzipArchive(tmpZipFile, Paths.get(atsToolsFolder));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if(tool.name.equals(ATS)) {
					tool.folder = Paths.get(atsCacheInstall).resolve(tool.folderName).toFile().getAbsolutePath();
				}else {
					tool.folder = Paths.get(atsToolsFolder).resolve(tool.folderName).toFile().getAbsolutePath();
				}
			}

			if (tool.folder == null) {
				printLog("ATS tool is not installed on this system -> " + tool.name);
				System.exit(1);
			} else {
				toolInstalled(tool, envList);
			}
		}
	}	

	private static String getAtsVersion(String atsLibsUrl, DocumentBuilder db, String pomFilePath) {

		String version = getAtsPomVersion(db, pomFilePath);

		if(version != null && version.length() > 0) {
			if("LATEST".equals(version)) {
				version = getAtsLastVersion(atsLibsUrl);
				if(version != null) {
					return version;
				}
			} else{
				return version;
			}
		}

		return DEFAULT_ATS_VERSION;
	}

	private static String getAtsPomVersion(DocumentBuilder db, String pomFilePath) {

		try (InputStream is = new FileInputStream(pomFilePath)) {

			final Document doc = db.parse(is);

			final NodeList project = doc.getElementsByTagName("project");
			if (project.getLength() > 0) {
				final NodeList projectItems = project.item(0).getChildNodes();
				for (int i=0; i < projectItems.getLength(); i++) {
					if("dependencies".equals(projectItems.item(i).getNodeName())) {

						final NodeList dependencies = projectItems.item(i).getChildNodes();
						for (int j=0; j < dependencies.getLength(); j++) {

							String artifactId = null;
							String groupId = null;
							String version = null;

							final NodeList dependency = dependencies.item(j).getChildNodes();
							for (int k=0; k < dependency.getLength(); k++) {
								if("artifactId".equals(dependency.item(k).getNodeName())) {
									artifactId = dependency.item(k).getTextContent();
								}else if("groupId".equals(dependency.item(k).getNodeName())) {
									groupId = dependency.item(k).getTextContent();
								}else if("version".equals(dependency.item(k).getNodeName())) {
									version = dependency.item(k).getTextContent();
								}
							}

							if("com.actiontestscript".equals(groupId) && "ats-automated-testing".equals(artifactId)) {
								if(!"${ats.lib.version}".equals(version)) {
									return version;
								}
							}
						}
					}
				}

				for (int i=0; i < projectItems.getLength(); i++) {
					if("properties".equals(projectItems.item(i).getNodeName())) {
						final NodeList properties = projectItems.item(i).getChildNodes();
						for (int j=0; j < properties.getLength(); j++) {
							final Node property = properties.item(j);
							if("ats.lib.version".equals(property.getNodeName())) {
								return property.getTextContent();
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	//check if suite exists
	private static void checkSuiteFileExists(String[] suites){
		for(String suite : suites){
			if(!Files.exists(Paths.get(suite))){
				printLog("Suite file not found -> " + suite);
				System.exit(0);
			}
		}
	}

	private static List<String> getAtsDefaultSuite(DocumentBuilder db, String pomFilePath) {
		try (InputStream is = new FileInputStream(pomFilePath)) {
			final Document doc = db.parse(is);
			return findSuiteXmlFiles(doc);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	private static List<String> findSuiteXmlFiles(Node node) {
		List<String> suiteFiles = new ArrayList<>();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ("suiteXmlFile".equals(child.getNodeName())) {
				suiteFiles.add(child.getTextContent());
			}
			suiteFiles.addAll(findSuiteXmlFiles(child));
		}
		return suiteFiles;
	}

	private static File download(HttpURLConnection connection, String logString) throws Exception {

		final double fileLength = (double) connection.getContentLength();
		connection.disconnect();

		Progess display;
		if (fileLength == -1) {
			display = new SizeProgress(logString, "Mo");
		} else {
			display = new PercentProgress(logString, "%");
		}

		final File tmpFile = Files.createTempFile("ats-download-", ".tmp").toFile();

		int maxTry = 10;
		while(maxTry > 0 && !tryDownload(display, fileLength, connection.getURL(), tmpFile)) {
			maxTry--;
			printLog("Error downloading archive -> " + maxTry + " tries left ...");
		}

		if(maxTry > 0) {
			return tmpFile;
		}else {
			throw(new Exception("Unable to download archive -> " + connection.getURL().getFile()));
		}
	}

	private static boolean tryDownload(Progess display, double fileLength, URL url, File tmpFile) {

		try {

			FileOutputStream fos = new FileOutputStream(tmpFile);
			BufferedInputStream ins = new BufferedInputStream(url.openStream());

			byte dataBuffer[] = new byte[1024];
			int bytesRead = 0;
			double bytesLoaded = 0D;

			while ((bytesRead = ins.read(dataBuffer, 0, 1024)) != -1) {
				bytesLoaded += bytesRead;
				fos.write(dataBuffer, 0, bytesRead);
				display.print(bytesLoaded, fileLength);
			}

			fos.close();
			ins.close();

			return true;

		} catch (Exception e) {
			return false;
		}
	}

	//------------------------------------------------------------------------------------------------------------
	// Classes
	//------------------------------------------------------------------------------------------------------------

	private static class Progess{
		private int current = 0;
		private String logPrefix;
		private String logSufix;

		public Progess(String prefix, String suffix) {
			this.logPrefix = "Download [" + prefix + "] -> ";
			this.logSufix = " " + suffix;
		}

		public void print(double v1, double v2) {
			int value = calculate(v1, v2);
			if(value % 5 == 0 && current != value) {
				System.out.println(logPrefix + value + logSufix);
				current = value;
			}
		}

		protected int calculate(double i1, double i2) {
			return 0;
		}
	}

	public static class PercentProgress extends Progess{

		public PercentProgress(String prefix, String suffix) {
			super(prefix, suffix);
		}

		@Override
		protected int calculate(double i1, double i2) {
			return (int) Math.round(i1/i2 * 100);
		}
	}

	public static class SizeProgress extends Progess{

		public SizeProgress(String prefix, String suffix) {
			super(prefix, suffix);
		}

		@Override
		protected int calculate(double i1, double i2) {
			return (int) Math.round(i1/1000000);
		}
	}

	private static class FullLogConsumer implements Consumer<String> {
		@Override
		public void accept(String s) {
			System.out.println(s);
		}
	}

	private static class TestNGLogConsumer implements Consumer<String> {
		@Override
		public void accept(String s) {
			System.out.println(
					s.replace("[TestNG]", "")
					.replace("[main] INFO org.testng.internal.Utils -", "[TestNG]")
					.replace("Warning: [org.testng.ITest]", "[TestNG] Warning :")
					.replace("[main] INFO org.testng.TestClass", "[TestNG]")
					);
		}
	}

	private static class StreamGobbler extends Thread {
		private InputStream inputStream;
		private Consumer<String> consumer;

		public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
			this.inputStream = inputStream;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
		}
	}

	private static class AtsToolEnvironment {

		public String name;
		public String envName;
		public String folder;
		public String folderName;
		public boolean check = true;

		public String url;

		public AtsToolEnvironment(String name) {
			this.name = name;
			this.envName = name.toUpperCase() + "_HOME";
		}

		public AtsToolEnvironment(String name, Path path) {
			this.name = name;
			this.envName = name.toUpperCase() + "_HOME";
			this.update(path);
		}

		public void update(Path folder) {
			this.folder = folder.toAbsolutePath().toString();
			this.folderName = folder.getFileName().toString();
			this.check = false;
		}
	}

	//------------------------------------------------------------------------------------------------------------
	// Utils
	//------------------------------------------------------------------------------------------------------------

	private static void printLog(String data) {
		System.out.println("[ATS-LAUNCHER] " + data);
	}
	
	private static void printError(String data) {
		System.out.println("[ATS-ERROR] " + data);
	}

	private static void execute(String[] commands, File currentDir, Consumer<String> outputConsumer, Consumer<String> errorConsumer, Map<String, String> execEnv) throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder(commands).directory(currentDir);
		execEnv.putAll(System.getenv());

		pb.inheritIO();
		pb.environment().putAll(execEnv);
		pb.environment().put("java.net.useSystemProxies", "true");
		
		startProcess(pb, outputConsumer, errorConsumer);
		
		printLog("Launch suites execution terminated");
	}
	
	private static void execute(String[] commands, File currentDir, Consumer<String> outputConsumer, Consumer<String> errorConsumer) throws IOException, InterruptedException {

		final ProcessBuilder pb = new ProcessBuilder(commands).directory(currentDir);

		pb.inheritIO();
		pb.environment().putAll(System.getenv());
		pb.environment().put("java.net.useSystemProxies", "true");
		
		startProcess(pb, outputConsumer, errorConsumer);
	}

	private static void execute(String[] commands) throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder(commands);
		final Process p = pb.start();
		p.waitFor();
	}
	
	private static void startProcess(ProcessBuilder pb, Consumer<String> outputConsumer, Consumer<String> errorConsumer) throws IOException, InterruptedException {
		final Process p = pb.start();

		new StreamGobbler(p.getErrorStream(), errorConsumer).start();
		new StreamGobbler(p.getInputStream(), outputConsumer).start();

		p.waitFor();
	}

	//------------------------------------------------------------------------------------------------------------
	// Files
	//------------------------------------------------------------------------------------------------------------

	private static void unzipArchive(File archive, Path destination) {
		try {
			unzipFolder(archive.toPath(), destination);
		} catch (IOException e) {
			e.printStackTrace();
		}
		archive.delete();
	}

	private static void unzipFolder(Path source, Path target) throws IOException {

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {

			ZipEntry zipEntry = zis.getNextEntry();

			while (zipEntry != null) {

				boolean isDirectory = false;

				if (zipEntry.getName().endsWith(File.separator) || zipEntry.isDirectory()) {
					isDirectory = true;
				}

				Path newPath = zipSlipProtect(zipEntry, target);

				if (isDirectory) {
					Files.createDirectories(newPath);
				} else {

					if (newPath.getParent() != null) {
						if (Files.notExists(newPath.getParent())) {
							Files.createDirectories(newPath.getParent());
						}
					}
					Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
		Path targetDirResolved = targetDir.resolve(zipEntry.getName());
		Path normalizePath = targetDirResolved.normalize();
		if (!normalizePath.startsWith(targetDir)) {
			throw new IOException("Bad zip entry: " + zipEntry.getName());
		}
		return normalizePath;
	}

	private static void copyFolder(Path src, Path dest) throws IOException {
		try (Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		}
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private static ArrayList<String> listJavaClasses(int subLen, File directory) {

		final ArrayList<String> list = new ArrayList<String>();
		final File[] fList = directory.listFiles();

		if (fList == null) {
			throw new RuntimeException("Directory list files return null value ! (" + directory.getAbsolutePath() + ")");
		} else {
			for (File file : fList) {
				if (file.isFile()) {
					if (file.getName().endsWith(".java")) {
						list.add(file.getAbsolutePath().substring(subLen).replaceAll("\\\\", "/"));
					}
				} else if (file.isDirectory()) {
					list.addAll(listJavaClasses(subLen, file));
				}
			}
		}

		return list;
	}

	private static void emptyFolder(Path folder) throws IOException {
		if (Files.exists(folder) && Files.isDirectory(folder)) {
			File[] files = folder.toFile().listFiles();
			if(files != null) {
				for(File f : files) {
					if(f.isDirectory()) {
						deleteDirectory(f.toPath());
					} else {
						f.delete();
					}
				}
			}
		}
	}

	private static void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
					Files.delete(path);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
					Files.delete(directory);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private static void isValidOutputDirectory(String strPathDirectory, String argName){
		String[] strError = new String[1];
		String strDirectory = strPathDirectory;
		boolean isValid = true;
		isValid = isValidDirectoryName(strDirectory, strError, false);

		if(strPathDirectory.contains("/")){
			strDirectory = strDirectory.substring(strDirectory.indexOf("/")+1);
		} else if(strPathDirectory.contains("\\")){
			strDirectory = strDirectory.substring(strDirectory.indexOf("\\")+1);
		}

		if(isValid){
			if( Paths.get(strPathDirectory).isAbsolute()) {
				strDirectory = Paths.get(strPathDirectory).getFileName().toString();
			}
			isValid = isValidDirectoryName(strDirectory,strError,true);
		}

		if(!isValid){
			printLog("Invalid input in "+argName+" -> " + strError[0] + "  path  : " + strPathDirectory);
			System.exit(0);
		}
	}

	private static boolean isDirectoryWritableOrCreatable(String path) {
		Path directoryPath = Paths.get(path);

		boolean canCreateDirectory = true;

		while (directoryPath != null) {
			if (Files.exists(directoryPath)) {
				if (Files.isWritable(directoryPath)) {
					break;
				} else {
					canCreateDirectory = false;
					break;
				}
			} else {
				directoryPath = directoryPath.getParent();
			}
		}

		return canCreateDirectory;
	}

	private static boolean isValidDirectoryName(String name, String[] strError, boolean fullChars) {
		strError[0] = null;

		if (name == null || name.trim().isEmpty()) {
			strError[0] = "Directory name is empty or content only spaces";
			return false;
		}

		String forbiddenChars ="";
		forbiddenChars = ( (fullChars) ? "\\/:*?\"<>|" : "*?\"<>|" );
		for (char c : forbiddenChars.toCharArray()) {
			if (name.indexOf(c) != -1) {
				strError[0] = "Directory name contains forbidden character '" + c + "'";
				return false;
			}
		}

		// Windows name reserved
		if(operatingSystem.equals(WINDOWS)) {
			String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
					"COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4",
					"LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

			for (String reservedName : reservedNames) {
				if (name.equalsIgnoreCase(reservedName)) {
					strError[0] = "Directory name is reserved by Windows";
					return false;
				}
			}
		}

		return true;
	}

	//------------------------------------------------------------------------------------------------------------
	// Tools
	//------------------------------------------------------------------------------------------------------------

	private static String getAtsLastVersion(String url) {

		final HttpURLConnection connection = createHttpConnection(url, true);

		if (connection != null) {

			try {
				final Pattern pattern = Pattern.compile("<a href=[\"'](.*)\\.zip[\"']>.*\\.zip</a>", Pattern.CASE_INSENSITIVE);

				final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

				String lastVersion = "0";
				String inputLine;

				while ((inputLine = in.readLine()) != null) { 
					Matcher matcher = pattern.matcher(inputLine);
					if(matcher.find()) {
						final String remoteVersion = matcher.group(1);
						if(String.CASE_INSENSITIVE_ORDER.compare(lastVersion, remoteVersion) < 0) {
							lastVersion = remoteVersion;
						}
					}
				}

				in.close();

				return lastVersion;

			}catch(IllegalArgumentException | IOException e) {}
		}

		return null;
	}

	private static boolean isDirectoryExistAndNoEmpty(Path folderPath) {

		if (Files.exists(folderPath)) {
			try{
				File currentFolderFile = new File(folderPath.toAbsolutePath().toString());
				return  currentFolderFile.isDirectory() && currentFolderFile.list().length > 0;
			} catch (Exception e) {}
		}
		return false;
	}

	private static boolean checkIsJenkinsOk(String atsLibsRemoteUrl, String jenkinsToolsUrl) {

		final Map<String, String[]> versions = getServerToolsVersion(jenkinsToolsUrl);
		String[] atsVersions = versions.get("ats");

		if (atsVersions != null && atsVersions.length > 1) {
			try{
				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				String pomAtsVersion = getAtsVersion(atsLibsRemoteUrl, dbf.newDocumentBuilder(), projectFolderPath.resolve("pom.xml").toAbsolutePath().toString());
				String versionElement = atsVersions[1];
				if (versionElement != null && versionElement.equals(pomAtsVersion)) {
					return true;
				} else {
					printLog("AtsLauncher error -> The version of ATS used in the project is not the same as the one used in Jenkins");
					printLog("AtsLauncher error -> Please update the version of ATS in the project or in Jenkins");
				}
			} catch (Exception e){}
		}
		printLog("AtsLauncher error -> Jenkins Url is not available, try to connect to ActionTestScript server ...");

		return false;
	}

	public static void disableSSL() throws NoSuchAlgorithmException, KeyManagementException {

		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {}

		}};

		// Install the all-trusting trust manager

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());

		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier

		HostnameVerifier allHostsValid = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	//----------------------------------------------- HttpDownloader ------------------------------------------------

	private static HttpURLConnection getConnection(String s) throws MalformedURLException, IOException, URISyntaxException {

		final URL url = new URI(s).toURL();

		if(proxy != null) {
			return (HttpURLConnection) url.openConnection(proxy);
		} else {
			return (HttpURLConnection) url.openConnection();
		}
	}

	private static HttpURLConnection createHttpConnection(String url, boolean followRedirects) {
		try {
			HttpURLConnection.setFollowRedirects(followRedirects);
			HttpURLConnection connection = getConnection(url);

			connection.setUseCaches(false);

			if (connection.getResponseCode() == 200) {
				return connection;
			} else {
				printLog("Server response error -> " + connection.getResponseCode());
				return null;
			}
		} catch (URISyntaxException | IOException e) {
			printLog("Unable to connect to server -> " + url);
			return null;
		}
	}

	private static void downloadAndExtract(String url, Path destFolder, String downloadText, String filenameVersion, boolean followRedirects) {
		HttpURLConnection connection = createHttpConnection(url, followRedirects);
		String archiveExtension = getExtension(getFilename(url));
		if (connection != null) {

			printLog("Download [" + downloadText + " (" + filenameVersion + ")] -> " + url);

			try {
				final File downloaded = download(connection, downloadText);
				if(archiveExtension.equals(TGZ)) {
					execute(
							new String[]{
									"tar",
									"-xzf",
									downloaded.getAbsolutePath(),
									"-C",
									destFolder.toFile().getAbsolutePath()});
					if (posixFilePermission != null) {
						String driverName = LINUX_DRIVER_NAME;
						if(operatingSystem.equals(MACOS)){
							driverName = MACOS_DRIVER_NAME;
						}
						final Path driverPath = destFolder.resolve(driverName);
						Files.setPosixFilePermissions(driverPath, posixFilePermission);
					}
					downloaded.delete();
				} else if(archiveExtension.equals(ZIP)) {
					unzipArchive(downloaded, destFolder);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}