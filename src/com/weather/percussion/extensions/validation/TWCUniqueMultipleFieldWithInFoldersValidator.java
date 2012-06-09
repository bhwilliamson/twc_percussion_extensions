package com.weather.percussion.extensions.validation;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.pso.utils.PSOExtensionParamsHelper;
import com.percussion.pso.utils.PSONodeCataloger;
import com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;

public class TWCUniqueMultipleFieldWithInFoldersValidator extends PSOUniqueFieldWithInFoldersValidator {
	
	public TWCUniqueMultipleFieldWithInFoldersValidator() {
	    super();
	}
	
    public Boolean processUdf(Object params[], IPSRequestContext request)
        throws PSConversionException {	
        log.debug("Entering processUdf()");
        try {
            String cmd = request.getParameter(SYS_COMMAND_PARAM);
            String actionType = request.getParameter(DB_ACTION_TYPE_PARAM);
            log.debug((new StringBuilder()).append("cmd param: ").append(cmd));
            log.debug((new StringBuilder()).append("action type param: ").append(actionType));
            
            //Return true if action type not valid
            if(actionType == null || !actionType.equals(ACTION_TYPE_INSERT) && !actionType.equals(ACTION_TYPE_UPDATE)) {
                log.debug("Action type invalid, returning true");
                return true;       
            }
            
            PSOExtensionParamsHelper paramsHelper = new PSOExtensionParamsHelper(getExtensionDef(), params, request, log);
            String fieldNames = paramsHelper.getRequiredParameter(EXT_PARAM_FIELDNAMES);
            log.debug("Field Names Param: " + fieldNames);
            if (StringUtils.isEmpty(fieldNames)) {
                log.debug("field names are empty, returning true");
                return true;
            }
            fieldMap = new HashMap<String, String>();
            String[] fieldNamesAry = fieldNames.split(",");
            for (int i = 0; i < fieldNamesAry.length; i++) {
                fieldMap.put(fieldNamesAry[i], request.getParameter(fieldNamesAry[i]));
            }
            
            boolean excludePromotableVersion = paramsHelper.getOptionalParameterAsBoolean(EXT_PARAM_EXCLUDE_PROMOTABLE_VERSIONS, 
                    Boolean.valueOf(false)).booleanValue();
            String checkPaths = paramsHelper.getOptionalParameter(EXT_PARAM_CHECK_PATHS, null); 
            log.debug((new StringBuilder()).append("Check Paths param: ").append(checkPaths));
            
            Number contentId = new Integer(0);
            if (actionType.equals(ACTION_TYPE_UPDATE)) {
                contentId = paramsHelper.getRequiredParameterAsNumber(SYS_CONTENTID_PARAM);
                log.debug((new StringBuilder()).append("Checking for update of content id: ").append(contentId));
            }
            
            if(StringUtils.isNotBlank(cmd) && !cmd.equalsIgnoreCase("modify")) {
                log.debug((new StringBuilder()).append("command is not modify - ").append(cmd).toString());
                return Boolean.valueOf(true);
            }
            if(isPromotable(contentId.intValue()) && excludePromotableVersion) {
                log.debug("exclude promotable version");
                return Boolean.valueOf(true);     
            }
            
            //Get content types with all of the given fields
            String typeListString = makeTypeList(fieldMap.keySet());
            log.debug((new StringBuilder()).append("Types with these fields: ").append(typeListString));
            
            boolean retVal = true;
            if (actionType.equals(ACTION_TYPE_UPDATE)) {
                retVal = isFieldValueUniqueInFolderForExistingItem(contentId.intValue(), 
                        "", "", typeListString, checkPaths);
            }
            else {
                Integer folderId = getFolderId(request);
                if (folderId != null) {
                    retVal = isFieldValueUniqueInFolder(folderId, "", "", typeListString, checkPaths);
                }
                else {
                    retVal = false;
                }                
            }
            log.debug((new StringBuilder()).append("Returning value: ").append(retVal));
            return retVal;
                
        }
        catch(Exception e) {
            log.error("Exception validating multiple fields unique within folder", e);
        }
               
        return true;
    }
    
