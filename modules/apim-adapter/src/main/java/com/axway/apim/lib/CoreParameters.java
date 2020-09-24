package com.axway.apim.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter.CacheType;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.errorHandling.ErrorState;
import com.axway.apim.lib.utils.TestIndicator;

public class CoreParameters {
	
	private static Logger LOG = LoggerFactory.getLogger(CoreParameters.class);
	
	public enum Mode {
		replace, 
		ignore, 
		add;
		
		public static Mode valueOfDefault(String key) {
			for (Mode e : values()) {
		        if (e.name().equals(key)) {
		            return e;
		        }
		    }
			return Mode.add;
		}
	}
	
	public static String APIM_CLI_HOME = "AXWAY_APIM_CLI_HOME";
	
	private static CoreParameters instance;
	
	private List<CacheType> cachesToClear = null;
	
	int defaultPort = 8075;
	
	/**
	 * These properties are used for instance to translate key in given config files
	 */
	private Map<String, String> properties;
	
	private String stage;
	
	private String returnCodeMapping;
	
	private String clearCache;
	
	private String hostname;
	
	private int port = -1;
	
	private String adminUsername;
	
	private String adminPassword;
	
	private String username;
	
	private String password;
	
	private Boolean ignoreAdminAccount = false;
	
	private Boolean allowOrgAdminsToPublish = true;
	
	private Boolean force;
	
	private Boolean ignoreQuotas;
	
	private Mode quotaMode = Mode.add;
	private Mode clientAppsMode = Mode.add;
	private Mode clientOrgsMode = Mode.add;
	
	private String detailsExportFile;
	
	private Boolean replaceHostInSwagger = true;
	
	private Boolean rollback = true;
	
	private String confDir;
	
	private Boolean ignoreCache = false;
	
	private String apimCLIHome;
	
	public CoreParameters() {
		super();
		CoreParameters.instance = this;
	}

