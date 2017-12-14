package service.impl;

import com.google.inject.Inject;

import service.interfaces.IConfigService;

import java.util.Properties;

public class ConfigServiceImpl implements IConfigService {

    @Inject
    public ConfigServiceImpl() {

    }

    @Override
    public String getProperty(final String name) {
        Properties properties = new Properties();
        String propertiesFileName = "/resources/config.properties";

        String property;

        switch(name) {
            case "dataDir":
                property = "./Results/";
                break;
            case "gene":
                property = "./Gene/";
                break;
            case "genome":
                property = "./Genome/";
                break;
            default:
                property = null;
                break;
        }
        return property;
    }
}
