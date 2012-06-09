package com.weather.percussion.extensions.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.weather.percussion.extensions.validation.TWCUniqueMultipleFieldWithInFoldersValidator;


public class TWCUniqueMultipleFieldWithInFoldersValidatorTest {
    
    @Test
    public void test_getQueryForValueInFolders() {
        TWCUniqueMultipleFieldWithInFoldersValidator validator = new TWCUniqueMultipleFieldWithInFoldersValidator();
        Map<String, String> fieldMap = new HashMap<String, String>();
        fieldMap.put("pageId", "12345");
        fieldMap.put("mode", "mytestmode");
        
        validator.setFieldMap(fieldMap);
        
        String fieldName = "pageId::mode";
        String fieldValue = "12345::mytestmode";
        String path = "//Sites/Desktop/";
        String typeList = "twcPage, twcLFC";
        String result1 = validator.getQueryForValueInFolders(1234, fieldName, fieldValue, path, typeList);      
        String expectedResult1 = "select rx:sys_contentid from twcPage, twcLFC where rx:sys_contentid != 1234 and rx:pageId = '12345' and rx:mode = 'mytestmode' and jcr:path like '//Sites/Desktop/'";
                                  
        assertEquals(result1, expectedResult1);
        
    }
    
    @Test
    public void test_getQueryForValueInFolder() {
        TWCUniqueMultipleFieldWithInFoldersValidator validator = new TWCUniqueMultipleFieldWithInFoldersValidator();
        
        Map<String, String> fieldMap = new HashMap<String, String>();
        fieldMap.put("pageId", "12345");
        fieldMap.put("mode", "mytestmode");
        
        validator.setFieldMap(fieldMap);        
        
        String fieldName = "pageId::mode";
        String fieldValue = "12345::mytestmode";
        String path = "//Sites/Desktop/";
        String typeList = "twcPage, twcLFC";
        String result1 = validator.getQueryForValueInFolder(fieldName, fieldValue, path, typeList);      
        String expectedResult1 = "select rx:sys_contentid from twcPage, twcLFC where rx:pageId = '12345' and rx:mode = 'mytestmode' and jcr:path like '//Sites/Desktop/'";
                                  
        assertEquals(result1, expectedResult1);
    }

}