    public String getQueryForValueInFolders(int contentId, String fieldName, String fieldValue, String path, String typeList) {
        String firstPattern = "select rx:sys_contentid from {0} where rx:sys_contentid != {1}";
        StringBuilder query = new StringBuilder(MessageFormat.format(firstPattern.toString(), typeList, String.valueOf(contentId)));
                
        Set<String> keys = fieldMap.keySet();
        for (String key : keys) {
            if (StringUtils.isBlank(fieldMap.get(key))) {
                query.append((new StringBuilder(" and rx:").append(key).append(" is null")).toString());
            }
            else {
                StringBuilder innerPattern = new StringBuilder(" and rx:").append(key).append(" = ''{0}''");
                query.append(MessageFormat.format(innerPattern.toString(), fieldMap.get(key)));
            }
        }
        
        StringBuilder pathPattern = new StringBuilder(" and jcr:path like ''{0}''");
        query.append(MessageFormat.format(pathPattern.toString(), path));

        log.debug((new StringBuilder()).append("getQueryForValueInFolders() query: ").append(query));
        return query.toString();
    }

    public String getQueryForValueInFolder(String fieldName, String fieldValue, String path, String typeList) {
        String firstPattern = "select rx:sys_contentid from {0} where";
        StringBuilder query = new StringBuilder(MessageFormat.format(firstPattern.toString(), typeList));
                
        Set<String> keys = fieldMap.keySet();
        boolean isFirst = true;
        for (String key : keys) {
            if (StringUtils.isBlank(fieldMap.get(key))) {
                if (isFirst) {
                    query.append((new StringBuilder(" rx:").append(key).append(" is null")).toString());
                }
                else {
                    query.append((new StringBuilder(" and rx:").append(key).append(" is null")).toString());
                }
            }
            else {
                StringBuilder innerPattern;
                if (isFirst) {
                    innerPattern = new StringBuilder(" rx:").append(key).append(" = ''{0}''");
                }
                else {
                    innerPattern = new StringBuilder(" and rx:").append(key).append(" = ''{0}''");
                }           
                query.append(MessageFormat.format(innerPattern.toString(), fieldMap.get(key)));
            }
            isFirst = false;
        }
        
        StringBuilder pathPattern = new StringBuilder(" and jcr:path like ''{0}''");
        query.append(MessageFormat.format(pathPattern.toString(), path));

        log.debug((new StringBuilder()).append("getQueryForValueInFolder() query: ").append(query));      
        return query.toString();        
    }    

    public void init(IPSExtensionDef extensionDef, File file) throws PSExtensionException {
        super.init(extensionDef, file);
        if(this.psoNodeCataloger == null) {
            setPsoNodeCataloger(new PSONodeCataloger());
        }
    }
    
    public void setPsoNodeCataloger(PSONodeCataloger psoNodeCataloger) {
        this.psoNodeCataloger = psoNodeCataloger;
    }
    
    //All types must have all fields in fieldNames
    protected String makeTypeList(Set<String> fieldNames) throws RepositoryException {
        Collection<String> totalTypeList = new ArrayList<String>();
        boolean first = true;
        for (String fieldName : fieldNames) {
            List<String> currentTypeList = psoNodeCataloger.getContentTypeNamesWithField(fieldName);
            if (first) {
                totalTypeList.addAll(currentTypeList);
                first = false;
            }
            else {
                totalTypeList = CollectionUtils.intersection(totalTypeList, currentTypeList);
            }
        }
        
        StringBuilder retVal = new StringBuilder();
        first = true;
        for (String type : totalTypeList) {
            if (!first) {
                retVal.append(", ");            
            }
            retVal.append(type);
            first = false;
        }
    
        return retVal.toString();
    }       
    
    private Map<String, String> fieldMap;
    public Map<String, String> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, String> fieldMap) {
        this.fieldMap = fieldMap;
    }

    private PSONodeCataloger psoNodeCataloger;
    
    private static final String SYS_COMMAND_PARAM = "sys_command";
    private static final String DB_ACTION_TYPE_PARAM = "DBActionType";
    private static final String ACTION_TYPE_INSERT = "INSERT";
    private static final String ACTION_TYPE_UPDATE = "UPDATE";
    private static final String EXT_PARAM_FIELDNAMES = "fieldNames";
    private static final String EXT_PARAM_EXCLUDE_PROMOTABLE_VERSIONS = "excludePromotableVersions";
    private static final String EXT_PARAM_CHECK_PATHS = "checkPaths";
    private static final String SYS_CONTENTID_PARAM = "sys_contentid";
    private static final String FIELD_NAME_DELIMITER = "::";
    
    private static final Log log = LogFactory.getLog(com.weather.percussion.extensions.validation.TWCUniqueMultipleFieldWithInFoldersValidator.class);
}
