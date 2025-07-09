package com.vnpt.it5.ics.authentication;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadPropertiesSingleton {
    private static final String FILE_CONFIG = "application.properties";
    private static ReadPropertiesSingleton instance = null;
    private Properties properties = new Properties();

    /**
     * Use singleton pattern to create ReadConfig object one time and use
     * anywhere
     *
     * @return instance of ReadConfig object
     */
    public static ReadPropertiesSingleton getInstance() {
        if (instance == null) {
            instance = new ReadPropertiesSingleton();
            instance.readConfig();
        }
        return instance;
    }

    /**
     * get property with key
     *
     * @param key
     * @return value of key
     */
    public String getProperty(String key) {
        String property = properties.getProperty(key);
        if(property==null){
            return null;
        }
        return property;
    }

    /**
     * read file .properties
     */
    private void readConfig() {
        InputStream inputStream = null;
        try {

            String currentDir = System.getProperty("user.dir");
            inputStream = getClass().getClassLoader()
                    .getResourceAsStream("application.properties");
//            inputStream = new FileInputStream(currentDir + FILE_CONFIG);
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
