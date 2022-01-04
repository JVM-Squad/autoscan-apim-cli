package com.axway.apim.api.model;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class QuotaRestrictionDeserializer extends JsonDeserializer<QuotaRestriction> {
	
	private final static String validPeriods = "month|week|day|hour|minute|second";

	@SuppressWarnings("serial")
	@Override
	public QuotaRestriction deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectCodec oc = jp.getCodec();
		JsonNode node = oc.readTree(jp);
		String type = node.get("type").asText();
		JsonNode quotaConfig = node.get("config");
		if(type.equals("throttlemb")) {
			if(!quotaConfig.has("period") || !quotaConfig.has("per") || !quotaConfig.has("mb")) {
				throw new AppException("Invalid quota config. For type 'throttlemb' the following configs are required: period, per, mb", ErrorCode.INVALID_QUOTA_CONFIG);
			}
		} else if(type.equals("throttle")) {
			if(!quotaConfig.has("period") || !quotaConfig.has("per") || !quotaConfig.has("messages")) {
				throw new AppException("Invalid quota config. For type 'throttle' the following configs are required: period, per, messages", ErrorCode.INVALID_QUOTA_CONFIG);
			}
		} else {
			throw new AppException("Unsupported Quota-Type: '" + type + "'. Must be either: throttle or throttlemb", ErrorCode.INVALID_QUOTA_CONFIG);
		}
		String period = quotaConfig.get("period").asText();
		String per = quotaConfig.get("per").asText();
		Pattern pattern = Pattern.compile("^("+validPeriods+")$");
		Matcher matcher = pattern.matcher(period);
		if(!matcher.matches()) {
			throw new AppException("Invalid quota period: '"+period+"'. Must be one of the following: "+validPeriods, ErrorCode.INVALID_QUOTA_CONFIG);
		}
		QuotaRestriction restriction = new QuotaRestriction();
		restriction.setType(QuotaRestrictiontype.valueOf(type));
		restriction.setMethod(node.get("method").asText());
		Map<String, String> configMap = new LinkedHashMap<String, String>();
		configMap.put("period", period);
		configMap.put("per", per);
		if(node.has("api")) {
			restriction.setApiId(node.get("api").asText());
		}
		if(type.equals("throttle")) {
			configMap.put("messages", quotaConfig.get("messages").asText());
		} else {
			configMap.put("mb", quotaConfig.get("mb").asText());
		}
		restriction.setConfig(configMap);
		return restriction;
	}
}
