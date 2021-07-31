package UITestFramework;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import logger.Log;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.log4j.PropertyConfigurator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;


	@Slf4j
	public class CreateSession {

		public WebDriver driver = null;
		Properties configFile;
		protected static Properties lobConfigProp = new Properties();
		protected static Properties localeConfigProp = new Properties();
		protected FileInputStream configFis, lobConfigFis, localeConfigFis;
		public Properties testDataFile;
		private final String CONFIG_FILE_PATH = "//src//main//java//config//config.properties";
		protected File file = new File("");
		Properties configProp = new Properties();
		String OS;
		private static AppiumDriverLocalService appiumService;


		/**
		 * this method starts Appium server. Calls startAppiumServer method to start the session depending upon your OS.
		 *
		 * @throws Exception Unable to start appium server
		 */
		@BeforeSuite
		public void invokeAppium() throws Exception {
			OS = System.getProperty("os.name").toLowerCase();
			try {
				startAppiumServer(OS);
				Log.info("Appium server started successfully");
			} catch (Exception e) {
				Log.logError(getClass().getName(), "startAppium", "Unable to start appium server");
				throw new Exception(e.getMessage());
			}
		}

		/**
		 * this method stops Appium server.Calls stopAppiumServer method to
		 * stop session depending upon your OS.
		 *
		 * @throws Exception Unable to stop appium server
		 */
		@AfterSuite
		public void stopAppium() throws Exception {
			try {
				stopAppiumServer(OS);
				Log.info("Appium server stopped successfully");

			} catch (Exception e) {
				Log.logError(getClass().getName(), "stopAppium", "Unable to stop appium server");
				throw new Exception(e.getMessage());
			}
		}


		/**
		 * this method creates the driver depending upon the passed parameter (android or iOS)
		 * and loads the properties files (config and test data properties files).
		 *
		 * @param os         android or iOS
		 * @param methodName - name of the method under execution
		 * @throws Exception issue while loading properties files or creation of driver.
		 */
		//@Parameters({"os"})
		//@BeforeMethod
		public void createDriver(String os, Method methodName, String apkPath, String appPackageValue, String appActivityValue) throws Exception {

			propertiesFileLoad(os);

			File propertiesFile = new File(file.getAbsoluteFile() + "//src//main//java//log4j.properties");
			PropertyConfigurator.configure(propertiesFile.toString());
			Log.info("--------------------------------------");


			if (os.equalsIgnoreCase("android")) {
				String buildPath = choosebuild(os,apkPath);
				androidDriver(buildPath, methodName,appPackageValue,appActivityValue);
				Log.info("Android driver created");

			} else if (os.equalsIgnoreCase("iOS")) {
				String buildPath = choosebuild(os,apkPath);
				iOSDriver(buildPath, methodName);
				Log.info("iOS driver created");
			}
		}

		/**
		 * this method quit the driver after the execution of test(s)
		 */
		@AfterMethod
		public void teardown() {
			Log.info("Shutting down driver");
			driver.quit();
		}

		/**
		 * @return
		 */
		public WebDriver getDriver() {
			return driver;
		}


		/**
		 * this method creates the android driver
		 *
		 * @param buildPath  - path to pick the location of the app
		 * @param methodName - name of the method under execution
		 * @throws MalformedURLException Thrown to indicate that a malformed URL has occurred.
		 */
		public synchronized void androidDriver(String buildPath, Method methodName, String appPackageValue, String appActivityValue) throws MalformedURLException {
			File app = new File(buildPath);
			DesiredCapabilities capabilities = new DesiredCapabilities();
			//capabilities.setCapability("deviceName", "Android Emulator");
			capabilities.setCapability("version", "6.0");
			capabilities.setCapability("deviceName", "android");
			capabilities.setCapability("platformName", "Android");
			capabilities.setCapability("appPackage", appPackageValue);
			capabilities.setCapability("appActivity", appActivityValue);
			//capabilities.setCapability("name", methodName.getName());
			capabilities.setCapability("app", app.getAbsolutePath());
			capabilities.setCapability(MobileCapabilityType.NO_RESET, false);
			capabilities.setCapability("automationName", "UiAutomator2");
			driver = new AndroidDriver(new URL("http://localhost:4444/wd/hub"), capabilities);

		}

		/**
		 * this method creates the iOS driver
		 *
		 * @param buildPath-  path to pick the location of the app
		 * @param methodName- name of the method under execution
		 * @throws MalformedURLException Thrown to indicate that a malformed URL has occurred.
		 */
		public void iOSDriver(String buildPath, Method methodName) throws MalformedURLException {
			File app = new File(buildPath);
			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability("platformName", "iOS");
			capabilities.setCapability("platformVersion", "14.2");
			//capabilities.setCapability("appiumVersion", "1.3.7");
			capabilities.setCapability("name", methodName.getName());
			capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "iPhone Simulator");
			capabilities.setCapability("app", app.getAbsolutePath());
			capabilities.setCapability("udid", "59E5D48E-E91D-4579-9E0E-A8598C36688F");
			capabilities.setCapability(MobileCapabilityType.NO_RESET, false);
			driver = new IOSDriver(appiumService.getUrl(), capabilities);

		}


		/**
		 * this method starts the appium  server depending on your OS.
		 *
		 * @param os your machine OS (windows/linux/mac)
		 * @throws IOException          Signals that an I/O exception of some sort has occurred
		 * @throws ExecuteException     An exception indicating that the executing a subprocesses failed
		 * @throws InterruptedException Thrown when a thread is waiting, sleeping,
		 *                              or otherwise occupied, and the thread is interrupted, either before
		 *                              or during the activity.
		 */
		public void startAppiumServer(String os) throws ExecuteException, IOException, InterruptedException {
			AppiumServiceBuilder builder;
			if (os.contains("windows")) {
				CommandLine command = new CommandLine("cmd");
				command.addArgument("/c");
				command.addArgument("C:/Program Files/nodejs/node.exe");
				command.addArgument("C:/Appium/node_modules/appium/bin/appium.js");
				command.addArgument("--address", false);
				command.addArgument("127.0.0.1");
				command.addArgument("--port", false);
				command.addArgument("4723");
				command.addArgument("--full-reset", false);

				DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
				DefaultExecutor executor = new DefaultExecutor();
				executor.setExitValue(1);
				executor.execute(command, resultHandler);
				Thread.sleep(5000);
			} else if (os.contains("mac os x")) {
				builder = new AppiumServiceBuilder()
						.usingAnyFreePort()
						.withArgument(GeneralServerFlag.SESSION_OVERRIDE)
						.withArgument(GeneralServerFlag.LOG_LEVEL, "error");
				appiumService = builder.build();
				appiumService.start();
				Log.info("Appium started on " + appiumService.getUrl());
			} else if (os.contains("linux")) {
				//Start the appium server
				System.out.println("ANDROID_HOME : ");
				System.getenv("ANDROID_HOME");
				//	System.out.println("PATH :" +System.getenv("PATH"));
				CommandLine command = new CommandLine("/bin/bash");
				command.addArgument("-c");
				command.addArgument("~/.linuxbrew/bin/node");
				command.addArgument("~/.linuxbrew/lib/node_modules/appium/lib/appium.js", true);
				DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
				DefaultExecutor executor = new DefaultExecutor();
				executor.setExitValue(1);
				executor.execute(command, resultHandler);
				Thread.sleep(5000); //Wait for appium server to start

			} else {
				Log.info(os + "is not supported yet");
			}
		}

		/**
		 * this method stops the appium  server.
		 *
		 * @param os your machine OS (windows/linux/mac).
		 * @throws IOException      Signals that an I/O exception of some sort has occurred.
		 * @throws ExecuteException An exception indicating that the executing a subprocesses failed.
		 */
		public void stopAppiumServer(String os) throws ExecuteException, IOException {
			if (os.contains("windows")) {
				CommandLine command = new CommandLine("cmd");
				command.addArgument("/c");
				command.addArgument("Taskkill /F /IM node.exe");

				DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
				DefaultExecutor executor = new DefaultExecutor();
				executor.setExitValue(1);
				executor.execute(command, resultHandler);
			} else if (os.contains("mac os x")) {
				appiumService.stop();
			} else if (os.contains("linux")) {
				// need to add it
			}
		}

		/**
		 * this method loads properties files config and file having test data.
		 *
		 * @param platform android or ios, to load specific test data file.
		 * @throws Exception property files are not loaded successfully
		 */
		public void propertiesFileLoad(String platform) throws Exception {
			configFis = new FileInputStream(file.getAbsoluteFile()
					+ CONFIG_FILE_PATH);
			configProp.load(configFis);

			File f = new File(file.getAbsoluteFile() + "//src//main//java//config//" + platform
					+ "_config.properties");

			if (f.exists() && !f.isDirectory()) {
				lobConfigFis = new FileInputStream(file.getAbsoluteFile()
						+ "/src//main//java//config//" + platform + "_config.properties");
				lobConfigProp.load(lobConfigFis);

				String locale = lobConfigProp.getProperty("LOCALE");

				localeConfigFis = new FileInputStream(file.getAbsoluteFile()
						+ "//src//main//java//testData//" + locale + "_" + platform + ".properties");
				localeConfigProp.load(localeConfigFis);
			} else {
				throw new Exception("Properties files loading failed ");
			}
		}

		public String choosebuild(String invokeDriver, String apkName) {
			String appPath = null;
			if (invokeDriver.equals("android")) {
				appPath = configProp.getProperty(apkName);
				return appPath;
			} else if (invokeDriver.equals("iOS")) {
				appPath = configProp.getProperty(apkName);
				return appPath;
			}

			return appPath;
		}


	}

