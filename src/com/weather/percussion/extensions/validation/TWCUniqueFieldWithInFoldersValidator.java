package com.weather.percussion.extensions.validation;

import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.percussion.data.PSConversionException;
import com.percussion.pso.utils.PSOExtensionParamsHelper;
import com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator;
import com.percussion.server.IPSRequestContext;

public class TWCUniqueFieldWithInFoldersValidator extends
        PSOUniqueFieldWithInFoldersValidator {
    
    public TWCUniqueFieldWithInFoldersValidator() {
        super();
    }
    
    /**
     * Identical to the PSO extension similarly named except returns true on exception, not false.
     * This is needed for web services to correctly create content.  Web services creates the content first without
     * a related folder, which 
     */
    public Boolean processUdf(Object params[], IPSRequestContext request) throws PSConversionException {
        String fieldName = "";
        String fieldValue = "";
        log.debug("Entering TWCUniqueFieldWithInFoldersValidator");
        try {
            String cmd = request.getParameter(SYS_COMMAND_PARAM);
            String actionType = request.getParameter(DB_ACTION_TYPE_PARAM);
            
            //Return true if action isn't valid
            if(actionType == null || !actionType.equals(ACTION_TYPE_INSERT) && !actionType.equals(ACTION_TYPE_UPDATE)) {
                log.debug((new StringBuilder("Invalid action: ").append(actionType).append(" returning true")).toString());
                return true;
            }
            
            PSOExtensionParamsHelper paramsHelper = new PSOExtensionParamsHelper(getExtensionDef(), params, request, log);
            fieldName = paramsHelper.getRequiredParameter(EXT_PARAM_FIELDNAME);
            fieldValue = request.getParameter(fieldName);
            log.debug((new StringBuilder("Field Name: ").append(fieldName)).toString());
            log.debug((new StringBuilder("Field Value: ").append(fieldValue)).toString());
            
            if(fieldValue == null) {
                log.debug((new StringBuilder()).append("Field value was null for field: ").append(fieldName).toString());
                return true;
            }            
            boolean xpv = paramsHelper.getOptionalParameterAsBoolean(EXT_PARAM_EXCLUDE_PROMOTABLE_VERSIONS, false);
            String checkPaths = paramsHelper.getOptionalParameter(EXT_PARAM_CHECK_PATHS, null);
            
            Number contentId = new Integer(0);
            if(actionType.equals(ACTION_TYPE_UPDATE))
                contentId = paramsHelper.getRequiredParameterAsNumber(SYS_CONTENTID_PARAM);

            if(StringUtils.isNotBlank(cmd) && !cmd.equalsIgnoreCase(CMD_MODIFY)) {
                log.debug((new StringBuilder()).append("command is not modify - ").append(cmd).toString());
                return true;
            }
            if(isPromotable(contentId.intValue()) && xpv) {
                log.debug("exclude promotable version");
                return true;     
            }
    
            String typeList = makeTypeList(fieldName);
            boolean retVal = true;
            if(actionType.equals(ACTION_TYPE_UPDATE)) {
                retVal = isFieldValueUniqueInFolderForExistingItem(contentId.intValue(), fieldName, fieldValue, typeList, checkPaths);
            } 
            else {
                Number folderId = getFolderId(request);
                if(folderId != null)
                    retVal = isFieldValueUniqueInFolder(folderId.intValue(), fieldName, fieldValue, typeList, checkPaths);
                else
                    retVal = true; //Web services create items without folders, need to return true here
            }
            
            log.debug((new StringBuilder("Returning: ")).append(retVal).toString());
            return retVal;
            
        }
        catch(Exception e) {
            log.error(MessageFormat.format("An error happend while checking if fieldName: {0} was unique for contentId: {1} with fieldValue: {2}", new Object[] {
                fieldName, request.getParameter("sys_contentid"), fieldValue
            }), e);
        }
        return true;
    }
    
    private static final String SYS_COMMAND_PARAM = "sys_command";
    private static final String DB_ACTION_TYPE_PARAM = "DBActionType";
    private static final String ACTION_TYPE_INSERT = "INSERT";
    private static final String ACTION_TYPE_UPDATE = "UPDATE";
    private static final String EXT_PARAM_FIELDNAME = "fieldName";
    private static final String EXT_PARAM_EXCLUDE_PROMOTABLE_VERSIONS = "excludePromotableVersions";
    private static final String EXT_PARAM_CHECK_PATHS = "checkPaths";
    private static final String SYS_CONTENTID_PARAM = "sys_contentid";    
    private static final String CMD_MODIFY = "modify";

    private static final Log log = LogFactory.getLog(com.weather.percussion.extensions.validation.TWCUniqueFieldWithInFoldersValidator.class);
}
