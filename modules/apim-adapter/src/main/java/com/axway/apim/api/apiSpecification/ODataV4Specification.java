package com.axway.apim.api.apiSpecification;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmAnnotatable;
import org.apache.olingo.commons.api.edm.EdmAnnotation;
import org.apache.olingo.commons.api.edm.EdmAnnotations;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.EdmOperation;
import org.apache.olingo.commons.api.edm.EdmParameter;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmSchema;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.edm.EdmTerm;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.odata2.api.edm.EdmAnnotationAttribute;

import com.axway.apim.lib.errorHandling.AppException;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class ODataV4Specification extends ODataSpecification {
	
	Edm edm;
	
	@SuppressWarnings("rawtypes")
	Map<String, Schema> schemas = new HashMap<String, Schema>();
	
	Map<String, String> knownEntityTags = new HashMap<String, String>();
	
	Map<String, String> namespaceAliasMap = new HashMap<String, String>();
	
	Map<FullQualifiedName, EdmAnnotations> entityAnnotations = new HashMap<FullQualifiedName, EdmAnnotations>();

	@Override
	public APISpecType getAPIDefinitionType() throws AppException {
		return APISpecType.ODATA_V4;
	}
	
	@Override
	public boolean parse(byte[] apiSpecificationContent) throws AppException {
		try {
			super.parse(apiSpecificationContent);
			ODataClient client = ODataClientFactory.getClient();
			edm = client.getReader().readMetadata(new ByteArrayInputStream(apiSpecificationContent));
			this.openAPI = new OpenAPI();
			
			Info info = new Info();
			info.setTitle("OData Service");
			info.setDescription("The OData Service from " + apiSpecificationFile);
			// When running as part of an Integration-Test - This avoids creating a dynamic API-Specification file
			if(apiSpecificationFile.contains("ImportActionTest") ) {
				info.setDescription("The OData Service from my test file");
			}
			info.setVersion("4.0");
			openAPI.setInfo(info);
			
			List<EdmSchema> edmSchemas = edm.getSchemas();
			for(EdmSchema schema : edmSchemas) {
				info.setTitle(schema.getNamespace() + " OData Service");
				
				namespaceAliasMap.put(schema.getNamespace(), schema.getAlias());
				
				for(EdmAnnotations annotationGroup : schema.getAnnotationGroups()) {
					System.out.println(annotationGroup.getTargetPath());
					entityAnnotations.put(new FullQualifiedName(annotationGroup.getTargetPath()), annotationGroup);
				}
				
				for(EdmEntitySet entityType : schema.getEntityContainer().getEntitySets()) {
					openAPI.path(getEntityPath(entityType), getPathItemForEntity(entityType, false));
					openAPI.path(getEntityIdPath(entityType), getPathItemForEntity(entityType, true));
				}
				
				for(EdmFunction function : schema.getFunctions()) {
					openAPI.path("/" + function.getName(), getPathItemForFunction(function));
				}
				
				for(EdmAction action : schema.getActions()) {
					openAPI.path("/" + action.getName(), getPathItemForFunction(action));
				}
			}
			
			Components comp = new Components();
			comp.setSchemas(schemas);
			this.openAPI.setComponents(comp);

			return true;
		} catch (Exception e) {
			if(true || LOG.isDebugEnabled()) {
				LOG.error("Error parsing OData V4 MetaData.", e);
			}
			return false;
		}
	}
	
	private PathItem getPathItemForEntity(EdmEntitySet entity, boolean idPath) throws EdmException {
		PathItem pathItem = new PathItem();
		
		String entityName = entity.getName();
		EdmEntityType entityType = entity.getEntityType();
		
		System.out.println("entityName: " + entityName + " Container: " + entity.getEntityContainer());
		
		
		
		if(idPath) {
			// All Key-Properties are mapped to a general Id parameter and we only document it
			String paramIdDescription = "Id supports: ";
			for(EdmKeyPropertyRef key : entityType.getKeyPropertyRefs() ) {
				paramIdDescription += key.getName() + ", ";
			}
			paramIdDescription = paramIdDescription.substring(0, paramIdDescription.length()-2);
			Parameter param = new PathParameter();
			param.setName("Id");
			param.setSchema(new StringSchema());
			param.setDescription(paramIdDescription);
			pathItem.addParametersItem(param);
		}
		
		List<String> tag = new ArrayList<String>();
		if(getTitle(entity)==null) {
			tag.add(entityName);
			knownEntityTags.put(entityName, entityName);
		} else {
			String title = getTitle(entity);
			tag.add(title);
			knownEntityTags.put(title, title);
		}

		Operation operation;
		ApiResponses responses;
		// GET Method
		operation = new Operation();
		operation.setTags(tag);
		if(idPath) {
			operation.setSummary("Get " + entityName + " on Id");
			operation.setOperationId("get"+entityName+"Id");
		} else {
			operation.setSummary("Get " + entityName);
			operation.setOperationId("get"+entityName);
		}
		
		String operationDescription = "Returns the entity: " + entityName + ". "
				+ "For more information on how to access entities visit: <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part2-url-conventions.html#sec_AddressingEntities\">Addressing Entities</a>";
		List<String> navProperties = new ArrayList<String>();
		List<String> structProperties = entityType.getPropertyNames();
		
		if(entityType.getNavigationPropertyNames()!=null && entityType.getNavigationPropertyNames().size()>0) {
			for(String navigationProperty : entityType.getNavigationPropertyNames()) {
				navProperties.add(navigationProperty);
			}
			operationDescription += "<br /><br />The entity: " + entityName + " supports the following navigational properties: "+navProperties;
			operationDescription += "<br />For example: .../" + entityName + "(Entity-Id)/<b>" + navProperties.get(0) + "</b>/.....";
		} else {
			operationDescription += "<br />The entity: " + entityName + " supports <b>no</b> navigational properties.";
		}
		
		ArraySchema stringArraySchema = new ArraySchema();
		stringArraySchema.setItems(new StringSchema());
		
		operation.setDescription(operationDescription);
		operation.addParametersItem(createParameter("$expand", "The syntax of a $expand query option is a comma-separated list of Navigation Properties. Additionally each Navigation Property can be followed by a forward slash and another Navigation Property to enable identifying a multi-level relationship."
				+ "<br /><br />See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html#sec_SystemQueryOptionexpand\">Expand</a>", new StringSchema(), getExample(navProperties) ));
		operation.addParametersItem(createParameter("$select", "The value of a $select System Query Option is a comma-separated list of selection clauses. Each selection clause may be a Property name, Navigation Property name, or the \"*\" character. "
				+ "<br /><br /></i>You may use: "+structProperties+"</i>. "
				+ "<br />See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html#sec_SystemQueryOptionselect\">Select</a>", new StringSchema(), getExample(structProperties) ));
		if(!idPath) { // When requesting with specific ID the following parameters are not required/meaningful
			operation.addParametersItem(createParameter("$filter", "Filter items by property values. See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part2-url-conventions.html#sec_SystemQueryOptionfilter\">Filter</a>", new StringSchema()));
			operation.addParametersItem(createParameter("$orderby", "Order items by property values. "
					+ "<br /><br />You may use: "+structProperties+". "
					+ "<br />See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part2-url-conventions.html#sec_SystemQueryOptionorderby\">Orderby</a>", new StringSchema(), getExample(structProperties)));
			IntegerSchema topSchema = new IntegerSchema();
			topSchema.setDefault(10);
			operation.addParametersItem(createParameter("$top", "Show only the first n items. See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html#sec_SystemQueryOptiontop\">Top</a>", topSchema, "5"));
			operation.addParametersItem(createParameter("$skip", "Skip the first n items. See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html#sec_SystemQueryOptionskip\">Skip</a>", new IntegerSchema(), "20"));
			operation.addParametersItem(createParameter("$count", "Include count of items. See <a target=\"_blank\" href=\"http://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html#sec_SystemQueryOptioncount\">Count</a>", new BooleanSchema()));
			
		}
		operation.addParametersItem(createParameter("$format", "Response format if supported by the backend service.", new StringSchema(), "json"));
		
		responses = new ApiResponses()
				.addApiResponse("200", createResponse("EntitySet " + entityName, 
						getSchemaForType(entityType, (idPath) ? true : false)))
				._default(createResponse("Unexpected error"));
		operation.setResponses(responses);
		pathItem.operation(HttpMethod.GET, operation);
		operation.setDescription(operationDescription);
		
		if(!idPath) return pathItem;
		
		// POST Method
		operation = new Operation();
		operation.setTags(tag);
		operation.setSummary("Create a new entity " + entityName);
		operation.setOperationId("create"+entityName);
		operation.setDescription("Create a new entity in EntitySet: " + entityName);
		operation.setRequestBody(createRequestBody(entityType, "The entity to create", true));
		
		responses = new ApiResponses()
				.addApiResponse("201", createResponse("EntitySet " + entityName))
				._default(createResponse("Unexpected error"));
		operation.setResponses(responses);
		pathItem.operation(HttpMethod.POST, operation);
		
		// PATCH Method
		operation = new Operation();
		operation.setTags(tag);
		operation.setSummary("Update entity " + entityName);
		operation.setOperationId("update"+entityName);
		operation.setDescription("Update an existing entity: " + entityName);
		operation.setRequestBody(createRequestBody(entityType, "The entity to update", true));
		
		responses = new ApiResponses()
				.addApiResponse("200", createResponse("EntitySet " + entityName))
				._default(createResponse("Unexpected error"));
		operation.setResponses(responses);
		pathItem.operation(HttpMethod.PATCH, operation);
		
		// DELETE Method
		operation = new Operation();
		operation.setTags(tag);
		operation.setSummary("Delete " + entityName);
		operation.setOperationId("delete"+entityName);
		operation.setDescription("Delete an entity " + entityName);
		
		responses = new ApiResponses()
				.addApiResponse("204", createResponse("Entity " + entityName + " successfully deleted"))
				._default(createResponse("Unexpected error"));
		operation.setResponses(responses);
		pathItem.operation(HttpMethod.DELETE, operation);
		
		return pathItem;
	}
	
	public StringSchema getSchemaAllowedValues(Enum[] allowedValues) {
		StringSchema schema = new StringSchema();
		for(Enum allowedValue : allowedValues) {
			schema.addEnumItemObject(allowedValue.name());
		}
		return schema;
	}
	
	private String getEntityIdPath(EdmEntitySet entityType) throws EdmException {
		String singleEntityPath = "/" + entityType.getName() + "({Id})*";
		return singleEntityPath;
	}
	
	private String getEntityPath(EdmEntitySet entityType) throws EdmException {
		String singleEntityPath = "/" + entityType.getName() + "*";
		return singleEntityPath;
	}
	
	private PathItem getPathItemForFunction(EdmOperation function) throws EdmException {
		PathItem pathItem = new PathItem();
		Operation operation = new Operation();
		List<String> tag = new ArrayList<String>();
		boolean hasReturnType = true;
		if(function.getReturnType()==null) {
			hasReturnType = false;
		}
		// Add functions to the same group as the entity itself that is returned, but only if already presents
		if(hasReturnType && function.getReturnType().getType().getKind() == EdmTypeKind.ENTITY 
				&& knownEntityTags.containsKey(function.getReturnType().getType().getName())) {
			tag.add(function.getReturnType().getType().getName());
		} else {
			tag.add("Service operations");
		}
		
		operation.setTags(tag);
		operation.setOperationId(function.getName());
		// setFunctionDocumentation(function, operation);
		
		for(String parameterName : function.getParameterNames()) {
			EdmParameter param = function.getParameter(parameterName);
			operation.addParametersItem(createParameter(param));
		}
		
		pathItem.operation(HttpMethod.valueOf("GET"), operation);
		
		try {
			ApiResponses responses = new ApiResponses()
					.addApiResponse("200", createResponse(
							function.getReturnType().getType().getName(), 
							getSchemaForType(function.getReturnType().getType(), function.getReturnType().isCollection())))
					._default(createResponse("Unexpected error"));
			operation.setResponses(responses);
		} catch (Exception e) {
			// Happens for instance, when the given returnType cannot be resolved or is null
			if(hasReturnType) {
				LOG.error("Error setting response for function: " + function.getName() + ". Creating standard response.", e);
			}
			ApiResponses responses = new ApiResponses()
					.addApiResponse("200", createResponse(function.getName(), new StringSchema()))
					._default(createResponse("Unexpected error"));
			operation.setResponses(responses);
		}
		return pathItem;
	}

	private Parameter createParameter(EdmParameter param) throws EdmException {
		Schema<?> schema = getSchemaForType(param.getType(), param.isCollection());
		Parameter parameter = createParameter(param.getName(), getDescription(param), schema);
		parameter.setRequired(!param.isNullable());
		return parameter;
	}
	
	private Parameter createParameter(String name, String description, Schema<?> schema) {
		return createParameter(name, description, schema, null);
	}
	
	private Parameter createParameter(String name, String description, Schema<?> schema, String example) {
		Parameter param = new QueryParameter();
		param.setName(name);
		param.setDescription(description);
		param.setSchema(schema);
		if(example!=null) param.setExample(example);
		return param;
	}
	
	private ApiResponse createResponse(String description) throws EdmException {
		return createResponse(description, null);
	}
	
	private ApiResponse createResponse(String description, Schema<?> schema) throws EdmException {
		ApiResponse response = new ApiResponse();
		response.setDescription(description);
		Content content = new Content();
		MediaType mediaType = new MediaType();
		mediaType.setSchema(schema);
		content.addMediaType("application/json", mediaType);
		response.setContent(content);
		return response;
	}
	
	private RequestBody createRequestBody(EdmEntityType entityType, String description, boolean required) {
		RequestBody body = new RequestBody();
		body.setDescription(description);
		body.setRequired(required);
		Content content = new Content();
		MediaType mediaType = new MediaType();
		mediaType.setSchema(getSchemaForType(entityType, false));
		content.addMediaType("application/json", mediaType);
		body.setContent(content);
		return body;
	}
	
	
	/*private Schema<?> getSchemaForType(EdmType type) throws EdmException {
		return getSchemaForType(type, EdmMultiplicity.ONE);
	}*/
	
	private Schema<?> getSchemaForType(EdmType type, boolean isCollection) {
		try {
			if(type.getKind()==EdmTypeKind.PRIMITIVE) {
				return getSchemaForType(type, false, isCollection);
			} else {
				return getSchemaForType(type, true, isCollection);
			}
		} catch (EdmException e) {
			try {
				LOG.error("Error getting schema for type: " + type.getName());
			} catch (EdmException e1) {
			}
			return null;
		}
	}
	
	private Schema<Object> getSchemaForType(EdmType type, boolean asRef, boolean isCollection) throws EdmException {
		Schema schema = null;
		if(type.getKind()==EdmTypeKind.PRIMITIVE) {
			schema = (Schema<Object>)getSimpleSchema(type.getName());
		} else if(type.getKind()==EdmTypeKind.ENUM) {
			schema = new StringSchema();
			EdmEnumType enumType = (EdmEnumType)type;
			for(String member : enumType.getMemberNames()) {
				schema.addEnumItemObject(member);
			}
		} else if(type.getKind()==EdmTypeKind.ENTITY || type.getKind()==EdmTypeKind.COMPLEX) {
			EdmStructuredType entityType;
			if(type.getKind()==EdmTypeKind.ENTITY) {
				entityType = edm.getEntityType(new FullQualifiedName(type.getNamespace() , type.getName()));
			} else {
				entityType = edm.getComplexType(new FullQualifiedName(type.getNamespace() , type.getName()));
			}
			// Check, if the type has been created already
			if(schemas.containsKey(type.getName())) {
				schema = schemas.get(type.getName());
			} else {
				// Create an ObjectSchema that contains all declared properties
				schema = new ObjectSchema();
				for(String propertyName : entityType.getPropertyNames()) {
					EdmProperty property = (EdmProperty)entityType.getProperty(propertyName);
					Schema<Object> propSchema = getSchemaForType(property.getType(), true, property.isCollection());
					propSchema.setMaxLength(property.getMaxLength());
					propSchema.setDefault(property.getDefaultValue());
					propSchema.setNullable(property.isNullable());
					//propSchema.setTitle(property. );
					// propSchema.setDescription(property.);
					schema.addProperties(propertyName, propSchema);
				}
				EdmStructuredType typeImpl = (EdmStructuredType)type;
				schema.setDescription(getDescription(typeImpl));
				schema.setTitle(getTitle(typeImpl));
			}
			schemas.put(type.getName(), schema);
			if(asRef) {
				return new Schema<Object>().$ref(type.getName());
			}
		}
		if(isCollection) {
			Schema collectionSchema = new ArraySchema();
			collectionSchema.setItems(schema);
			return collectionSchema;
		} else {
			return schema;
		}
	}
	
	private String getDescription(EdmAnnotatable entity) {
		try {
			String summary = null;
			String longDescription = null;
			String quickInfo = getQuickInfo(entity);
			if(entity.getAnnotations()==null) return null;
			for(EdmAnnotation annoElem : entity.getAnnotations() ) {
				/*if("documentation".equals(annoElem. getName().toLowerCase())) {
					for(EdmAnnotationElement child : annoElem.getChildElements()) {
						if("summary".equals(child.getName().toLowerCase())) {
							summary = child.getText();
						}
						if("longdescription".equals(child.getName().toLowerCase())) {
							longDescription = child.getText();
							continue;
						}						
					}
				}*/
			}
			if(summary==null && longDescription == null && quickInfo == null) return null;
			String description = "";
			if(quickInfo != null) description = quickInfo;
			if(!description.equals("") && summary != null) description += "<br />";
			if(summary != null) description += summary;
			if(!description.equals("") && longDescription != null) description += "<br />";
			if(longDescription != null) description += longDescription;
			return description;
		} catch (EdmException e) {
			return null;
		}
	}
	
	private String getTitle(EdmAnnotatable entity) {
		return getFromAnnotationAttributes(entity, "label");
	}
		
	private String getQuickInfo(EdmAnnotatable entity) {
		return getFromAnnotationAttributes(entity, "quickinfo");
	}
	
	private String getFromAnnotationAttributes(EdmAnnotatable entity, String annotationName) {
		entity = entity;
		try {
			/*if(entity.getAnnotations()!=null && entity.getAnnotations().getAnnotationAttributes()!=null) {
				for(EdmAnnotationAttribute attribute : entity.getAnnotations().getAnnotationAttributes()) {
					if(annotationName.equals(attribute.getName().toLowerCase())) {
						return attribute.getText();
					}
				}
			}*/
			return null;
		} catch (EdmException e) {
			return null;
		}
	}
	
	private String getExample(List<String> possibleExamples) {
		// Avoid providing a example such ID, id as it is in most cases not the best option for an example
		for(String example : possibleExamples) {
			if(!example.toLowerCase().equals("id")) return example;
		}
		return null;
	}
	
	private boolean isEntityInsertable(EdmEntitySet entity) {
		EdmAnnotations annotations = getEntityAnnotations(entity);
		EdmTerm term = edm.getTerm(new FullQualifiedName("SAP__self", "SAP__capabilities.SearchRestrictions"));
		//annotations.getAnnotation(, null);
		if(annotations == null) return true;
		
		/*for(EdmAnnotation annotation : annotations.getAnnotationGroup().getAnnotations()) {
			annotation = annotation;
			EdmAnnotationsImp
			//EdmTerm term = edm.getTerm(new FullQualifiedName("SAP__capabilities.SearchRestrictions"));
			
		}*/
		return false;
	}
	
	private EdmAnnotations getEntityAnnotations(EdmEntitySet entity) {
		EdmEntityContainer container = entity.getEntityContainer();
		// Check if an alias exists for the namespace, which might be used for annotation target
		if(this.namespaceAliasMap.get(container.getNamespace()) != null) {
			String namespaceAlias = this.namespaceAliasMap.get(container.getNamespace());
			// Get the annotation using the alias instead of the namespace itself
			return this.entityAnnotations.get(new FullQualifiedName(namespaceAlias, entity.getEntityContainer() + "/" + entity.getName()));
		} else {
			// Otherwise try to get the annotations 
			return this.entityAnnotations.get(null);
		}
	}
}