	public static synchronized CoreParameters getInstance() {
		if(CoreParameters.instance == null && TestIndicator.getInstance().isTestRunning()) {
			return new CoreParameters(); // Skip this, just return an empty CommandParams to avoid NPE
		}
		return CoreParameters.instance;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getReturnCodeMapping() {
		return returnCodeMapping;
	}

	public void setReturnCodeMapping(String returnCodeMapping) {
		this.returnCodeMapping = returnCodeMapping;
	}

	public String getClearCache() {
		return clearCache;
	}

	public void setClearCache(String clearCache) {
		this.clearCache = clearCache;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostname() {
		return hostname;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		if(port==-1) return defaultPort;
		return port;
	}

	public String getUsername() {
		if(username!=null) {
			return username;
		} else {
			// Perhaps the admin_username is given
			return getAdminUsername();
		}
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		if(password!=null) {
			return password;
		} else {
			// Perhaps the admin_password is given (hopefully in combination with the admin_username)
			return getAdminPassword();
		}
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}
	
	public Boolean isForce() {
		return force;
	}

	public void setForce(Boolean force) {
		if(force==null) return;
		this.force = force;
	}

	public void setIgnoreQuotas(Boolean ignoreQuotas) {
		if(ignoreQuotas==null) return;
		this.ignoreQuotas = ignoreQuotas;
	}
	
	public Boolean isIgnoreQuotas() {
		if(ignoreQuotas==null) return false;
		return ignoreQuotas;
	}

	public Mode getQuotaMode() {
		return quotaMode;
	}

	public void setQuotaMode(Mode quotaMode) {
		if(quotaMode==null) return;
		this.quotaMode = quotaMode;
	}

	public Boolean isIgnoreClientApps() {
		if(clientAppsMode==Mode.ignore) return true;
		return false;
	}
	
	public Mode getClientAppsMode() {
		return clientAppsMode;
	}

	public void setClientAppsMode(Mode clientAppsMode) {
		if(clientAppsMode==null) return; // Stick with the default
		this.clientAppsMode = clientAppsMode;
	}

	public Boolean isIgnoreClientOrgs() {
		if(clientOrgsMode==Mode.ignore) return true;
		return false;
	}
	
	public Mode getClientOrgsMode() {
		return clientOrgsMode;
	}

	public void setClientOrgsMode(Mode clientOrgsMode) {
		if(clientOrgsMode==null) return; // Stick with the default
		this.clientOrgsMode = clientOrgsMode;
	}

	public String getAPIManagerURL() {
		return "https://"+this.getHostname()+":"+this.getPort();
	}
	
	public Boolean isIgnoreAdminAccount() {
		return ignoreAdminAccount;
	}

	public void setIgnoreAdminAccount(Boolean ignoreAdminAccount) {
		if(ignoreAdminAccount==null) return;
		this.ignoreAdminAccount = ignoreAdminAccount;
	}
	
	public Boolean isAllowOrgAdminsToPublish() {
		return allowOrgAdminsToPublish;
	}

	public void setAllowOrgAdminsToPublish(Boolean allowOrgAdminsToPublish) {
		if(allowOrgAdminsToPublish==null) return;
		this.allowOrgAdminsToPublish = allowOrgAdminsToPublish;
	}
	
	public String getDetailsExportFile() {
		return detailsExportFile;
	}

	public void setDetailsExportFile(String detailsExportFile) {
		this.detailsExportFile = detailsExportFile;
	}
	
	public boolean isReplaceHostInSwagger() {
		return replaceHostInSwagger;
	}

	public void setReplaceHostInSwagger(Boolean replaceHostInSwagger) {
		if(replaceHostInSwagger==null) return;
		this.replaceHostInSwagger = replaceHostInSwagger;
	}
	
	public Boolean isRollback() {
		return rollback;
	}

	public void setRollback(Boolean rollback) {
		if(rollback==null) return;
		this.rollback = rollback;
	}
	
	public String getConfDir() {
		return confDir;
	}

	public void setConfDir(String confDir) {
		this.confDir = confDir;
	}

	public boolean isIgnoreCache() {
		return ignoreCache;
	}

	public void setIgnoreCache(Boolean ignoreCache) {
		if(ignoreCache==null) return;
		this.ignoreCache = ignoreCache;
	}

	public String getApimCLIHome() {
		return apimCLIHome;
	}

	public void setApimCLIHome(String apimCLIHome) {
		this.apimCLIHome = apimCLIHome;
	}

	public List<CacheType> clearCaches() {
		if(getClearCache()==null) return null;
		if(cachesToClear!=null) return cachesToClear;
		cachesToClear = new ArrayList<CacheType>();
		for(String cacheName : getClearCache().split(",")) {
			if(cacheName.equals("ALL")) cacheName = "*";
			cacheName = cacheName.trim();
			if(cacheName.contains("*")) {
				Pattern pattern = Pattern.compile(cacheName.replace("*", ".*").toLowerCase());
				for(CacheType cacheType : CacheType.values()) {
					Matcher matcher = pattern.matcher(cacheType.name().toLowerCase());
					if(matcher.matches()) {
						cachesToClear.add(cacheType);
					}
				}
			} else {
				try {
					cachesToClear.add(CacheType.valueOf(cacheName));
				} catch (IllegalArgumentException e) {
					LOG.error("Unable to clear cache: " +cacheName + " as the cache is unknown.");
					LOG.error("Available caches: " + Arrays.asList(CacheType.values()));
				}
			}
		}
		return cachesToClear;
	}
	
	
	public void validateRequiredParameters() throws AppException {
		boolean parameterMissing = false;
		if(getUsername()==null && getAdminUsername()==null) {
			parameterMissing = true;
			LOG.error("Required parameter: 'username' or 'admin_username' is missing.");
		}
		if(getPassword()==null && getAdminPassword()==null) {
			parameterMissing = true;
			LOG.error("Required parameter: 'password' or 'admin_password' is missing.");
		}
		if(getHostname()==null) {
			parameterMissing = true;
			LOG.error("Required parameter: 'host' is missing.");
		}
		if(parameterMissing) {
			LOG.error("Missing required parameters. Use either Command-Line-Options or Environment.Properties to provided required parameters.");
			LOG.error("Get help with option -h");
			LOG.error("");
			ErrorState.getInstance().setError("Missing required parameters.", ErrorCode.MISSING_PARAMETER, false);
			throw new AppException("Missing required parameters.", ErrorCode.MISSING_PARAMETER);
		}
	}
	
	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}
