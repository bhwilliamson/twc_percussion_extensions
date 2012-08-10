package com.weather.percussion.extensions.schedule;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.datasource.PSDatasourceMgrLocator;
import com.percussion.services.schedule.IPSTask;
import com.percussion.services.schedule.IPSTaskResult;
import com.percussion.taxonomy.domain.Attribute_lang;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Value;
import com.percussion.taxonomy.repository.NodeServiceInf;
import com.percussion.taxonomy.repository.TaxonomyServiceInf;
import com.percussion.taxonomy.service.NodeService;
import com.percussion.util.PSPreparedStatement;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSConnectionInfo;

public class TWCSynchTaxonomiesTask implements IPSTask {

    @Override
    public void init(IPSExtensionDef extensionDef, File file)
            throws PSExtensionException {
        try {
            m_weatherConnection = getDBConnection("weather");
            m_sharedCosmosConnection = getDBConnection("shared_cosmos");
            
        } catch(Exception e) {
            throw new PSExtensionException(0, e.toString());
        }

    }

    @Override
    public IPSTaskResult perform(Map<String, String> params) {
        
        try {
            String geoTagTaxonomyId = params.get("geo_tag_taxonomy_id");
            
            
            m_nodeService = (NodeService) PSBaseServiceLocator.getBean("NodeService");            
            
            
            syncGeoTagTaxonomy(Integer.valueOf(geoTagTaxonomyId));
        } catch(Exception e) {
            //TODO: Return error as task result
        }
        
        //TODO: Return success task result
        return null;
    }
    
    private void syncGeoTagTaxonomy(Integer taxonomyId) throws Exception {
        
        //Get nodes & categorize        
        Collection nodes = m_nodeService.getAllNodes(taxonomyId, LANG_ID);
        List<String> topLevelNodeIds = new ArrayList<String>();
        List<String> secondLevelNodeIds = new ArrayList<String>();
        List<String> thirdLevelNodeIds = new ArrayList<String>();
        for (Object obj : nodes) {
            Node node = (Node) obj;
            if (node.getParent() == null) {
                topLevelNodeIds.add(getNodeIdValue(node));
            }
            else if (node.getNot_leaf()) {
                secondLevelNodeIds.add(getNodeIdValue(node));
            }
            else if (!node.getNot_leaf()) {
                thirdLevelNodeIds.add(getNodeIdValue(node));
            }
            else {
                log.error("Found geo tag that is neither type, parent, or geo tag with node id: " + node.getId());
            }
        }
        
        //Fetch types & match to top level taxons (geo type)
        String geoTypeQuery  = "select distinct geo_type from geo_master";
        PreparedStatement ps1 = PSPreparedStatement.getPreparedStatement(m_sharedCosmosConnection, geoTypeQuery);
        for(ResultSet resultSet = ps1.executeQuery(); resultSet.next();) {
            String geoType = resultSet.getString(1);
            log.debug("Found geo type fron shared cosmos: " + geoType);
            if (!topLevelNodeIds.contains(geoType)) {
                log.debug("Taxon not found.  Creating new top level node");
                topLevelNodeIds.add(geoType);
                //TODO: Create Node Here
            }
        }
        
        
        //For each type fetch parent values & match to 2nd level taxons
        String geoParentQuery = "select geom.geo_id, geom.name from geo_master geom where geom.geo_id " + 
                        "in (select geot.parent_geo_id from geo_master geom2, geo_tree geot where geom2.geo_id " + 
                        "= geot.geo_id and geom2.geo_type = '?')";
        for(String geoType : topLevelNodeIds) {
           
       }
        
        //For each parent fetch geo tag & match to 3rd level taxons
    }
    
    private void synchTopLevelGeo() {
        
    }
    
    private Connection getDBConnection(String connectionName) throws Exception {
        IPSDatasourceManager ipsdatasourcemanager = PSDatasourceMgrLocator.getDatasourceMgr();
        PSConnectionInfo psconnectioninfo = new PSConnectionInfo(connectionName);
        PSConnectionHelper connectionHelper = PSConnectionHelper.createInstance(ipsdatasourcemanager);
        return connectionHelper.getDbConnection(psconnectioninfo);     
    }
    
    private String getNodeIdValue(Node node) {
        Collection<Value> values = node.getValues();
        for (Value value : values) {
            Collection<Attribute_lang> attributeLangs = value.getAttribute().getAttribute_langs();
            for (Attribute_lang attrLang : attributeLangs) {
                if (ATTRIBUTE_VALUE_NAME.equals(attrLang.getName()) && LANG_ID == attrLang.getLanguage().getId()) {
                    return value.getName();
                }
            }
        }
        return "";
    }
    
    private Connection m_weatherConnection;
    private Connection m_sharedCosmosConnection;
    private NodeServiceInf m_nodeService;
    private TaxonomyServiceInf m_taxonomyService;
    
    //TODO: Parameterize?
    private static final int LANG_ID = 1;
    private static final String ATTRIBUTE_VALUE_NAME = "Value";
    
    private static final Log log = LogFactory.getLog(com.weather.percussion.extensions.schedule.TWCSynchTaxonomiesTask.class);

}
