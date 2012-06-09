package com.weather.percussion.extensions.jexl;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.percussion.error.PSErrorException;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.taxonomy.TaxonomyDBHelper;
import com.percussion.taxonomy.domain.Node;

public class TWCTaxonomyUtil extends PSJexlUtilBase implements IPSJexlExpression {
    
    public Node getParentNode(String node_id) throws PSErrorException, PSExtensionProcessingException {
        Node retVal = null;
        Session session;
        int langID = 1;
        
        if(node_id == null || StringUtils.isBlank(node_id)) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }

        session = null;
        try {
            session = TaxonomyDBHelper.getSessionFactory().openSession();
            Transaction tx = session.beginTransaction();          
            String queryString = (new StringBuilder("select parent_id from tax_node where id = ")).append(node_id).toString();
            List tmp = session.createSQLQuery(queryString).list();
            String parentNodeId = "";
            for (Object obj : tmp) {
                if (obj != null && obj instanceof Integer) {
                    parentNodeId = ((Integer) obj).toString();
                }
            }
            
            queryString = "select distinct n from Node n ";
            queryString = (new StringBuilder(queryString)).append("left join fetch n.taxonomy ").toString();
            queryString = (new StringBuilder(queryString)).append("left join fetch n.nodeEditors ne ").toString();
            queryString = (new StringBuilder(queryString)).append("left join fetch n.relatedNodesForNodeId rn ").toString();
            queryString = (new StringBuilder(queryString)).append("left join fetch n.node_status ").toString();
            queryString = (new StringBuilder(queryString)).append("join fetch n.values v ").toString();
            queryString = (new StringBuilder(queryString)).append("join fetch v.attribute a ").toString();
            queryString = (new StringBuilder(queryString)).append("left join fetch rn.relationship rt ").toString();
            queryString = (new StringBuilder(queryString)).append("left join fetch a.attribute_langs al ").toString();
            queryString = (new StringBuilder(queryString)).append("join fetch al.language ").toString();
            queryString = (new StringBuilder(queryString)).append("join fetch v.lang ").toString();
            queryString = (new StringBuilder(queryString)).append("where ").toString();
            queryString = (new StringBuilder(queryString)).append("n.id = ").append(parentNodeId).toString();
            queryString = (new StringBuilder(queryString)).append("and al.language.id = ? ").toString();
            queryString = (new StringBuilder(queryString)).append("and v.lang.id = ? order by n.id").toString();
            Query q = session.createQuery(queryString);
            q.setInteger(0, langID);
            q.setInteger(1, langID);
            List objs = q.list();
            for (Object obj : objs) {
                if (obj != null && obj instanceof Node) {
                    retVal = (Node) obj;
                }
            }
            
            tx.commit();           
            
        }
        catch(Exception ex) {   
            throw new PSExtensionProcessingException(ex.getMessage(), ex);
        }
        finally {
            try {
                session.close();
            }
            catch(Exception e) {
                throw new PSExtensionProcessingException(e.getMessage(), e);
            }
        }
        
        return retVal;
    }     
    
//    public String getParentNodeId(String node_id) throws PSErrorException, PSExtensionProcessingException {
//        String retVal = "";
//        Session session;
//        
//        if(node_id == null || StringUtils.isBlank(node_id)) {
//            throw new IllegalArgumentException("String cannot be null or empty");
//        }
//
//        session = null;
//        try {
//            session = TaxonomyDBHelper.getSessionFactory().openSession();
//            Transaction tx = session.beginTransaction();          
//            String queryString = (new StringBuilder("select parent_id from tax_node where id = ")).append(node_id).toString();
//            List tmp = session.createSQLQuery(queryString).list();
//            for (Object obj : tmp) {
//                if (obj != null && obj instanceof Integer) {
//                    retVal = ((Integer) obj).toString();
//                }
//            }
//            tx.commit();           
//            
//        }
//        catch(Exception ex) {   
//            throw new PSExtensionProcessingException(ex.getMessage(), ex);
//        }
//        finally {
//            try {
//                session.close();
//            }
//            catch(Exception e) {
//                throw new PSExtensionProcessingException(e.getMessage(), e);
//            }
//        }
//        
//        return retVal;
//    }    
}
