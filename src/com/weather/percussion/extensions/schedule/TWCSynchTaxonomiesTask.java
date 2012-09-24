package com.weather.percussion.extensions.schedule;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.datasource.PSDatasourceMgrLocator;
import com.percussion.services.schedule.IPSTask;
import com.percussion.services.schedule.IPSTaskResult;
import com.percussion.services.schedule.data.PSTaskResult;
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

    public void init(IPSExtensionDef extensionDef, File file)
            throws PSExtensionException {
    }

    @Override
    public IPSTaskResult perform(Map<String, String> params) {
        
        try {
            if (m_weatherConnection == null)
                m_weatherConnection = getDBConnection("weather");
            if (m_sharedCosmosConnection == null)
                m_sharedCosmosConnection = getDBConnection("shared_cosmos");  
            
            String geoTagTaxonomyId = params.get("geo_tag_taxonomy_id");
            
            
            m_nodeService = (NodeService) PSBaseServiceLocator.getBean("NodeService");            
            
            
            syncGeoTagTaxonomy(Integer.valueOf(geoTagTaxonomyId));
        } catch(Exception e) {
            //TODO: Return error as task result
        }
        
        //TODO: Return success task result
        IPSTaskResult result = new PSTaskResult(true, "success", new HashMap());
        return result;
    }
    
    private void syncGeoTagTaxonomy(Integer taxonomyId) throws Exception {
               
        //Get top level nodes        
        Collection nodes = m_nodeService.getAllNodes(taxonomyId, LANG_ID);
        List<String> topLevelNodeIds = new ArrayList<String>();
        List<Node> topLevelNodes = new ArrayList<Node>();
        for (Object obj : nodes) {
            Node node = (Node) obj;
            if (node.getParent() == null) {
                log.debug("Found top level taxonomy node: " + node.toString());
                topLevelNodeIds.add(getNodeIdValue(node));
                topLevelNodes.add(node);
            }
        }
        
        //Fetch types & match to top level taxons (geo type)
        log.debug("Finding top level nodes in shared cosmos db");
        PreparedStatement typeStatement = PSPreparedStatement.getPreparedStatement(m_sharedCosmosConnection, GEO_TYPE_QUERY);
        for(ResultSet resultSet = typeStatement.executeQuery(); resultSet.next();) {
            String geoType = resultSet.getString(1);
            log.debug("Found geo type fron shared cosmos: " + geoType);
            if (!topLevelNodeIds.contains(geoType)) {
                log.debug("Taxon not found.  Creating new top level node");
                topLevelNodeIds.add(geoType);
                //TODO: Create Node Here
                //Node newNode = new Node();
                //topLevelNodes.add(newNode);
            }
        }
        
        log.debug("Finding parent nodes and synchronizing");
        //For each type fetch parent values & match to 2nd level taxons
        for(Node topNode : topLevelNodes) {
            log.debug("XXXX Top level node: " + topNode.toString());
            String currentTopNodeIdValue = getNodeIdValue(topNode);
            log.debug("Found top node id value: " + currentTopNodeIdValue);
            //Get taxonomy nodes
            Collection parentNodes = m_nodeService.getChildNodes(topNode.getId());
            List<String> currentParentNodeIds = new ArrayList<String>();
            for (Object obj : parentNodes) {
                log.debug("Found parent node: " + obj.toString());
                currentParentNodeIds.add(getNodeIdValue((Node) obj));
            }
            
            //Fetch db parent nodes for this geo type and add any missing
            log.debug("Finding parent nodes in shared cosmos db");
            PreparedStatement parentStnt = PSPreparedStatement.getPreparedStatement(m_sharedCosmosConnection, GEO_PARENT_QUERY);
            parentStnt.setString(1, currentTopNodeIdValue);
            for (ResultSet resultSet = parentStnt.executeQuery(); resultSet.next();) {
               String id = resultSet.getString(1);
               String name = resultSet.getString(2);
               log.debug("Found parent in shared cosmos db with id::name: " + id + "::" + name);
               if (!currentParentNodeIds.contains(id)) {
                   log.debug("Parent taxon not found, creating new node");
                   currentParentNodeIds.add(id);
                   //TODO: Create Node Here
                   //Node newNode = new Node();
                   //parentNodes.add(newNode);
               }
            }
            
            log.debug("Now look for geo tags and synchronize");
            //For each parent node of this geo type fetch geo tags and look for matching taxon
            for (Object obj : parentNodes) {
                Node parentNode = (Node) obj;
                log.debug("Parent node: " + parentNode.toString());
                String currentParentNodeIdValue = getNodeIdValue(parentNode);
                log.debug("Found parent node id value: " + currentParentNodeIdValue);
                
                //Get taxonomy nodes
                log.debug("Getting all child taxonomy nodes");
                Collection geoTagNodes = m_nodeService.getChildNodes(parentNode.getId());
                List<String> currentGeoTagNodeIds = new ArrayList<String>();
                for (Object obj2 : geoTagNodes) {
                    log.debug("Found geo tag node: " + obj2.toString());
                    currentGeoTagNodeIds.add(getNodeIdValue((Node) obj2));
                }
                
                //Fetch db geo tags for this geo type and parent
                log.debug("Finding geo tags from shared cosmos db");
                PreparedStatement geoTagStmt = PSPreparedStatement.getPreparedStatement(m_sharedCosmosConnection, GEO_TAG_QUERY);
                geoTagStmt.setString(1, currentTopNodeIdValue);
                geoTagStmt.setString(2, currentParentNodeIdValue);
                for(ResultSet resultSet = geoTagStmt.executeQuery(); resultSet.next();) {
                    String id = resultSet.getString(1);
                    String name = resultSet.getString(2);
                    log.debug("Found geo type in shared cosmos db with id::name: " + id + "::" + name);
                    if (!currentGeoTagNodeIds.contains(id)) {
                        log.debug("Geo tag not found, creating new taxonomy node");
                        currentGeoTagNodeIds.add(id);
                        //TODO: Create Node Here
                        //Node newNode = new Node();
                        //geoTagNodes.add(newNode);
                    }
                }
            }
       }
        
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
    
    private static final String GEO_TYPE_QUERY  = "select distinct geo_type from geo_master";
    private static final String GEO_PARENT_QUERY = "select geom.geo_id, geom.name from geo_master geom where geom.geo_id " + 
        "in (select geot.parent_geo_id from geo_master geom2, geo_tree geot where geom2.geo_id " + 
        "= geot.geo_id and geom2.geo_type = '?')";
    private static final String GEO_TAG_QUERY = "select geo_id, name from geo_master geom where  geom.geo_id in ( " + 
        "select geot.geo_id from geo_tree geot where geot.geo_type = '?' and geot.parent_geo_id = '?' " + 
        "and geot.geo_type = geom.geo_type) and name != ''";
    
    private static final Log log = LogFactory.getLog(com.weather.percussion.extensions.schedule.TWCSynchTaxonomiesTask.class);

}
